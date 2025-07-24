package org.kmeans.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

public class ResultSaver {
    public static void saveResult(ClusteringResult result, String path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ClusteringResult loadResult(String path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(path), ClusteringResult.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
