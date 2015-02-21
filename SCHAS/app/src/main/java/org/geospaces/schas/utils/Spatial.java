package org.geospaces.schas.utils;

import android.location.Location;

public class Spatial {
    private static final int earthRadius = 6371;

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {

        double dLat = (double) Math.toRadians(lat2 - lat1);
        double dLon = (double) Math.toRadians(lon2 - lon1);
        double a =
                (double) (Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2));
        double c = (double) (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
        double d = earthRadius * c;
        return d;
    }

    public static double calculateDistance(Location l1, Location l2) {
        if ( l1 == null || l2 == null ) {
            return 0;
        }
        double dist = Spatial.calculateDistance(l1.getLatitude(), l1.getLongitude(),
                l2.getLatitude(), l2.getLongitude());
        return dist;
    }
}
