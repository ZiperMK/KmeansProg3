package org.kmeans.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SiteLoader {

    private static final Random random = new Random();
    private static final double MAX_CAPACITY = 30000.0;

    public static List<AccumulationSite> loadSites(String jsonPath, int requestedCount) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<AccumulationSite> result = new ArrayList<>();

        File jsonFile = new File(jsonPath);
        JsonNode root = mapper.readTree(jsonFile);

        List<AccumulationSite> dataset = new ArrayList<>();
        for (JsonNode node : root) {
            double lat = node.get("lat").asDouble();
            double lon = node.get("lon").asDouble();
            double cap = node.get("disposal_tons_per_year").asDouble();
            dataset.add(new AccumulationSite(lat, lon, cap));
        }

        Collections.shuffle(dataset, random);

        if (requestedCount <= dataset.size()) {
            result.addAll(dataset.subList(0, requestedCount));
        } else {
            result.addAll(dataset);
            result.addAll(generateRandomSites(requestedCount - dataset.size()));
        }

        return result;
    }

    private static List<AccumulationSite> generateRandomSites(int count) {
        List<AccumulationSite> generated = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double lat = 35 + random.nextDouble() * 25;
            double lon = -10 + random.nextDouble() * 40;
            double cap = 100 + random.nextDouble() * (MAX_CAPACITY - 100);
            generated.add(new AccumulationSite(lat, lon, cap));
        }
        return generated;
    }
}
