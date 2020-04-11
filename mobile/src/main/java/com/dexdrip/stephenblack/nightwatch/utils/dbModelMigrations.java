package com.dexdrip.stephenblack.nightwatch.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import com.dexdrip.stephenblack.nightwatch.model.AlertType;
import com.dexdrip.stephenblack.nightwatch.activities.SnoozeActivity;
import com.dexdrip.stephenblack.nightwatch.Constants;

/**
 * Created by stephenblack on 9/4/15.
 */
public class dbModelMigrations {
    private Context mContext;
    private SharedPreferences prefs;

    public dbModelMigrations(Context context) {
        this.mContext = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public void performAll() {
        migrateBGAlerts();
    }

    private void migrateBGAlerts() {
        // Migrate away from old style notifications to Tzachis new Alert system
        if(prefs.getBoolean("bg_notifications", true)){
            double highMark = Double.parseDouble(prefs.getString("highValue", "170"));
            double lowMark = Double.parseDouble(prefs.getString("lowValue", "70"));

            boolean doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);

            if(!doMgdl) {
                highMark = highMark * Constants.MMOLL_TO_MGDL;
                lowMark = lowMark * Constants.MMOLL_TO_MGDL;
            }
            boolean bg_sound_in_silent = prefs.getBoolean("bg_sound_in_silent", false);
            String bg_notification_sound = prefs.getString("bg_notification_sound", "content://settings/system/notification_sound");

            int bg_high_snooze = Integer.parseInt(prefs.getString("bg_snooze",  Integer.toString(SnoozeActivity.getDefaultSnooze(AlertType.alertType.high))));
            int bg_low_snooze = Integer.parseInt(prefs.getString("bg_snooze",  Integer.toString(SnoozeActivity.getDefaultSnooze(AlertType.alertType.low))));
            int bg_missed_snooze = Integer.parseInt(prefs.getString("bg_snooze",  Integer.toString(SnoozeActivity.getDefaultSnooze(AlertType.alertType.missed))));


            AlertType.add_alert(null, "High Alert", AlertType.alertType.high, highMark, true,
                    1, bg_notification_sound, 0, 0, bg_sound_in_silent, bg_high_snooze, true);

            AlertType.add_alert(null, "Low Alert", AlertType.alertType.low, lowMark,
                    true, 1, bg_notification_sound, 0, 0, bg_sound_in_silent, bg_low_snooze, true);

            AlertType.add_alert(null, "Missed Data", AlertType.alertType.missed, 30,
                    true, 1, bg_notification_sound, 0, 0, bg_sound_in_silent, bg_missed_snooze, true);

            prefs.edit().putBoolean("bg_notifications", false).apply();
        }
    }

}
