package com.safebox.util;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for three-option choice dialogs.
 */
public class ChoiceDialogController {

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
    private Button firstButton;
    @FXML
    private Button secondButton;
    @FXML
    private Button cancelButton;

    private Integer choice;

    /**
     * Configures the choice dialog.
     */
    public void init(String icon, String title, String header, String message,
                     String firstText, String secondText) {
        iconLabel.setText(icon);
        titleLabel.setText(title);
        headerLabel.setText(header);
        headerLabel.setVisible(header != null && !header.isBlank());
        headerLabel.setManaged(header != null && !header.isBlank());
        messageLabel.setText(message);
        firstButton.setText(firstText);
        secondButton.setText(secondText);
        cancelButton.setText(I18n.get("btn.cancel"));
    }

    /**
     * Applies a visual variant to the dialog.
     *
     * @param variant info, warning, or error
     */
    public void setVariant(String variant) {
        rootPane.getStyleClass().removeAll("warning", "error");
        if (variant != null && !variant.isBlank()) {
            rootPane.getStyleClass().add(variant);
        }
    }

    @FXML
    private void onFirst() {
        choice = 0;
        close();
    }

    @FXML
    private void onSecond() {
        choice = 1;
        close();
    }

    @FXML
    private void onCancel() {
        choice = null;
        close();
    }

    /**
     * Returns selected option index or null if cancelled.
     *
     * @return choice index
     */
    public Integer getChoice() {
        return choice;
    }

    private void close() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
