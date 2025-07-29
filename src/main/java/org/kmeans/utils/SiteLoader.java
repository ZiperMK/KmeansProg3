package org.kmeans.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class SiteLoader {

    private static final Random random = new Random();
    private static final double MAX_CAPACITY = 30000.0;
    private static final List<Geometry> europeLandPolygons = new ArrayList<>();
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static boolean landPolygonsLoaded = false;

    public static List<AccumulationSite> loadSites(String jsonPath, int count) {
        List<AccumulationSite> allSites = new ArrayList<>();
    
        try {
            File file = new File(jsonPath);
            if (!file.exists()) {
                System.out.println("⚠ File not found, generating random land-based sites...");
                return generateRandomSites(count);
            }
    
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(file);
    
            for (JsonNode node : root) {
                double lat, lon, cap;
    
                if (node.has("lat") && node.has("lon")) {
                    lat = node.get("lat").asDouble();
                    lon = node.get("lon").asDouble();
                    cap = node.get("disposal_tons_per_year").asDouble();
                } else if (node.has("latitude") && node.has("longitude")) {
                    lat = node.get("latitude").asDouble();
                    lon = node.get("longitude").asDouble();
                    cap = node.get("capacity").asDouble();
                } else {
                    continue; // Skip unrecognized entry
                }
    
                allSites.add(new AccumulationSite(lat, lon, cap));
            }
    
            if (allSites.isEmpty()) {
                System.out.println("⚠ No valid entries in file. Falling back to random generation.");
                return generateRandomSites(count);
            }
    
            // If count is smaller than half of the available dataset → pick random
            if (count <= allSites.size() / 2) {
                Collections.shuffle(allSites);
                return new ArrayList<>(allSites.subList(0, count));
            }
    
            List<AccumulationSite> result = new ArrayList<>(allSites);
    
            // If not enough, fill with random
            if (result.size() < count) {
                System.out.println("⚠ Only " + result.size() + " loaded. Generating " + (count - result.size()) + " random sites...");
                result.addAll(generateRandomSites(count - result.size()));
            } else {
                // Trim to exactly count entries
                result = result.subList(0, count);
            }
    
            return result;
    
        } catch (Exception e) {
            System.out.println("⚠ Failed to read file or parse. Falling back to random generation.");
            return generateRandomSites(count);
        }
    }
    

    public static void loadEuropeLandPolygons(String geoJsonPath) throws Exception {
        if (landPolygonsLoaded)
            return;
        String content = new String(Files.readAllBytes(new File(geoJsonPath).toPath()));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content);
        GeoJsonReader reader = new GeoJsonReader(geometryFactory);

        for (JsonNode feature : root.get("features")) {
            String region = feature.get("properties").get("region_un").asText();
            String name = feature.get("properties").get("ADMIN").asText();

            if (!region.equals("Europe"))
                continue;
            if (name.equals("Russia") || name.equals("Turkey"))
                continue;

            String geomStr = feature.get("geometry").toString();
            Geometry geom = reader.read(geomStr);
            europeLandPolygons.add(geom);
        }

        landPolygonsLoaded = true;
    }

    private static List<AccumulationSite> generateRandomSites(int count) {
        // Lazy load only if not already loaded
        if (!landPolygonsLoaded) {
            try {
                InputStream stream = SiteLoader.class.getResourceAsStream("/europe.geojson");
                if (stream == null) {
                    throw new IOException("GeoJSON resource not found in classpath");
                }
    
                String content = new String(stream.readAllBytes());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(content);
                GeoJsonReader reader = new GeoJsonReader(geometryFactory);
    
                Set<String> allowedCountries = Set.of(
                    "Austria", "Belgium", "Bulgaria", "Croatia", "Cyprus", "Czech Republic", "Czechia", "Denmark", "Estonia",
                    "Finland", "France", "Germany", "Greece", "Hungary", "Iceland", "Ireland", "Italy", "Latvia",
                    "Lithuania", "Luxembourg", "Malta", "Netherlands", "Norway", "Poland", "Portugal", "Romania",
                    "Slovakia", "Slovenia", "Spain", "Sweden", "Switzerland", "United Kingdom"
                );
    
                for (JsonNode feature : root.get("features")) {
                    JsonNode props = feature.get("properties");
                    if (props == null) continue;
    
                    String name = props.has("name") ? props.get("name").asText() : "";
                    if (!allowedCountries.contains(name)) continue;
    
                    String geomStr = feature.get("geometry").toString();
                    Geometry geom = reader.read(geomStr);
                    europeLandPolygons.add(geom);
                }
    
                landPolygonsLoaded = true;
    
            } catch (Exception e) {
                throw new RuntimeException("Failed to load Europe land polygons", e);
            }
        }
    
        List<AccumulationSite> generated = new ArrayList<>();
        int attempts = 0;
    
        while (generated.size() < count) {
            attempts++;
    
            double lat = 35 + random.nextDouble() * 25;
            double lon = -10 + random.nextDouble() * 40;
    
            Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
    
            for (Geometry polygon : europeLandPolygons) {
                if (polygon.contains(point)) {
                    double cap = 100 + random.nextDouble() * (MAX_CAPACITY - 100);
                    generated.add(new AccumulationSite(lat, lon, cap));
                    break;
                }
            }
    
            if (attempts % 100000 == 0) {
                System.out.println("Still trying... " + generated.size() + " / " + count);
            }
        }
    
        System.out.println("Generated " + count + " random land-based sites after " + attempts + " attempts.");
        return generated;
    }
}
