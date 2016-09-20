package org.geospaces.schas;

import android.location.Location;

/**
 * Created by Erik on 9/2/2016.
 */
public class CustomMarker {

    private Location MarkerLocation;
    private boolean IsValid;
    private boolean IsRecentPoint;

    public CustomMarker(Location location, boolean isRecentPoint){
        MarkerLocation = location;
        IsValid = true;
        IsRecentPoint = isRecentPoint;
    }

    public void MarkAsInvalid(){
        IsValid = false;
    }

    public void MarkAsValid(){
        IsValid = true;
    }

    public Location GetMarkerLocation(){
        return MarkerLocation;
    }

    public boolean GetValidity(){
        return IsValid;
    }

    public boolean GetPointIsRecent(){
        return IsRecentPoint;
    }

    public String ToString(){
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
