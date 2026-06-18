package com.safebox.util;

import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Custom title bar controls for undecorated windows.
 */
public final class WindowChrome {

    private WindowChrome() {
    }

    /**
     * Wires minimize, close and drag-to-move on a custom title bar.
     *
     * @param stage          application stage
     * @param dragRegion     area used to drag the window
     * @param minimizeButton minimize button
     * @param closeButton    close button
     */
    public static void install(Stage stage, Region dragRegion, Button minimizeButton, Button closeButton) {
        final double[] dragOffset = new double[2];

        dragRegion.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            dragOffset[0] = event.getSceneX();
            dragOffset[1] = event.getSceneY();
        });
        dragRegion.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            stage.setX(event.getScreenX() - dragOffset[0]);
            stage.setY(event.getScreenY() - dragOffset[1]);
        });

        minimizeButton.setOnAction(event -> stage.setIconified(true));
        closeButton.setOnAction(event ->
                stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
    }
}
