package com.dexdrip.stephenblack.nightwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dexdrip.stephenblack.nightwatch.services.DataCollectionService;

/**
 * Created by stephenblack on 11/3/14.
 */
public class AutoStart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, DataCollectionService.class));
    }
}
