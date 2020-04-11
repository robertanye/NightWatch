package com.dexdrip.stephenblack.nightwatch.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import androidx.preference.PreferenceManager;
import android.util.Log;

import com.dexdrip.stephenblack.nightwatch.Cal;
import com.dexdrip.stephenblack.nightwatch.watch.PebbleEndpoint;
import com.dexdrip.stephenblack.nightwatch.watch.PebbleEndpointInterface;
import com.dexdrip.stephenblack.nightwatch.model.Bg;
import com.google.gson.GsonBuilder;

import java.util.Objects;

import retrofit2.converter.gson.GsonConverterFactory;

import retrofit2.Retrofit;



/**
 * Created by stephenblack on 12/26/14.
 */
public class NightWatchRest {
    private Context mContext;
    private String mUrl;
    private static final String UNITS = "mgdl";
    private SharedPreferences prefs;
    private PowerManager.WakeLock wakeLock;


    NightWatchRest(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mUrl = prefs.getString("dex_collection_method", "https://{yoursite}.herokuapp.com");
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        assert powerManager != null;
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NW:rest wakelock");
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
    }

    boolean getBg(int count) {
        if (!prefs.getBoolean("nightscout_poll", false) && mUrl.compareTo("") != 0 && mUrl.compareTo("https://{yoursite}herokuapp.com") != 0) {
            return false;
        }
        try {
            boolean newData = false;
            PebbleEndpoint Bgs;

            if ( count > 1 ) {
                Bgs = pebbleEndpointInterface().getPebbleInfo(UNITS,count).execute().body();

            } else {
                Bgs = pebbleEndpointInterface().getPebbleInfo(UNITS).execute().body();
            }


            double slope = 0, intercept = 0, scale = 0;
            assert Bgs != null;
            if (Bgs.cals != null && Bgs.cals.size() != 0){
                Cal cal = Bgs.cals.get(0);
                slope = cal.slope;
                intercept = cal.intercept;
                scale = cal.scale;
            }

            for (Bg returnedBg: Bgs.bgs) {
                if (Bg.is_new(returnedBg)) {

                    //raw logic from https://github.com/nightscout/cgm-remote-monitor/blob/master/lib/plugins/rawbg.js#L59
                    if (slope != 0 && intercept != 0 && scale != 0) {
                        if (returnedBg.filtered == 0 || returnedBg.sgv_double() < 40) {
                            returnedBg.raw = scale * (returnedBg.unfiltered - intercept) / slope;
                        } else {
                            double ratio = scale * (returnedBg.filtered - intercept) / slope / returnedBg.sgv_double();
                            returnedBg.raw = scale * (returnedBg.unfiltered - intercept) / slope / ratio;
                        }
                    }
                    returnedBg.save();
                    DataCollectionService.newDataArrived(mContext, true, returnedBg);
                    newData = true;
                }
            }
            Log.d("REST CALL SUCCESS: ", "OK");
            if(wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); }
            return newData;
        } catch (Exception e) {
            Log.d("REST CALL ERROR: ", Objects.requireNonNull(e.getMessage()));
            e.printStackTrace();
            if(wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); }
            return false;
        }
    }

    public boolean getBg() {
        return getBg(1);
    }

    private PebbleEndpointInterface pebbleEndpointInterface() {


        Retrofit restInf = new Retrofit.Builder()
                .baseUrl(mUrl)
                .addConverterFactory(GsonConverterFactory
                .create(new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()))
                .build();
        return restInf.create(PebbleEndpointInterface.class);

    }


}
