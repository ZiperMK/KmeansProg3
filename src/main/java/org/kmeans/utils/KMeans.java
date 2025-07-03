package org.kmeans.utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class KMeans {
    public static List<ClusterCenter> cluster(List<AccumulationSite> sites, int k, int maxIterations) {
        List<ClusterCenter> centers = new ArrayList<>();
        Random rand = new Random();

        // Initialize random cluster centers
        for (int i = 0; i < k; i++) {
            AccumulationSite randSite = sites.get(rand.nextInt(sites.size()));
            centers.add(new ClusterCenter(randSite.latitude, randSite.longitude));
        }

        for (int iter = 0; iter < maxIterations; iter++) {
            for (ClusterCenter center : centers)
                center.assignedSites.clear();

            // Assign each site to the nearest center
            for (AccumulationSite site : sites) {
                ClusterCenter closest = null;
                double minDist = Double.MAX_VALUE;
                for (ClusterCenter center : centers) {
                    double dist = site.distanceTo(center);
                    if (dist < minDist) {
                        minDist = dist;
                        closest = center;
                    }
                }
                closest.assignedSites.add(site);
            }

            // Update center positions
            for (ClusterCenter center : centers)
                center.updateCenter();
        }

        return centers;
    }
}
