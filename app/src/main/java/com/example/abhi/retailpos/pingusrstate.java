package com.example.abhi.retailpos;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Abhi on 12/12/2015.
 */
public class pingusrstate extends Service {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final String DEBUG_TAG = "UpdateLocation::Service";
    DataSources DB = new DataSources(this);
    int frcdpunchouthr = 22;
    double latitude = 0;
    double longitude = 0;
    String addrs;
    GPSTracker gps;
    private AlarmManager alarms;
    private PendingIntent tracking;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            usrping();
            // Stop the service using the startId, so that we don't stop the service in the middle of handling another job
            boolean stopped = stopSelfResult(msg.arg1);
            Log.d(DEBUG_TAG, ">>>Servicestopped rslt:" + stopped);
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        super.onCreate();
        HandlerThread thread = new HandlerThread("ServiceStartArguments",android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Log.d(DEBUG_TAG, ">>>onCreate()");
        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        gps = new GPSTracker(this);
        alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        Log.d(DEBUG_TAG, ">>>onStartCommand()");
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
//    Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        Log.d(DEBUG_TAG, ">>>onDestroy()");
    }

    //obtain current location, insert into database and make toast notification on screen
    private void usrping() {

        java.util.Date date= new java.util.Date();
        final Timestamp ti = new Timestamp(date.getTime());
        // Get the current time.
        Calendar now = Calendar.getInstance();
        // Create the punchout time.
        Calendar punchoutTime = Calendar.getInstance();
        // Set the punch out time.
        punchoutTime.set(Calendar.HOUR_OF_DAY, frcdpunchouthr-1);
        Log.d("Alarm Time Hrs:",Integer.toString(now.get(Calendar.HOUR_OF_DAY)));
        // Check that the store "was opened" today.
        if (now.after(punchoutTime)) {
            Log.d("Frcd logout:","True");

            // Set User login status to logout
            SharedPreferences sharedPref = getSharedPreferences("userstatus",MODE_PRIVATE);
            sharedPref.edit();
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putInt("loginstate",0);
            prefEditor.commit();

            //Set the Alarm Status to cancelled
            SharedPreferences timersharedPref = getSharedPreferences("timerstate",MODE_PRIVATE);
            timersharedPref.edit();
            SharedPreferences.Editor timerprefEditor = timersharedPref.edit();
            timerprefEditor.putString("timerstate","Alarm Cancelled at "+ ti);
            timerprefEditor.commit();
            Log.d("Location Pinger :","Stopped after forced punch out");


            Intent intent = new Intent(getBaseContext(), AlarmReceiver.class);
            tracking = PendingIntent.getBroadcast(getBaseContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarms.cancel(tracking);

            //check if GPS enabled
            if(gps.canGetLocation()){
                gps.getLocation();
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
                addrs = this.GetGPSAddress(latitude, longitude);
            }else{
                addrs = "Error in GPS";
            }
            DB.pinglocn("Punch-Frcd", "logout", Double.toString(latitude),Double.toString(longitude), 0, System.currentTimeMillis(), addrs);
        }
        else
        {	Log.d("Frcd logout:","False");

            SharedPreferences timersharedPref = getSharedPreferences("timerstate",MODE_PRIVATE);
            timersharedPref.edit();
            SharedPreferences.Editor timerprefEditor = timersharedPref.edit();
            timerprefEditor.putString("timerstate","Alarm Set, Last trigger : "+ ti);
            timerprefEditor.commit();
            //check if GPS enabled
            if(gps.canGetLocation()){
                gps.getLocation();
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
                addrs = GetGPSAddress(latitude, longitude);
                //Toast.makeText(MainActivity.this.getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude + "\nAddress:" + addrs, Toast.LENGTH_LONG).show();
            }else{
                // can't get location
                // GPS or Network is not enabled
                // Ask user to enable GPS/network in settings
                //gps.showSettingsAlert();
                addrs = "Error in GPS";
            }
            DB.pinglocn("UsrState","IDLE", Double.toString(latitude), Double.toString(longitude), System.currentTimeMillis(), 0, addrs);
        }
    }


    //get string address
    public String GetGPSAddress(double lat, double lon){
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        String ret = "";
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if(addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("Address:\n");
                for(int i=0; i<returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                ret = strReturnedAddress.toString();
            }
            else{
                ret = "No Address returned!";
            }
        } catch (IOException e) {
            e.printStackTrace();
            ret = "Cannot get Address!";
        }
        return ret;
    }

}