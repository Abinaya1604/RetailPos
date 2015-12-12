package com.example.abhi.retailpos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Abhi on 12/12/2015.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String DEBUG_TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(DEBUG_TAG, "Recurring alarm; requesting location tracking.");
        // start the service
        Intent pinging = new Intent(context, pingusrstate.class);
        context.startService(pinging);
    }
}