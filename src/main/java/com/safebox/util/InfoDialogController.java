package com.safebox.util;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for styled information dialogs.
 */
public class InfoDialogController {

    @FXML
    private VBox rootPane;
    @FXML
    private Label iconLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label headerLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Button okButton;

    private boolean confirmed;

    /**
     * Configures dialog content.
     *
     * @param icon    emoji or symbol for the dialog
     * @param title   window title
     * @param header  headline text
     * @param message body text
     */
    public void init(String icon, String title, String header, String message) {
        iconLabel.setText(icon);
        titleLabel.setText(title);
        headerLabel.setText(header);
        headerLabel.setVisible(header != null && !header.isBlank());
        headerLabel.setManaged(header != null && !header.isBlank());
        messageLabel.setText(message);
        okButton.setText(I18n.get("btn.ok"));
    }

    /**
     * Applies a visual variant: info, warning, or error.
     *
     * @param variant style variant name
     */
    public void setVariant(String variant) {
        rootPane.getStyleClass().removeAll("warning", "error");
        if (variant != null && !variant.isBlank()) {
            rootPane.getStyleClass().add(variant);
        }
    }

    @FXML
    private void onOk() {
        confirmed = true;
        close();
    }

    /**
     * Returns whether the user confirmed the dialog.
     *
     * @return true after OK
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    private void close() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }
}
