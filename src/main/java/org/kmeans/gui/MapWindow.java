package org.kmeans.gui;

import org.kmeans.utils.AccumulationSite;
import org.kmeans.utils.ClusterCenter;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerCircle;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.Coordinate;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MapWindow extends JFrame {
    private JMapViewer map;
    private static final Color[] COLORS = {
        Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.ORANGE,
        Color.CYAN, Color.PINK, Color.YELLOW, Color.GRAY, Color.LIGHT_GRAY
    };

    public MapWindow(List<ClusterCenter> clusters) {
        super("KMeans Waste Clustering");

        map = new JMapViewer();
        setLayout(new BorderLayout());
        add(map, BorderLayout.CENTER);

        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        drawClusters(clusters);
        fitViewport(clusters);
    }

    private void drawClusters(List<ClusterCenter> clusters) {
        for (int i = 0; i < clusters.size(); i++) {
            ClusterCenter center = clusters.get(i);
            Color color = COLORS[i % COLORS.length];

            // Draw facility (cluster center) as a larger dot
            MapMarkerDot centerDot = new MapMarkerDot(center.latitude, center.longitude);
            centerDot.setBackColor(color.darker());
            map.addMapMarker(centerDot);
            
            // Draw accumulation sites assigned to this facility
            for (AccumulationSite site : center.assignedSites) {
                MapMarkerDot siteDot = new MapMarkerDot(site.latitude, site.longitude);
                siteDot.setBackColor(color);
                map.addMapMarker(siteDot);
            }
            
        }
    }

    private void fitViewport(List<ClusterCenter> clusters) {
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (ClusterCenter c : clusters) {
            for (AccumulationSite s : c.assignedSites) {
                minLat = Math.min(minLat, s.latitude);
                maxLat = Math.max(maxLat, s.latitude);
                minLon = Math.min(minLon, s.longitude);
                maxLon = Math.max(maxLon, s.longitude);
            }
        }

        double centerLat = (minLat + maxLat) / 2;
        double centerLon = (minLon + maxLon) / 2;

        map.setDisplayPosition(new Coordinate(centerLat, centerLon), 4);
    }
}
