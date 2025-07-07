package org.kmeans.gui;

import org.kmeans.utils.AccumulationSite;
import org.kmeans.utils.ClusterCenter;
import org.kmeans.utils.LabeledMapMarker;
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
    
        // Enable interactivity
        map.setZoomControlsVisible(true);
        map.setScrollWrapEnabled(true);
        map.setTileGridVisible(false);
    
        setLayout(new BorderLayout());
        add(map, BorderLayout.CENTER);
    
        // Add reset button
        JButton resetButton = new JButton("Reset View");
        resetButton.addActionListener(e -> fitViewport(clusters));
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(resetButton);
        add(controlPanel, BorderLayout.NORTH);
    
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    
        drawClusters(clusters);
        fitViewport(clusters);
    
        setVisible(true);
    }
    

    private void drawClusters(List<ClusterCenter> clusters) {
        for (int i = 0; i < clusters.size(); i++) {
            ClusterCenter center = clusters.get(i);
            Color color = COLORS[i % COLORS.length];

            // Draw facility (cluster center) as a larger dot
            Coordinate centerCoord = new Coordinate(center.latitude, center.longitude);
            LabeledMapMarker labeledCenter = new LabeledMapMarker(centerCoord, 0.06, "Cluster " + (i+1), color.darker());
            map.addMapMarker(labeledCenter);

            // Draw accumulation sites assigned to this facility
            for (AccumulationSite site : center.assignedSites) {
                MapMarkerDot siteDot = new MapMarkerDot(site.latitude, site.longitude);
                siteDot.setBackColor(color);
                map.addMapMarker(siteDot);
            }

        }
    }

    private void fitViewport(List<ClusterCenter> clusters) {
        map.setDisplayToFitMapMarkers(); // Automatically fits all visible markers
    }
    
}
