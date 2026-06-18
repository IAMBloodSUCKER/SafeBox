package com.safebox.util;

import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/**
 * Applies rounded clipping to transparent window roots.
 */
public final class WindowClip {

    private WindowClip() {
    }

    /**
     * Clips a node to a rounded rectangle.
     *
     * @param root   root node to clip
     * @param radius corner radius in pixels
     */
    public static void applyRounded(Parent root, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        if (root instanceof Region region) {
            clip.widthProperty().bind(region.widthProperty());
            clip.heightProperty().bind(region.heightProperty());
        } else {
            root.layoutBoundsProperty().addListener((obs, oldBounds, bounds) -> {
                clip.setWidth(bounds.getWidth());
                clip.setHeight(bounds.getHeight());
            });
        }
        root.setClip(clip);
    }
}
