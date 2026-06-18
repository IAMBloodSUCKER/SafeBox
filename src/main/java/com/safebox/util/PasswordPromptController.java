package com.safebox.util;

import com.safebox.service.PasswordManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;

/**
 * Controller for export/import password prompt dialog.
 */
public class PasswordPromptController {

    @FXML
    private Label iconLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label headerLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Label passwordLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private VBox confirmBox;
    @FXML
    private Label confirmLabel;
    @FXML
    private PasswordField confirmField;
    @FXML
    private Button cancelButton;
    @FXML
    private Button confirmButton;

    private char[] result;
    private boolean requireConfirmation;

    /**
     * Configures the password prompt dialog.
     *
     * @param title                dialog title
     * @param header               headline
     * @param message              body text
     * @param requireConfirmation  whether confirm field is shown
     */
    public void init(String title, String header, String message, boolean requireConfirmation) {
        this.requireConfirmation = requireConfirmation;
        iconLabel.setText("\uD83D\uDD12");
        titleLabel.setText(title);
        headerLabel.setText(header);
        headerLabel.setVisible(header != null && !header.isBlank());
        headerLabel.setManaged(header != null && !header.isBlank());
        messageLabel.setText(message);
        passwordLabel.setText(I18n.get("transfer.password"));
        confirmLabel.setText(I18n.get("transfer.password.confirm"));
        confirmBox.setVisible(requireConfirmation);
        confirmBox.setManaged(requireConfirmation);
        cancelButton.setText(I18n.get("btn.cancel"));
        confirmButton.setText(I18n.get("btn.ok"));
    }

    @FXML
    private void onConfirm() {
        char[] password = passwordField.getText().toCharArray();
        if (password.length < 8) {
            messageLabel.setText(I18n.get("error.transfer.passwordLength"));
            PasswordManager.wipe(password);
            return;
        }
        if (requireConfirmation) {
            char[] confirm = confirmField.getText().toCharArray();
            if (!Arrays.equals(password, confirm)) {
                messageLabel.setText(I18n.get("error.passwords.mismatch"));
                PasswordManager.wipe(password);
                PasswordManager.wipe(confirm);
                return;
            }
            PasswordManager.wipe(confirm);
        }
        result = password;
        close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    /**
     * Returns entered password or null if cancelled.
     *
     * @return password characters
     */
    public char[] getPassword() {
        return result;
    }

    private void close() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        stage.close();
    }
}
