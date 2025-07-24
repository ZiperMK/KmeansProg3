package org.kmeans.utils;

import java.util.*;
import java.util.concurrent.*;

public class KMeansParallel {

    public static ClusteringResult cluster(List<AccumulationSite> sites, int k, int maxIterations) {
        List<ClusterCenter> centers = new ArrayList<>();
        Random rand = new Random();

        // Initialize random cluster centers
        for (int i = 0; i < k; i++) {
            AccumulationSite randSite = sites.get(rand.nextInt(sites.size()));
            centers.add(new ClusterCenter(randSite.latitude, randSite.longitude));
        }

        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);

        long startTime = System.nanoTime();
        int cycles = 0;

        try {
            for (int iter = 0; iter < maxIterations; iter++) {
                cycles++;

                // Clear assigned sites
                for (ClusterCenter center : centers)
                    center.assignedSites.clear();

                // Prepare one assignment map per thread
                List<Map<ClusterCenter, List<AccumulationSite>>> threadAssignments = new ArrayList<>();

                // Chunk sites into batches for each thread
                List<List<AccumulationSite>> batches = chunkSites(sites, cores);
                List<Future<?>> futures = new ArrayList<>();

                for (List<AccumulationSite> batch : batches) {
                    Future<?> future = executor.submit(() -> {
                        Map<ClusterCenter, List<AccumulationSite>> localMap = new HashMap<>();
                        for (ClusterCenter center : centers)
                            localMap.put(center, new ArrayList<>());

                        for (AccumulationSite site : batch) {
                            ClusterCenter closest = null;
                            double minDist = Double.MAX_VALUE;
                            for (ClusterCenter center : centers) {
                                double dist = site.distanceTo(center);
                                if (dist < minDist) {
                                    minDist = dist;
                                    closest = center;
                                }
                            }
                            localMap.get(closest).add(site);
                        }

                        synchronized (threadAssignments) {
                            threadAssignments.add(localMap);
                        }
                    });

                    futures.add(future);
                }

                for (Future<?> f : futures) f.get(); // Wait for all threads

                // Merge thread-local assignments into global lists
                for (Map<ClusterCenter, List<AccumulationSite>> local : threadAssignments) {
                    for (Map.Entry<ClusterCenter, List<AccumulationSite>> entry : local.entrySet()) {
                        entry.getKey().assignedSites.addAll(entry.getValue());
                    }
                }

                // Update centers in parallel
                List<Future<Boolean>> updateTasks = new ArrayList<>();
                for (ClusterCenter center : centers) {
                    updateTasks.add(executor.submit(center::updateCenter));
                }

                boolean changed = false;
                for (Future<Boolean> update : updateTasks) {
                    if (update.get()) changed = true;
                }

                if (!changed) break; // early convergence
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        long endTime = System.nanoTime();
        long durationMillis = (endTime - startTime) / 1_000_000;

        return new ClusteringResult(centers, cycles, durationMillis);
    }

    // Helper method to split sites into N roughly equal chunks
    private static List<List<AccumulationSite>> chunkSites(List<AccumulationSite> sites, int numChunks) {
        List<List<AccumulationSite>> chunks = new ArrayList<>();
        int chunkSize = (int) Math.ceil((double) sites.size() / numChunks);
        for (int i = 0; i < sites.size(); i += chunkSize) {
            chunks.add(sites.subList(i, Math.min(sites.size(), i + chunkSize)));
        }
        return chunks;
    }
}
