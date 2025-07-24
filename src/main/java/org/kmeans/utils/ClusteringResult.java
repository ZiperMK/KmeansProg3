package org.kmeans.utils;

import java.util.List;

public class ClusteringResult {
    public List<ClusterCenter> centers;
    public int cycles;
    public long durationMillis;

    // ✅ Default constructor needed by Jackson
    public ClusteringResult() {}

    public ClusteringResult(List<ClusterCenter> centers, int cycles, long durationMillis) {
        this.centers = centers;
        this.cycles = cycles;
        this.durationMillis = durationMillis;
    }
}
