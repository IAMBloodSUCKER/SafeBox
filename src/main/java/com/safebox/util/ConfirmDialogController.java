package com.safebox.util;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for styled confirmation dialogs.
 */
public class ConfirmDialogController {

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
    private Button cancelButton;
    @FXML
    private Button confirmButton;

    private Boolean confirmed;

    /**
     * Configures dialog content.
     *
     * @param icon        emoji or symbol
     * @param title       window title
     * @param header      headline
     * @param message     body text
     * @param confirmText confirm button label
     */
    public void init(String icon, String title, String header, String message, String confirmText) {
        iconLabel.setText(icon);
        titleLabel.setText(title);
        headerLabel.setText(header);
        headerLabel.setVisible(header != null && !header.isBlank());
        headerLabel.setManaged(header != null && !header.isBlank());
        messageLabel.setText(message);
        cancelButton.setText(I18n.get("btn.cancel"));
        confirmButton.setText(confirmText);
    }

    /**
     * Applies a visual variant: warning or error.
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
    private void onConfirm() {
        confirmed = true;
        close();
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        close();
    }

    /**
     * Returns the user's choice, or empty if dialog was closed.
     *
     * @return optional boolean
     */
    public Boolean getConfirmed() {
        return confirmed;
    }

    private void close() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        stage.close();
    }
}
