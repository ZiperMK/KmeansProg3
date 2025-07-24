package org.kmeans.utils;

import java.util.List;
import java.util.ArrayList;

public class ClusterCenter {
    public double latitude;
    public double longitude;
    public List<AccumulationSite> assignedSites = new ArrayList<>();

    // ✅ Default constructor for Jackson
    public ClusterCenter() {}

    public ClusterCenter(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean updateCenter() {
        if (assignedSites.isEmpty()) return false;

        double sumLat = 0, sumLon = 0;
        for (AccumulationSite site : assignedSites) {
            sumLat += site.latitude;
            sumLon += site.longitude;
        }

        double newLat = sumLat / assignedSites.size();
        double newLon = sumLon / assignedSites.size();

        boolean changed = (newLat != this.latitude || newLon != this.longitude);
        this.latitude = newLat;
        this.longitude = newLon;

        return changed;
    }
}
