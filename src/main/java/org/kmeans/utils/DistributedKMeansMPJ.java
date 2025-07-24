package org.kmeans.utils;

import mpi.MPI;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class DistributedKMeansMPJ {

    private static final int MAX_ITERATIONS = 50;

    public static void main(String[] args) throws Exception {

        System.out.println("[DEBUG] MPJ passed args:");
        for (int i = 0; i < args.length; i++) {
            System.out.println("  args[" + i + "] = " + args[i]);
        }
        // --- Init MPI ---
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        // We now expect 3 args: JSON path, numClusters, numSites
        if (args.length < 3) {
            if (rank == 0) {
                System.out.println(
                        "Usage: mpjrun.sh -np <procs> DistributedKMeansMPJ <sites.json> <numClusters> <numSites>");
            }
            MPI.Finalize();
            return;
        }

        // ✅ Always pick LAST THREE args because MPJ injects its own before yours
        String sitesFile = args[args.length - 3];
        int k = Integer.parseInt(args[args.length - 2]);
        int numSitesRequested = Integer.parseInt(args[args.length - 1]);

        if (rank == 0) {
            System.out.println("[MASTER] Trying to load file: " + sitesFile +
                    " (exists? " + new File(sitesFile).exists() + ")");
            runMaster(sitesFile, k, size, numSitesRequested);
        } else {
            runWorker(k);
        }

        MPI.Finalize();
    }

    // --- Master ---
    private static void runMaster(String sitesFile, int k, int size, int numSitesRequested) throws Exception {
        
        System.out.println("[MASTER] Loading " + numSitesRequested + " sites...");

        // Load ONLY the requested number of sites, no Integer.MAX_VALUE anymore
        List<AccumulationSite> allSites = SiteLoader.loadSites(sitesFile, numSitesRequested);
        System.out.println("[MASTER] Loaded " + allSites.size() + " sites.");

        List<double[]> dataPoints = extractCoordinates(allSites);

        // Split data into chunks for workers
        List<List<double[]>> chunks = splitData(dataPoints, size - 1);

        System.out.println("[MASTER] Sending data chunks to workers...");
        for (int worker = 1; worker < size; worker++) {
            Object[] sendBuf = new Object[] { chunks.get(worker - 1) };
            MPI.COMM_WORLD.Send(sendBuf, 0, 1, MPI.OBJECT, worker, 0);
        }

        // Initialize centroids randomly
        List<double[]> centroids = initializeRandomCentroids(dataPoints, k);
        System.out.println("[MASTER] Initial centroids: " + Arrays.deepToString(centroids.toArray()));

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            System.out.println("[MASTER] Iteration " + (iter + 1));

            // Broadcast centroids to all workers
            Object[] centroidArray = centroids.toArray();
            MPI.COMM_WORLD.Bcast(centroidArray, 0, k, MPI.OBJECT, 0);

            // Prepare accumulator for partial results
            double[][] globalSums = new double[k][2];
            int[] globalCounts = new int[k];

            // Receive partial results from workers
            for (int worker = 1; worker < size; worker++) {
                Object[] recvBuf = new Object[1];
                MPI.COMM_WORLD.Recv(recvBuf, 0, 1, MPI.OBJECT, worker, 1);

                PartialResult pr = (PartialResult) recvBuf[0];
                mergePartialResults(pr, globalSums, globalCounts);
            }

            // Update centroids
            centroids = recomputeCentroids(globalSums, globalCounts);
        }

        System.out.println("[MASTER] Final centroids:");
        for (int i = 0; i < centroids.size(); i++) {
            System.out.println("Cluster " + i + ": " + Arrays.toString(centroids.get(i)));
        }
    }

    // --- Worker ---
    private static void runWorker(int k) throws Exception {
        int rank = MPI.COMM_WORLD.Rank();

        // ✅ 1. Receive the chunk of data for this worker
        Object[] recvBuf = new Object[1];
        MPI.COMM_WORLD.Recv(recvBuf, 0, 1, MPI.OBJECT, 0, 0);

        @SuppressWarnings("unchecked")
        List<double[]> localData = (List<double[]>) recvBuf[0];

        System.out.println("[WORKER " + rank + "] Received " + localData.size() + " points.");

        // ✅ 2. Iterate for each iteration of K-means
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {

            // ✅ 2a. Receive the current centroids from master
            Object[] centroidArray = new Object[k];
            MPI.COMM_WORLD.Bcast(centroidArray, 0, k, MPI.OBJECT, 0);

            // Convert to a List<double[]>
            List<double[]> centroids = new ArrayList<>(k);
            for (Object obj : centroidArray) {
                if (obj != null) {
                    centroids.add((double[]) obj);
                } else {
                    System.err.println("[WORKER " + rank + "] Warning: received null centroid");
                    centroids.add(new double[] { 0.0, 0.0 }); // fallback
                }
            }

            // ✅ 2b. Compute partial result for this worker's local data
            PartialResult pr = computePartialResult(localData, centroids);

            // ✅ 2c. Send partial result back to master
            MPI.COMM_WORLD.Send(new Object[] { pr }, 0, 1, MPI.OBJECT, 0, 1);

            System.out.println("[WORKER " + rank + "] Iteration " + (iter + 1) + " done.");
        }

        System.out.println("[WORKER " + rank + "] Finished all iterations.");
    }

    // --- Utility Methods ---
    private static List<double[]> extractCoordinates(List<AccumulationSite> sites) {
        List<double[]> coords = new ArrayList<>();
        for (AccumulationSite s : sites) {
            coords.add(new double[] { s.latitude, s.longitude });
        }
        return coords;
    }

    private static List<List<double[]>> splitData(List<double[]> data, int numChunks) {
        List<List<double[]>> chunks = new ArrayList<>();
        int chunkSize = (int) Math.ceil((double) data.size() / numChunks);
    
        for (int i = 0; i < data.size(); i += chunkSize) {
            // ✅ Convert subList into a real ArrayList (serializable)
            List<double[]> chunk = new ArrayList<>(data.subList(i, Math.min(i + chunkSize, data.size())));
            chunks.add(chunk);
        }
        return chunks;
    }
    

    private static List<double[]> initializeRandomCentroids(List<double[]> points, int k) {
        List<double[]> shuffled = new ArrayList<>(points);
        Collections.shuffle(shuffled, new Random());
        return new ArrayList<>(shuffled.subList(0, k));
    }

    private static PartialResult computePartialResult(List<double[]> data, List<double[]> centroids) {
        int k = centroids.size();
        double[][] sums = new double[k][2];
        int[] counts = new int[k];

        for (double[] point : data) {
            int cluster = nearestCluster(point, centroids);
            sums[cluster][0] += point[0];
            sums[cluster][1] += point[1];
            counts[cluster]++;
        }
        return new PartialResult(sums, counts);
    }

    private static int nearestCluster(double[] p, List<double[]> centroids) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < centroids.size(); i++) {
            double d = Math.pow(p[0] - centroids.get(i)[0], 2) + Math.pow(p[1] - centroids.get(i)[1], 2);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private static void mergePartialResults(PartialResult pr, double[][] globalSums, int[] globalCounts) {
        for (int i = 0; i < pr.counts.length; i++) {
            globalSums[i][0] += pr.sums[i][0];
            globalSums[i][1] += pr.sums[i][1];
            globalCounts[i] += pr.counts[i];
        }
    }

    private static List<double[]> recomputeCentroids(double[][] sums, int[] counts) {
        List<double[]> newCentroids = new ArrayList<>();
        for (int i = 0; i < sums.length; i++) {
            if (counts[i] > 0) {
                newCentroids.add(new double[] {
                        sums[i][0] / counts[i],
                        sums[i][1] / counts[i]
                });
            } else {
                newCentroids.add(new double[] { sums[i][0], sums[i][1] }); // unchanged if empty
            }
        }
        return newCentroids;
    }

    // --- Partial result container ---
    public static class PartialResult implements Serializable {
        double[][] sums;
        int[] counts;

        public PartialResult(double[][] s, int[] c) {
            sums = s;
            counts = c;
        }
    }
}
