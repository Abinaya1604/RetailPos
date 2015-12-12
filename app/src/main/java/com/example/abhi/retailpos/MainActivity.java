package com.example.abhi.retailpos;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends Activity {



    // Progress Dialog
    private ProgressDialog pDialog;

    // Creating JSON Parser object
    JSONParser jParserusr = new JSONParser();


    // url to get all products list, retailers
    private static String url_user_auth = "http://localhost/mobpos_demo/get_user_auth.php";


    // JSON Node names
    private static final String TAG_SUCCESS = "success";

    // products JSONArray
    JSONArray products = null;
    //retailer JSONArray
    JSONArray retailers = null;
    //device name
    String devname = null;
    String devid = null;
    // user auth
    int usr=0;
    GPSTracker gps;
    private AlarmManager alarms;
   // private PendingIntent tracking;
//    private long UPDATE_INTERVAL = 30 * 60 * 1000;
  //  private int START_DELAY = 5 * 60;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ///////////////////////////////////////////////////////////////////////////
        alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        ///////////////////////////////////////////////////////////////////////////

        gps = new GPSTracker(MainActivity.this);
        final SharedPreferences devicePref = getSharedPreferences("devicestate",MODE_PRIVATE);
        devname = devicePref.getString("devicename", "NA");
        setContentView(R.layout.activity_main);
        if(devname!="NA"){
            SharedPreferences sharedPref = getSharedPreferences("userstatus",MODE_PRIVATE);
            int loginstatus = sharedPref.getInt("loginstate",0);
            TextView logtx = (TextView) findViewById(R.id.home_punchstatus);
            if(loginstatus == 0){
                logtx.setText("Punched out");
                logtx.setBackgroundColor(Color.RED);
            }
            if(loginstatus == 1){
                logtx.setText("Punched in");
                logtx.setBackgroundColor(Color.GREEN);
            }
        }else{
            devid = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
            AlertDialog.Builder adb = null;
            adb = new AlertDialog.Builder(this);
            adb.setTitle("You are logging in from a New Device!");
            adb.setMessage("Get the device password from SAF admin and try!!");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            adb.setView(input);
            input.requestFocus();
            // Set up the buttons
            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, int which) {
                    Thread login = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            PackageInfo pinfo;
                            String versionName = "0.0";
                            try {
                                pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                                versionName = pinfo.versionName;
                            } catch (NameNotFoundException e) {
                                e.printStackTrace();
                            }
                            devname = input.getText().toString();
                            List<NameValuePair> usrparams = new ArrayList<NameValuePair>();
                            usrparams.add(new BasicNameValuePair("devid", devid));
                            usrparams.add(new BasicNameValuePair("device", devname));
                            usrparams.add(new BasicNameValuePair("version", versionName));
                            Log.d("Usr Request : ", devid+"-"+devname);
                            JSONObject usrjson = null;
                            try{
                                // getting JSON string from URL
                                usrjson = jParserusr.makeHttpRequest(url_user_auth, "POST", usrparams);
                                // Check your log cat for JSON reponse
                                Log.d("Usr Auth : ", usrjson.toString());
                            }catch (Exception er){
                                er.printStackTrace();
                            }
                            try {
                                // Checking for SUCCESS TAG
                                int usrslt = usrjson.getInt(TAG_SUCCESS);
                                usr =  usrslt;
                            }catch (Exception er){
                                er.printStackTrace();
                            }
                        }
                    });
                    login.start();
                    try {
                        login.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (usr==1){
                        String devnm = input.getText().toString();
                        devicePref.edit();
                        SharedPreferences.Editor prefEditor = devicePref.edit();
                        prefEditor.putString("devicename", devnm);
                        prefEditor.commit();
                        devname = devnm;
                        setContentView(R.layout.activity_main);
                        SharedPreferences sharedPref = getSharedPreferences("userstatus",0);
                        int loginstatus = sharedPref.getInt("loginstate",0);
                        TextView logtx  = (TextView) findViewById(R.id.home_punchstatus);
                        if(loginstatus == 0){
                            logtx.setText("Punched out");
                            logtx.setBackgroundColor(Color.RED);
                        }
                        if(loginstatus == 1){
                            logtx.setText("Punched in");
                            logtx.setBackgroundColor(Color.GREEN);
                        }
                    }else{
                        dialog.cancel();
                        dialog.dismiss();
                        finish();
                    }

                }
            });
            adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    dialog.dismiss();
                    dialog = null;
                    finish();
                }
            });
            AlertDialog alertToShow = adb.create();
            alertToShow.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            alertToShow.show();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        TextView logtx = (TextView) findViewById(R.id.home_punchstatus);
        SharedPreferences sharedPref = getSharedPreferences("userstatus",0);
        int loginstatus = sharedPref.getInt("loginstate",0);
        if(loginstatus == 0){
            logtx.setText("Punched out");
            logtx.setBackgroundColor(Color.RED);
        }else if(loginstatus == 1){
            logtx.setText("Punched in");
            logtx.setBackgroundColor(Color.GREEN);
        }
    }



    public void punch(View view) {
        syncbtn = (Button) findViewById(R.id.btn_home_sync);
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("Punch In/Out!!!");
        adb.setMessage("Do you want to punch In/Out?");
        // Set up the buttons
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DataSources DB = new DataSources(MainActivity.this);
                TextView logtx  = (TextView) findViewById(R.id.home_punchstatus);
                SharedPreferences sharedPref = getSharedPreferences("userstatus",0);
                int loginstatus = sharedPref.getInt("loginstate",0);
                double latitude = 0.0;
                double longitude = 0.0;
                String addrs;
                if(loginstatus == 0){
                    logtx.setText("Punched in");
                    logtx.setBackgroundColor(Color.GREEN);
                    sharedPref.edit();
                    SharedPreferences.Editor prefEditor = sharedPref.edit();
                    prefEditor.putInt("loginstate",1);
                    prefEditor.commit();
                    // check if GPS enabled
                    if(gps.canGetLocation()){
                        gps.getLocation();
                        latitude = gps.getLatitude();
                        longitude = gps.getLongitude();
                        addrs = MainActivity.this.GetAddress(latitude, longitude);
                    }else{
                        // can't get location
                        Toast.makeText(getApplicationContext(),"GPS problem, hence call for manual attendance", Toast.LENGTH_LONG).show();
                        addrs = "Error in GPS";
                    }
                    DB.pinglocn("Punch", "login", Double.toString(latitude), Double.toString(longitude), System.currentTimeMillis(), 0, addrs);
                    ///////////////////////////////////////////////////////////////////////////
                    setRecurringAlarm(MainActivity.this);
                    ///////////////////////////////////////////////////////////////////////////
                }else{
                    logtx.setText("Punched out");
                    logtx.setBackgroundColor(Color.RED);
                    sharedPref.edit();
                    SharedPreferences.Editor prefEditor = sharedPref.edit();
                    prefEditor.putInt("loginstate",0);
                    prefEditor.commit();
                    //check if GPS enabled
                    if(gps.canGetLocation()){
                        gps.getLocation();
                        latitude = gps.getLatitude();
                        longitude = gps.getLongitude();
                        addrs = MainActivity.this.GetAddress(latitude, longitude);
                    }else{
                        // can't get location
                        // GPS or Network is not enabled
                        Toast.makeText(getApplicationContext(),"GPS problem, hence call for manual attendance", Toast.LENGTH_LONG).show();
                        addrs = "Error in GPS";
                    }
                    SharedPreferences ordhdPref = getSharedPreferences("Orderhead",MODE_PRIVATE);
                    SharedPreferences.Editor ordhdPrefEditor = ordhdPref.edit();
                    ordhdPrefEditor.clear();
                    ordhdPrefEditor.commit();
                    DB.pinglocn("Punch", "logout", Double.toString(latitude),Double.toString(longitude), 0, System.currentTimeMillis(), addrs);
                    ///////////////////////////////////////////////////////////////////////////
                    stopRecurringALarm(MainActivity.this);
                    ///////////////////////////////////////////////////////////////////////////
                }
                syncbtn.performClick();
            }
        });
        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alertToShow = adb.create();
        alertToShow.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertToShow.show();
    }

    ///////////////////////////////////////////////////////////////////////////
    public void stopRecurringALarm(Context context){
        Intent intent = new Intent(context, AlarmReceiver.class);
        tracking = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(tracking);
        Log.d("Stoptrack MainAcivity", ">>>Stop tracking()");
    }

    private void setRecurringAlarm(Context context) {
        // get a Calendar object with current time
        Calendar cal = Calendar.getInstance();
        // add start delay to the calendar object
        cal.add(Calendar.SECOND, START_DELAY);
        Log.d("Starttrack MainAcivity", ">>>Start tracking()");

        Intent intent = new Intent(context, AlarmReceiver.class);

        tracking = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), UPDATE_INTERVAL, tracking);
    }
    ///////////////////////////////////////////////////////////////////////////


    // Background Async Task to sync data by making HTTP Request for products & retailers downloads, orders and ping uploads
    class SyncData extends AsyncTask<String, String, String> {
        int prodsyncrslt = 0;
        int Rtrssyncrslt = 0;
        SharedPreferences revPref = getSharedPreferences("Revision",MODE_PRIVATE);
        String currev;

        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (pDialog != null){
                pDialog.dismiss();
            }else{
                pDialog = new ProgressDialog(MainActivity.this);
                pDialog.setMessage("Data Sync in progress. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setCancelable(false);
                pDialog.show();
                Log.d("Staring Sync: ", "Prd:"+String.valueOf(prodsyncrslt)+",Rtlrs:"+String.valueOf(Rtrssyncrslt));
            }
        }

        //update appversion, download products, retailers and upload locationpings and customer orders from url
        protected String doInBackground(String... args) {

            // Get revision pref for smart sync (946666810000 is an old date millsec values)
            currev = revPref.getString("CurrentRev", "946666810000");

            //****************************************************************************************
            //upload the order lines
            DataSources DB = new DataSources(MainActivity.this);
            List<uploadorderline> ols = DB.getAllOL4UPLD();
            if (ols.size()>0){
                //Repeat and loop this until all objects are added
                for(uploadorderline ol : ols){

                    int ordupldrslt = 0;
                    int ordlnid = ol.getolid();
                    String ordno = devname+"-"+ol.getOrderNo();
                    String custid = ol.getCustid();
                    String pcode = ol.getPcode();
                    String qty = String.valueOf(ol.getQty());
                    String invno = devname+"-"+ol.getInvNo();
                    String rate = String.valueOf(ol.getRate());
                    String txprcnt = String.valueOf(ol.getTx());


                    // Building Parameters
                    List<NameValuePair> addolparam = new ArrayList<NameValuePair>();
                    addolparam.add(new BasicNameValuePair("ordno", ordno));
                    addolparam.add(new BasicNameValuePair("custid", custid));
                    addolparam.add(new BasicNameValuePair("pcode", pcode));
                    addolparam.add(new BasicNameValuePair("qty", qty));
                    addolparam.add(new BasicNameValuePair("invno", invno));
                    addolparam.add(new BasicNameValuePair("rate", rate));
                    addolparam.add(new BasicNameValuePair("txprcnt", txprcnt));


                    // check for success tag
                    try {

                        // getting JSON Object
                        JSONObject jsonOLUpld = jParserolupld.makeHttpRequest(url_upload_orders,
                                "POST", addolparam);

                        // check log cat for response
                        Log.d("Order upload Response", jsonOLUpld.toString());


                        ordupldrslt = jsonOLUpld.getInt(TAG_SUCCESS);

                        if (ordupldrslt == 1) {
                            // successfully uploaded orderlines
                            DB.delolID(ordlnid);
                        } else {
                            // failed to upload product
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            //****************************************************************************************
            //upload the pings
            List<Ping> pings = DB.getAllPings();

            if (pings.size()>0){
                //Repeat and loop this until all objects are added
                for(Ping ping : pings){
                    int pngupldrslt = 0;
                    int pngid = ping.getid();
                    String type = ping.getType();
                    String ref = devname+"-"+ping.getRef();
                    String dev = devname;
                    String lat = Double.toString(ping.getLat());
                    String lng = Double.toString(ping.getLong());
                    String bat = Double.toString(ping.getBattery());
                    String start = Long.toString(ping.getStart());
                    String stop = Long.toString(ping.getStop());
                    String add = ping.getAdd();

                    // Building Parameters
                    List<NameValuePair> addpingparam = new ArrayList<NameValuePair>();
                    addpingparam.add(new BasicNameValuePair("type", type));
                    addpingparam.add(new BasicNameValuePair("ref", ref));
                    addpingparam.add(new BasicNameValuePair("dev", dev));
                    addpingparam.add(new BasicNameValuePair("lat", lat));
                    addpingparam.add(new BasicNameValuePair("lng", lng));
                    addpingparam.add(new BasicNameValuePair("bat", bat));
                    addpingparam.add(new BasicNameValuePair("start", start));
                    addpingparam.add(new BasicNameValuePair("stop", stop));
                    addpingparam.add(new BasicNameValuePair("add", add));



                    // check for success tag
                    try {
                        // getting JSON Object
                        JSONObject jsonPing = jParserping.makeHttpRequest(url_upload_ping,
                                "POST", addpingparam);
                        // check log cat for response
                        Log.d("Ping Upload Response"+String.valueOf(pngid), jsonPing.toString());
                        pngupldrslt = jsonPing.getInt(TAG_SUCCESS);

                        if (pngupldrslt == 1) {
                            // successfully uploaded pingid
                            DB.delPingID(pngid);
                        } else {
                            // failed to upload ping
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            //****************************************************************************************
            // Send the appversion to the user
            PackageInfo pinfo;
            String versionName = "0.0";
            try {
                pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                versionName = pinfo.versionName;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            List<NameValuePair> versionparams = new ArrayList<NameValuePair>();
            SharedPreferences devicePrf = getSharedPreferences("devicestate",MODE_PRIVATE);
            devname = devicePrf.getString("devicename", "NA");
            devid = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
            versionparams.add(new BasicNameValuePair("devid", devid));
            versionparams.add(new BasicNameValuePair("device", devname));
            versionparams.add(new BasicNameValuePair("version", versionName));
            Log.d("Version Upld : ", devid+"-"+ devname+"-"+versionName);
            JSONObject Verjson = null;
            usr = 0;
            try{
                // getting JSON string from URL
                Verjson = jParservers.makeHttpRequest(url_user_auth, "POST", versionparams);
                int usrslt = Verjson.getInt(TAG_SUCCESS);
                usr =  usrslt;
                // Check your log cat for JSON reponse
                Log.d("Version Upload : ", Verjson.toString());
            }catch (Exception er){
                er.printStackTrace();
            }
            if (usr==0){
                Log.d("Resseting Device : ", "Unauthorized Usr");
                //Reset device name
                SharedPreferences devicePref = getSharedPreferences("devicestate",MODE_PRIVATE);
                devicePref.edit();
                SharedPreferences.Editor prefEditor = devicePref.edit();
                prefEditor.putString("devicename", "NA");
                prefEditor.commit();
                //Reset login status
                SharedPreferences userstsPref = getSharedPreferences("userstatus",MODE_PRIVATE);
                userstsPref.edit();
                SharedPreferences.Editor userstsEditor = userstsPref.edit();
                userstsEditor.putInt("loginstate",0);
                userstsEditor.commit();
                DB.delAllCustomers();
                DB.delAllProds();
                DB.delAllOrdLns();
                DB.delAllPings();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this,"You are Unathorized, contact SAF admin", Toast.LENGTH_LONG).show();
                        TextView logtx  = (TextView) findViewById(R.id.home_punchstatus);
                        logtx.setText("Punched Out");
                        logtx.setBackgroundColor(Color.RED);
                    }
                });
                return null;
            }


            //****************************************************************************************
            //download products
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("device", devname));
            params.add(new BasicNameValuePair("updtime", currev));
            JSONObject json = null;
            try {
                // getting JSON string from URL
                json = jParserprod.makeHttpRequest(url_all_products, "POST", params);
                // Checking for SUCCESS TAG
                prodsyncrslt = json.getInt(TAG_SUCCESS);
                if (prodsyncrslt == 1) {
                    // products found
                    // Getting Array of Products
                    products = json.getJSONArray(TAG_PRODUCTS);
                    // looping through All Products
                    for (int i = 0; i < products.length(); i++) {
                        JSONObject c = products.getJSONObject(i);
                        // Storing each json item in variable
                        DB.syncProduct(new Product(c.getString(TAG_PCODE),c.getString(TAG_PRODNM),c.getString(TAG_PRODCAT),Double.parseDouble(c.getString(TAG_RATE)),c.getString(TAG_UOM),Double.parseDouble(c.getString(TAG_TX))),c.getString(TAG_FRPCODE),Double.parseDouble(c.getString(TAG_FRPRCNT)),Double.parseDouble(c.getString(TAG_QTYBRK)));
                    }
                } else {
                    // no updated products found

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //****************************************************************************************
            //download retailers
            // Building Parameters
            List<NameValuePair> paramsret = new ArrayList<NameValuePair>();
            paramsret.add(new BasicNameValuePair("device", devname));
            paramsret.add(new BasicNameValuePair("updtime", currev));
            JSONObject jsonret = null;

            try {
                // getting JSON string from URL
                jsonret = jParserret.makeHttpRequest(url_all_retailers, "POST", paramsret);
                // Checking for SUCCESS TAG
                Rtrssyncrslt = jsonret.getInt(TAG_SUCCESS);
                if (Rtrssyncrslt == 1) {
                    // retailers found
                    // Getting Array of Retailers
                    retailers = jsonret.getJSONArray(TAG_RETAILERS);
                    // looping through All retailers
                    for (int i = 0; i < retailers.length(); i++) {
                        JSONObject c = retailers.getJSONObject(i);
                        // Storing each json item in variable
                        DB.syncCustomer(new Customer(c.getString(TAG_CUSID),c.getString(TAG_CUSNM),c.getString(TAG_DIST),c.getString(TAG_AREA),c.getString(TAG_STREET),c.getString(TAG_PHN)));
                    }
                } else {
                    // no updated retailers found
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // store current time in the shared preferences
            revPref.edit();
            SharedPreferences.Editor revprefEditor = revPref.edit();
            revprefEditor.putString("CurrentRev",Long.toString(System.currentTimeMillis()));
            revprefEditor.commit();
            return null;
        }


    }


}
