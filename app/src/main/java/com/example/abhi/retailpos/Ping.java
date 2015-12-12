package com.example.abhi.retailpos;

/**
 * Created by Abhi on 12/12/2015.
 */
public class Ping {
    private int id;
    private String Type;
    private String Ref;
    private double Lat;
    private double Long;
    private double Battery;
    private long Start;
    private long Stop;
    private String Add;

    public Ping() {}

	/*public Ping(int ordno,String custname,String pcode,double qty,int invno ,double rate,double tx){
		super();
	}*/


    @Override
    public String toString() {
        return "Ping [Type :"+ Type + ", Ref :" + Ref + ", Loc :" + Lat+","+Long+ ", Time :" + Start+"-"+Stop + ", Add :" + Add +"]";
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getRef() {
        return Ref;
    }

    public void setRef(String ref) {
        Ref = ref;
    }

    public double getLat() {
        return Lat;
    }

    public void setLat(double lat) {
        Lat = lat;
    }

    public double getLong() {
        return Long;
    }

    public void setLong(double _long) {
        Long = _long;
    }

    public double getBattery() {
        return Battery;
    }

    public void setBattery(double Batt) {
        Battery = Batt;
    }

    public long getStart() {
        return Start;
    }

    public void setStart(long start) {
        Start = start;
    }

    public long getStop() {
        return Stop;
    }

    public void setStop(long stop) {
        Stop = stop;
    }

    public String getAdd() {
        return Add;
    }

    public void setAdd(String add) {
        Add = add;
    }

    public int getid() {
        return id;
    }

    public void setid(int ID) {
        id = ID;
    }

}
