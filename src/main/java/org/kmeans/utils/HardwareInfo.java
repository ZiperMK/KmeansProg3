package org.kmeans.utils;

public class HardwareInfo {
    public static void printSystemStats() {
        System.out.println("Hardware Info:");
        System.out.println("- CPU cores available: " + getCoreCount());
        System.out.println("- Max memory (MB): " + getMaxMemoryMB());
    }

    public static int getCoreCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static long getMaxMemoryMB() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }
    public static int estimateMaxSites() {
        long bytesPerSite = 500;
        long usableBytes = (long)(getMaxMemoryMB() * 0.7) * 1024 * 1024;
        return (int)(usableBytes / bytesPerSite);
    }
    
    public static int estimateMaxClusters(int maxSites) {
        return Math.min(1000, maxSites / 5);
    }
    
}
