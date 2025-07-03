package org.kmeans.utils;

import java.util.List;
import java.util.ArrayList;


public class ClusterCenter {
    public double latitude;
    public double longitude;
    public List<AccumulationSite> assignedSites = new ArrayList<>();

    public ClusterCenter(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void updateCenter() {
        if (assignedSites.isEmpty()) return;
        double latSum = 0, lonSum = 0;
        for (AccumulationSite site : assignedSites) {
            latSum += site.latitude;
            lonSum += site.longitude;
        }
        this.latitude = latSum / assignedSites.size();
        this.longitude = lonSum / assignedSites.size();
    }
}
