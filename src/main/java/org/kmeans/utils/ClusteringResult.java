package org.kmeans.utils;

import java.util.List;

public class ClusteringResult {
    public final List<ClusterCenter> centers;
    public final int cycles;
    public final long durationMillis;

    public ClusteringResult(List<ClusterCenter> centers, int cycles, long durationMillis) {
        this.centers = centers;
        this.cycles = cycles;
        this.durationMillis = durationMillis;
    }
}
