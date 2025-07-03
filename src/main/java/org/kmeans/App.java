package org.kmeans;

import org.kmeans.gui.MapWindow;
import org.kmeans.utils.AccumulationSite;
import org.kmeans.utils.ClusterCenter;
import org.kmeans.utils.KMeans;

import javax.swing.*;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

public class App {
    public static void main(String[] args) {
        // Step 1: Ask user for parameters
        String siteInput = JOptionPane.showInputDialog("Enter number of accumulation sites:");
        String clusterInput = JOptionPane.showInputDialog("Enter number of clusters:");

        int numSites = Integer.parseInt(siteInput);
        int numClusters = Integer.parseInt(clusterInput);

        // Step 2: Generate random accumulation sites (in Europe bounds)
        List<AccumulationSite> sites = new ArrayList<>();
        Random rand = new Random();

        // Rough bounding box: lat 35–60, lon -10 to 30 (Europe)
        for (int i = 0; i < numSites; i++) {
            double lat = 35 + rand.nextDouble() * 25;
            double lon = -10 + rand.nextDouble() * 40;
            double capacity = 100 + rand.nextDouble() * 900; // tonnes
            sites.add(new AccumulationSite(lat, lon, capacity));
        }

        // Step 3: Run clustering
        List<ClusterCenter> clusters = KMeans.cluster(sites, numClusters, 100);

        // Step 4: Open map window
        SwingUtilities.invokeLater(() -> new MapWindow(clusters));
    }
}
