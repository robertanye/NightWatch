package com.dexdrip.stephenblack.nightwatch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.activeandroid.Cache;
import com.activeandroid.Configuration;
import com.activeandroid.ActiveAndroid;
import android.database.sqlite.SQLiteDatabase;

import com.dexdrip.stephenblack.nightwatch.model.Bg;
import com.dexdrip.stephenblack.nightwatch.model.ShareGlucose;
import com.dexdrip.stephenblack.nightwatch.sharemodels.ShareRest;
import com.dexdrip.stephenblack.nightwatch.integration.dexdrip.Intents;
import com.dexdrip.stephenblack.nightwatch.alerts.Notifications;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import retrofit.Retrofit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataCollectionService extends Service {
    public static final int TIMEOUT = 15000; // 15 seconds for testing. Lower it afterwards.
    DataFetcher dataFetcher;
    SharedPreferences mPrefs;
    SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener;
    boolean wear_integration  = false;
    boolean pebble_integration  = false;
    boolean endpoint_set = false;
    private PendingIntent wakeIntent;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setSettings();
        listenForChangeInSettings();

        // check to see if we need to create the database
        initDb( this );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "collector");
        wakeLock.acquire();
        setFailoverTimer();
        setSettings();
        if(endpoint_set) { doService(wakeLock); } else { if(wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); } }
        setAlarm();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mPrefs != null && mPreferencesListener != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(mPreferencesListener);
        }
        setFailoverTimer();
    }
    private void initDb( Context context) {
        Configuration dbConfiguration = new Configuration.Builder(context).create();
        try {
            SQLiteDatabase db = Cache.openDatabase();
            ActiveAndroid.initialize(this,false ); // second parameter turns on debug output
            if (db != null) {
                Log.d("DataCollectionService", "InitDb DB exists");
            }
            else {
                ActiveAndroid.initialize(this,false);
                Log.d("DataCollectionService", "InitDb DB does NOT exist. Call ActiveAndroid.initialize()");
            }
        } catch (Exception e) {
            ActiveAndroid.initialize(this,false);
            Log.d("DataCollectionService", "InitDb CATCH: DB does NOT exist. Call ActiveAndroid.initialize()");
        }


    }
    public void setSettings() {
        wear_integration = mPrefs.getBoolean("watch_sync", false);
        pebble_integration = mPrefs.getBoolean("pebble_sync", false);
        if (mPrefs.getBoolean("nightscout_poll", false) || mPrefs.getBoolean("share_poll", false)) {
            endpoint_set = true;
        } else {
            endpoint_set = false;
        }
    }
    public void setFailoverTimer() {
        long retry_in = (1000 * 60 * 6);
        Log.d("DataCollectionService", "Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        long wakeTime = calendar.getTimeInMillis() + retry_in;
        PendingIntent serviceIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
        } else {
            alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
        }

    }

    public void listenForChangeInSettings() {
        mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                getApplicationContext().startService(new Intent(getApplicationContext(), Notifications.class));
                setSettings();
            }
        };
        mPrefs.registerOnSharedPreferenceChangeListener(mPreferencesListener);
    }
    public void doService(PowerManager.WakeLock wakeLock) { doService(1, wakeLock);}
    public void doService(int count, PowerManager.WakeLock wakeLock) {
        Log.d("Performing data fetch: ", "Wish me luck");
        dataFetcher = new DataFetcher(getApplicationContext(), wakeLock);
        dataFetcher.execute(count);
    }

    public void setAlarm() {
        long retry_in = (long) sleepTime();
        Log.d("DataCollectionService", "Next packet should be available in " + (retry_in / (60 * 1000)) + " minutes");
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        long wakeTime = calendar.getTimeInMillis() + retry_in;
        Log.d(this.getClass().getName() , "ArmTimer waking at: "+ new Date(wakeTime) +" in " +  (wakeTime - calendar.getTimeInMillis())/60000d + " minutes");
        if (wakeIntent != null)
            alarm.cancel(wakeIntent);
        wakeIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            alarm.setAlarmClock(new AlarmManager.AlarmClockInfo(wakeTime, wakeIntent), wakeIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, wakeIntent);
        } else {
            alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, wakeIntent);
        }
    }

    public double sleepTime() {
        Bg last_bg=null;
        try {
            last_bg = Bg.last();
        } catch (Exception exx ) {
            last_bg = null;
        }
        if (last_bg != null) {
            return Math.max((1000 * 30), Math.min(((long) (((1000 * 60 * 5) + 15000) - ((new Date().getTime()) - last_bg.datetime))), (1000 * 60 * 5)));
        } else {
            return (1000 * 60 * 5);
        }
    }

    public class DataFetcher extends AsyncTask<Integer, Void, Boolean> {
        Context mContext;
        PowerManager.WakeLock mWakeLock;

        DataFetcher(Context context, PowerManager.WakeLock wakeLock) { mContext = context; mWakeLock = wakeLock; }

        @Override
        protected Boolean doInBackground(Integer... params) {
            int requestCount = params[0];
            if (params[0] == 1) {
                requestCount = requestCount();
            }
            try {
                if(mPrefs.getBoolean("nightscout_poll", false)) {
                    Log.d("NightscoutPoll", "fetching " + requestCount);
                    boolean success = new Rest(mContext).getBg(requestCount);
                    Thread.sleep(10000);
                    if (success) {
                        //quick fix: stay awake a bit to handover wakelog
                        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
                        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "quickFix2").acquire(TIMEOUT);
                        mContext.startService(new Intent(mContext, WatchUpdaterService.class));
                    }
                    getApplicationContext().startService(new Intent(getApplicationContext(), Notifications.class));
                    if(mWakeLock != null && mWakeLock.isHeld()) { mWakeLock.release(); }
                    return true;
                }
                if(mPrefs.getBoolean("share_poll", false)) {
                    Log.d("ShareRest", "fetching " + requestCount);
                    // create the callback function
                    Callback<List<ShareGlucose>> shareDataCallback = new Callback<List<ShareGlucose>>() {
                        @Override
                        public void onResponse(Call<List<ShareGlucose>> call, Response<List<ShareGlucose>> response) {

                        }

                        @Override
                        public void onFailure(Call<List<ShareGlucose>> call, Throwable t) {

                        }
                    };
                    Boolean shareRest = new ShareRest(mContext, null).getBgReadings(requestCount,  shareDataCallback );
                    if ( shareRest ) {
                        //test wakelock: stay awake a bit to handover wakelog
                        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
                        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "quickFix3").acquire(TIMEOUT);

                        mContext.startService(new Intent(mContext, WatchUpdaterService.class));
                    }
                    getApplicationContext().startService(new Intent(getApplicationContext(), Notifications.class));
                    if(mWakeLock != null && mWakeLock.isHeld()) { mWakeLock.release(); }
                    return true;
                }
                return true;
            }
            catch (InterruptedException exx) { Log.d("Interruption Error: ", "BOOOO"); }
            catch (Exception ex) { Log.d("Unrecognized Error: ", "BOOOO"); }
            if(mWakeLock != null && mWakeLock.isHeld()) { mWakeLock.release(); }
            return false;
        }
    }

    public static void newDataArrived(Context context, boolean success, Bg bg) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "collector data arrived");
        wakeLock.acquire();
        if (success && bg != null) {
            Intent intent = new Intent(context, WatchUpdaterService.class);
            String date = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date ((long)bg.datetime));
            intent.putExtra("timestamp", bg.datetime);
            Log.d("NewDataArrived", "New Data Arrived with timestamp "+ date);
            context.startService(intent);
            Intent updateIntent = new Intent(Intents.ACTION_NEW_BG);
            //test wakelock: stay awake a bit to handover wakelog
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "quickFix1").acquire(TIMEOUT);
            context.sendBroadcast(updateIntent);
        }
        context.startService(new Intent(context, Notifications.class));
        if(wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); }
    }
    public int requestCount() {
        Bg bg = Bg.last();
        if(bg == null) {
            return 576;
        } else if (bg.datetime < new Date().getTime()) {
            return Math.min((int) Math.ceil(((new Date().getTime() - bg.datetime) / (5 * 1000 * 60))), 576);
        } else {
            return 1;
        }
    }
}
