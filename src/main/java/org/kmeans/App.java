package org.kmeans;

import org.kmeans.gui.MapWindow;
import org.kmeans.utils.*;

import javax.swing.*;
import java.util.List;

public class App {
    public static void main(String[] args) {
        try {
            // Step 1: Choose execution mode
            String[] modes = {"Sequential", "Parallel", "Distributed (Not Available)"};
            String selectedMode = (String) JOptionPane.showInputDialog(
                    null,
                    "Choose execution mode:",
                    "Execution Mode",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    modes,
                    modes[0] // default is Sequential
            );

            if (selectedMode == null) {
                JOptionPane.showMessageDialog(null, "No mode selected. Exiting.");
                return;
            }

            // Step 2: Ask user for parameters
            String siteInput = JOptionPane.showInputDialog("Enter number of accumulation sites:");
            String clusterInput = JOptionPane.showInputDialog("Enter number of clusters:");
            String guiInput = JOptionPane.showInputDialog("Enable GUI? (yes/no):");

            int numSites = Integer.parseInt(siteInput);
            int numClusters = Integer.parseInt(clusterInput);
            boolean enableGUI = guiInput.trim().equalsIgnoreCase("yes");

            // Step 3: Print hardware info
            HardwareInfo.printSystemStats();

            // Step 4: Load sites
            String pathToJson = "src/main/resources/disposal_sites.json";
            List<AccumulationSite> sites = SiteLoader.loadSites(pathToJson, numSites);

            // Step 5: Run clustering in a background thread
            new Thread(() -> {
                ClusteringResult result;

                switch (selectedMode) {
                    case "Parallel":
                        result = KMeansParallel.cluster(sites, numClusters, 100);
                        break;
                    case "Distributed (Not Available)":
                        JOptionPane.showMessageDialog(null, "Distributed mode is not yet implemented.");
                        return;
                    case "Sequential":
                    default:
                        result = KMeans.cluster(sites, numClusters, 100);
                }

                List<ClusterCenter> clusters = result.centers;

                System.out.println("Cycles: " + result.cycles);
                System.out.println("Run time: " + result.durationMillis + " ms");

                // Step 6: Show GUI if enabled
                if (enableGUI) {
                    SwingUtilities.invokeLater(() -> new MapWindow(clusters));
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }
}
