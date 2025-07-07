package org.kmeans.utils;

public class HardwareInfo {
    public static void printSystemStats() {
        int cores = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        System.out.println("Hardware Info:");
        System.out.println("- CPU cores available: " + cores);
        System.out.println("- Max memory (MB): " + maxMemory);
    }
}