package org.geospaces.schas.UtilityObjectClasses;

/**
 * Created by Erik on 10/2/2016.
 */

public class DatabaseLocationObject {
    public String measuredAt;
    public float lat;
    public float lon;
    public String alt;
    public String speed;
    public String bearing;
    public String accuracy;
    public String recordType;
    public String sessionNum;
    public boolean isValid;

    public boolean isUploadedPreviously = false;
    public boolean isWrittenIntoFile;

    public DatabaseLocationObject(String measuredAt,
                       float lat,
                       float lon,
                       String alt,
                       String speed,
                       String bearing,
                       String accuracy,
                       String recordType,
                       String sessionNum,
                       boolean isValid){
        this.measuredAt = measuredAt;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.speed = speed;
        this.bearing = bearing;
        this.accuracy = accuracy;
        this.recordType = recordType;
        this.sessionNum = sessionNum;
        this.isValid = isValid;
        this.isWrittenIntoFile = false;
    }

    public String ToString(){
        String returnString =
                "measured_at=" + measuredAt + "," +
                "lat=" + lat + "," +
                "lon=" + lon + "," +
                "alt=" + alt + "," +
                "speed=" + speed + "," +
                "bearing=" + bearing + "," +
                "accuracy=" + accuracy + "," +
                "record_type=" + recordType + "," +
                "session_num=" + sessionNum + "," +
                "is_valid=" + isValid;

        return returnString;
    }

    public boolean GetValidity() {
        return isValid;
    }

    public void setPreviouslyUploaded(){
        isUploadedPreviously = true;
    }

    public void setWrittenIntoFile(){
        isWrittenIntoFile = true;
    }
}