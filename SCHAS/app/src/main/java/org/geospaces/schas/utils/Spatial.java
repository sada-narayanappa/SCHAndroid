package org.geospaces.schas.utils;

import android.location.Location;

public class Spatial {
    private static final int earthRadius = 6371;

    public static float calculateDistance(double lat1, double lon1, double lat2, double lon2) {

        float dLat = (float) Math.toRadians(lat2 - lat1);
        float dLon = (float) Math.toRadians(lon2 - lon1);
        float a =
                (float) (Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2));
        float c = (float) (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
        float d = earthRadius * c;
        return d;
    }

    public static float calculateDistance(Location l1, Location l2) {
        if ( l1 == null || l2 == null ) {
            return 0;
        }
        float dist = Spatial.calculateDistance(l1.getLatitude(), l1.getLongitude(),
                l2.getLatitude(), l2.getLongitude());
        return dist;
    }
}
