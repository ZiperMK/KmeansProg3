package org.kmeans.utils;

import org.openstreetmap.gui.jmapviewer.MapMarkerCircle;
import org.openstreetmap.gui.jmapviewer.Coordinate;

import java.awt.*;

public class LabeledMapMarker extends MapMarkerCircle {
    private final String label;

    public LabeledMapMarker(Coordinate coord, double radius, String label, Color fillColor) {
        super(coord, radius);
        this.label = label;
        setBackColor(fillColor);
        setColor(Color.BLACK); // outline
        setStroke(new BasicStroke(2));
    }

    @Override
    public void paint(Graphics g, Point position, int radius) {
        super.paint(g, position, radius);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(label, position.x - label.length() * 3, position.y - radius - 5);
    }
}
