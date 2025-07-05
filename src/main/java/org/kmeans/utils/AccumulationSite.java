package org.kmeans.utils;

public class AccumulationSite {
    public double latitude;
    public double longitude;
    public double capacity;

    public AccumulationSite() {
        // Needed for Jackson
    }

    public AccumulationSite(double latitude, double longitude, double capacity) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
    }

    public double distanceTo(ClusterCenter center) {
        double dx = latitude - center.latitude;
        double dy = longitude - center.longitude;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
