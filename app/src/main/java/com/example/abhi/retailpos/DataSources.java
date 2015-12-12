package com.example.abhi.retailpos;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Abhi on 12/12/2015.
 */
public class DataSources extends SQLiteOpenHelper{
    private Context contxt;
    // Database Version
    private static final int DATABASE_VERSION = 2;
    // Database Name
    private static final String DATABASE_NAME = "SalesDB";

    public DataSources(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.contxt = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LOCATION_TABLE = "CREATE TABLE location ( " +
                "id INTEGER  PRIMARY KEY AUTOINCREMENT, " +
                "PingType TEXT ," +
                "Ref TEXT ," +
                "LAT TEXT ," +
                "LONG TEXT," +
                "Battery TEXT," +
                "StartTime INTEGER," +
                "StopTime INTEGER," +
                "Address TEXT)";
        db.execSQL(CREATE_LOCATION_TABLE);
        Log.d("Datsources", "OnCreate>>>>");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS location");
        Log.d("Datsources","OnUpgrade>>>>");
        // create fresh tables
        this.onCreate(db);
    }
    /**LOCATION TABLE METHODS
     * CRUD operations (create "add", read "get", update, delete)
     */

    // customers table name

    private static final String TABLE_LOCATION = "location";

    //customers field names
    private static final String KEY_PINGID = "id";
    private static final String KEY_PING = "PingType";
    private static final String KEY_REF = "Ref";
    private static final String KEY_LAT = "LAT";
    private static final String KEY_LONG = "LONG";
    private static final String KEY_BATT = "Battery";
    private static final String KEY_TIMESTRT = "StartTime";
    private static final String KEY_TIMESTP = "StopTime";
    private static final String KEY_ADD = "Address";

    // Get All pings for upload
    public List<Ping> getAllPings() {
        List<Ping> Pings = new LinkedList<Ping>();

        // 1. build the query
        String query = "SELECT * FROM " + TABLE_LOCATION;

        //2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build OLs and add it to list
        Ping pg = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if(cursor.moveToFirst()){
                do{
                    pg = new Ping();
                    pg.setid(Integer.parseInt(cursor.getString(0)));
                    pg.setType(cursor.getString(1));
                    pg.setRef(cursor.getString(2));
                    String lat = cursor.getString(3);
                    String lng = cursor.getString(4);
                    String bat = cursor.getString(5);
                    pg.setLat(Double.parseDouble(lat));
                    pg.setLong(Double.parseDouble(lng));
                    pg.setBattery(Double.parseDouble(bat));

                    Log.d("frm sqlite lat : long", lat+":"+lng);

                    if (cursor.getString(6) != null){
                        pg.setStart(Long.parseLong(cursor.getString(6)));
                    }else{
                        pg.setStart(0);
                    }

                    if (cursor.getString(7) != null){
                        pg.setStop(Long.parseLong(cursor.getString(7)));
                    }else{
                        pg.setStop(0);
                    }
                    pg.setAdd(cursor.getString(8));


                    // Add product to products
                    Pings.add(pg);
                }while (cursor.moveToNext());
            }
        }
        cursor.close();
        db.close();
        Log.d("getAllPings()", Pings.toString());

        // return pings
        return Pings;
    }

    public void delPingID(int PID) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();


        // 2. delete
        db.delete(TABLE_LOCATION,
                KEY_PINGID+" = ?",
                new String[] { String.valueOf(PID) });

        // 3. close
        db.close();

        Log.d("Ping of ID :"+String.valueOf(PID)+" was deleted", "Success");

    }

    public void delAllPings() {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. delete
        db.execSQL("DELETE FROM location");

        // 3. close
        db.close();
        Log.d("All Pings Deleted", "Success");

    }

    //Ping Location
    public void pinglocn(String Ping,String Ref,String LAT,String LONG,long TimeStrt,long TimeStp,String Address){

        Intent batteryIntent = this.contxt.getApplicationContext().registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int rawlevel = batteryIntent.getIntExtra("level", -1);
        double scale = batteryIntent.getIntExtra("scale", -1);
        double level = -1;
        if (rawlevel >= 0 && scale > 0) {
            level = rawlevel / scale;
        }


        Log.d("Ping"+Ping,Ref+"-"+Address);
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_PING, Ping);
        values.put(KEY_REF, Ref);
        values.put(KEY_LAT,LAT);
        values.put(KEY_LONG,LONG);
        values.put(KEY_BATT,Double.toString(level));
        values.put(KEY_TIMESTRT,TimeStrt);
        values.put(KEY_TIMESTP,TimeStp);
        values.put(KEY_ADD,Address);

        // 3. insert
        db.insert(TABLE_LOCATION, // table
                null, //nullColumnHack
                values); // key/value -> keys = column names/ values = column values

        // 4. close
        db.close();
    }


}
