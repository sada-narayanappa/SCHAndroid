package org.geospaces.schas.UtilityObjectClasses;

import android.location.Location;

/**
 * Created by Erik on 9/2/2016.
 */
public class CustomMarker {

    private Location MarkerLocation;
    private boolean IsValid;
    private boolean IsRecentPoint;
    private boolean isWrittenIntoFile;

    public CustomMarker(Location location, boolean isRecentPoint){
        MarkerLocation = location;
        IsValid = true;
        IsRecentPoint = isRecentPoint;
        isWrittenIntoFile = false;
    }

    public void markAsInvalid(){
        IsValid = false;
    }

    public void markAsValid(){
        IsValid = true;
    }

    public Location getMarkerLocation(){
        return MarkerLocation;
    }

    public boolean getValidity(){
        return IsValid;
    }

    public boolean getPointIsRecent(){
        return IsRecentPoint;
    }

    public boolean getWrittenIntoFile() {
        return isWrittenIntoFile;
    }

    public void setIsWrittenIntoFile(boolean isWrittenIntoFile){
        this.isWrittenIntoFile = isWrittenIntoFile;
    }

    @Override
    public String toString(){
        String uploadString =
                "measured_at=" + (System.currentTimeMillis() / 1000) + "," +
                        "lat=" + MarkerLocation.getLatitude() + "," +
                        "lon=" + MarkerLocation.getLongitude() + "," +
                        "alt=" + MarkerLocation.getAltitude() + "," +
                        "speed=" + MarkerLocation.getSpeed() + "," +
                        "bearing=" + MarkerLocation.getBearing() + "," +
                        "accuracy=" + MarkerLocation.getAccuracy() + "," +
                        "record_type=" + MarkerLocation.getProvider() + "," +
                        "session_num=" + (System.currentTimeMillis() / 1000000 * 60) + "," +
                        "is_valid=" + IsValid;

        return uploadString;
    }
}
