package org.kmeans;

import org.kmeans.gui.MapWindow;
import org.kmeans.utils.*;

import javax.swing.*;

import java.io.File;
import java.util.List;

public class App {
    public static void main(String[] args) {
        try {
            // Step 1: Choose execution mode
            String[] modes = { "Sequential", "Parallel", "Distributed " };
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
                    case "Distributed ":
                        try {
                            int np = 4; // 1 master + 3 workers

                            String mpjHome = System.getenv("MPJ_HOME");
                            if (mpjHome == null) {
                                throw new RuntimeException("MPJ_HOME not set. Cannot run distributed mode.");
                            }

                            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
                            String pathSep = isWindows ? ";" : ":";

                            String mpjRunCmd = isWindows
                                    ? mpjHome + "\\bin\\mpjrun.bat"
                                    : mpjHome + "/bin/mpjrun.sh";

                            StringBuilder cpBuilder = new StringBuilder("target/classes");

                            File depDir = new File("target/dependency");
                            if (depDir.exists()) {
                                File[] jars = depDir.listFiles(f -> f.getName().endsWith(".jar"));
                                if (jars != null) {
                                    for (File jar : jars) {
                                        cpBuilder.append(pathSep).append("target/dependency/").append(jar.getName());
                                    }
                                }
                            }
                            cpBuilder.append(pathSep).append(".");

                            String classPath = cpBuilder.toString();
                            System.out.println("MPJ Classpath used: " + classPath);

                            // Run the distributed job
                            ProcessBuilder pb = new ProcessBuilder(
                                    mpjRunCmd,
                                    "-np", String.valueOf(np),
                                    "-dev", "multicore",
                                    "-cp", classPath,
                                    "org.kmeans.utils.DistributedKMeansMPJ",
                                    "src/main/resources/disposal_sites.json",
                                    String.valueOf(numClusters),
                                    String.valueOf(numSites));
                            pb.inheritIO();
                            Process proc = pb.start();
                            proc.waitFor();

                            // ✅ Now load the result written by DistributedKMeansMPJ
                            ClusteringResult distResult = ResultSaver.loadResult("results/distributed_result.json");
                            if (distResult == null) {
                                throw new RuntimeException("Distributed mode did not produce a valid result file.");
                            }
                            // Reassign sites to nearest cluster for GUI coloring
                            for (AccumulationSite site : sites) {
                                int nearest = findNearestCluster(site, distResult.centers);
                                distResult.centers.get(nearest).assignedSites.add(site);
                            }

                            // Same post-processing as other modes
                            System.out.println("Cycles: " + distResult.cycles);
                            System.out.println("Run time: " + distResult.durationMillis + " ms");

                            if (enableGUI) {
                                SwingUtilities.invokeLater(() -> new MapWindow(distResult.centers));
                            }

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null,
                                    "Failed to launch distributed mode: " + ex.getMessage());
                        }
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
    private static int findNearestCluster(AccumulationSite site, List<ClusterCenter> centers) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < centers.size(); i++) {
            double d = Math.pow(site.latitude - centers.get(i).latitude, 2)
                     + Math.pow(site.longitude - centers.get(i).longitude, 2);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }
    
}
