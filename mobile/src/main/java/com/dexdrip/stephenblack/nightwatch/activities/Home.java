package com.dexdrip.stephenblack.nightwatch.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.dexdrip.stephenblack.nightwatch.BgGraphBuilder;
import com.dexdrip.stephenblack.nightwatch.services.DataCollectionService;
import com.dexdrip.stephenblack.nightwatch.R;
import com.dexdrip.stephenblack.nightwatch.services.WatchUpdaterService;
import com.dexdrip.stephenblack.nightwatch.integration.dexdrip.Intents;
import com.dexdrip.stephenblack.nightwatch.model.Bg;
import com.dexdrip.stephenblack.nightwatch.utils.IdempotentMigrations;

import java.util.Date;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ViewportChangeListener;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static android.preference.PreferenceManager.setDefaultValues;


public class Home extends BaseActivity {
    public static final String MENU_NAME = "NightWatch";
    private LineChartView chart;
    private PreviewLineChartView previewChart;
    Viewport tempViewport = new Viewport();
    Viewport holdViewport = new Viewport();
    public float right;
    public boolean updateStuff;
    public boolean updatingPreviewViewport = false;
    public boolean updatingChartViewport = false;
    public SharedPreferences prefs;
    public BgGraphBuilder bgGraphBuilder;
    BroadcastReceiver _broadcastReceiver;
    BroadcastReceiver newDataReceiver;
    OnSharedPreferenceChangeListener preferenceChangeListener;

    @Override
    public String getMenuName() {
        return MENU_NAME;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_home;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDefaultValues(this, R.xml.pref_general, false);
        setDefaultValues(this, R.xml.pref_license, false);
        setDefaultValues(this, R.xml.pref_data_source, false);
        setDefaultValues(this, R.xml.pref_other, false);
        setDefaultValues(this, R.xml.pref_bg_notification, false);
        setDefaultValues(this, R.xml.pref_watch_integration, false);



        prefs = getDefaultSharedPreferences(this);

        checkEula();

        new IdempotentMigrations(getApplicationContext()).performAll();

        startService(new Intent(getApplicationContext(), DataCollectionService.class));


        preferenceChangeListener = (sharedPreferences, key) -> invalidateOptionsMenu();

        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        Intent intent = new Intent();
        String packageName = getPackageName();
        Log.d(this.getClass().getName(), "Maybe ignoring battery optimization");
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName) &&
                !prefs.getBoolean("requested_ignore_battery_optimizations", false)) {

            Log.d(this.getClass().getName(), "Requesting ignore battery optimization");

            prefs.edit().putBoolean("requested_ignore_battery_optimizations", true).apply();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }

    }

    public void checkEula() {
        boolean IUnderstand = prefs.getBoolean("I_understand", false);
        if (!IUnderstand) {
            Intent intent = new Intent(getApplicationContext(), LicenseAgreementActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bgGraphBuilder = new BgGraphBuilder(getApplicationContext());
        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    setupCharts();
                    displayCurrentInfo();
                    holdViewport.set(0, 0, 0, 0);
                }
            }
        };
        newDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                setupCharts();
                displayCurrentInfo();
                holdViewport.set(0, 0, 0, 0);
            }
        };
        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        registerReceiver(newDataReceiver, new IntentFilter(Intents.ACTION_NEW_BG));
        setupCharts();
        displayCurrentInfo();
        holdViewport.set(0, 0, 0, 0);
    }

    @Override
    protected void onDestroy() {
        if (preferenceChangeListener != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home, menu);

        if (!prefs.getBoolean("watch_sync", false)) {
            menu.removeItem(R.id.action_open_watch_settings);
        }
        if (!prefs.getBoolean("watch_sync", false) && !prefs.getBoolean("pebble_sync", false)) {
            menu.removeItem(R.id.action_resend_last_bg);

        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_resend_last_bg:
                startService(new Intent(this, WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_RESEND));
                break;
            case R.id.action_open_watch_settings:
                startService(new Intent(this, WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_OPEN_SETTINGS));
        }
        return super.onOptionsItemSelected(item);
    }

    public void setupCharts() {
        bgGraphBuilder  = new BgGraphBuilder(this);
        updateStuff = false;
        chart = findViewById(R.id.chart);
        chart.setZoomType(ZoomType.HORIZONTAL);

        previewChart = findViewById(R.id.chart_preview);
        previewChart.setZoomType(ZoomType.HORIZONTAL);

        chart.setLineChartData(bgGraphBuilder.lineData());
        previewChart.setLineChartData(bgGraphBuilder.previewLineData());
        updateStuff = true;

        previewChart.setViewportCalculationEnabled(true);
        chart.setViewportCalculationEnabled(true);
        previewChart.setViewportChangeListener(new ViewportListener());
        chart.setViewportChangeListener(new ChartViewPortListener());
        setViewport();
    }

    private class ChartViewPortListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingPreviewViewport) {
                updatingChartViewport = true;
                previewChart.setZoomType(ZoomType.HORIZONTAL);
                previewChart.setCurrentViewport(newViewport);
                updatingChartViewport = false;
            }
        }
    }

    private class ViewportListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingChartViewport) {
                updatingPreviewViewport = true;
                chart.setZoomType(ZoomType.HORIZONTAL);
                chart.setCurrentViewport(newViewport);
                tempViewport = newViewport;
                updatingPreviewViewport = false;
            }
            if (updateStuff) {
                holdViewport.set(newViewport.left, newViewport.top, newViewport.right, newViewport.bottom);
            }
        }
    }

    public void setViewport() {
        if (tempViewport.left == 0.0 || holdViewport.left == 0.0 || holdViewport.right  >= (new Date().getTime())) {
            previewChart.setCurrentViewport(bgGraphBuilder.advanceViewport(chart, previewChart));
        } else {
            previewChart.setCurrentViewport(holdViewport);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_broadcastReceiver != null) {
            unregisterReceiver(_broadcastReceiver);
        }
        if (newDataReceiver != null) {
            unregisterReceiver(newDataReceiver);
        }
    }

    public void displayCurrentInfo() {
        final TextView currentBgValueText = findViewById(R.id.currentBgValueRealTime);
        final TextView notificationText = findViewById(R.id.notices);
        if ((currentBgValueText.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) > 0) {
            currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        Bg lastBgreading = Bg.last();

        if (lastBgreading != null) {
            //TODO: Adrian: Remove raw string?
            if ( prefs.getBoolean("showRaw",false) ) {
                // show age and raw readings
                notificationText.setText(getString(R.string.bg_age_display,
                        lastBgreading.readingAge(),
                        Bg.threeRaw((prefs.getString("units", "mgdl").equals("mgdl")))));
            } else {
                // show age and the delta
                notificationText.setText(getString(R.string.bg_age_display,
                        lastBgreading.readingAge(),
                        bgGraphBuilder.unitizedDeltaString(true,true)));
            }

            currentBgValueText.setText(getString(R.string.bg_value_display,
                    lastBgreading.sgv_double(),lastBgreading.slopeArrow()));

            if ((new Date().getTime()) - (60000 * 16) - lastBgreading.datetime > 0) {
                notificationText.setTextColor(Color.parseColor("#C30909"));
                currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                notificationText.setTextColor(Color.WHITE);
            }
            double estimate = lastBgreading.sgv_double();
            if (bgGraphBuilder.unitized(estimate) <= bgGraphBuilder.lowMark) {
                currentBgValueText.setTextColor(Color.parseColor("#C30909"));
            } else if (bgGraphBuilder.unitized(estimate) >= bgGraphBuilder.highMark) {
                currentBgValueText.setTextColor(Color.parseColor("#FFBB33"));
            } else {
                currentBgValueText.setTextColor(Color.WHITE);
            }
        }
    }
}
