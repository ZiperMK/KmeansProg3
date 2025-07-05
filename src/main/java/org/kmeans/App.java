package org.kmeans;

import org.kmeans.gui.MapWindow;
import org.kmeans.utils.*;

import javax.swing.*;
import java.util.List;

public class App {
    public static void main(String[] args) {
        try {
            // Step 1: Ask user for parameters
            String siteInput = JOptionPane.showInputDialog("Enter number of accumulation sites:");
            String clusterInput = JOptionPane.showInputDialog("Enter number of clusters:");

            int numSites = Integer.parseInt(siteInput);
            int numClusters = Integer.parseInt(clusterInput);

            // Step 2: Load sites from JSON or generate extras
            String pathToJson = "src/main/resources/disposal_sites.json"; // Adjust path if needed
            List<AccumulationSite> sites = SiteLoader.loadSites(pathToJson, numSites);

            // Step 3: Run clustering
            ClusteringResult result = KMeans.cluster(sites, numClusters, 100);
            List<ClusterCenter> clusters = result.centers;

            System.out.println("Cycles: " + result.cycles);
            System.out.println("Run time: " + result.durationMillis + " ms");

            // Step 4: Open map window
            SwingUtilities.invokeLater(() -> new MapWindow(clusters));

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }
}
