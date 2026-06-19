package com.safebox.util;

import com.safebox.service.PasswordManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.util.Arrays;

/**
 * Controller for the change master password dialog.
 */
public class ChangePasswordDialogController {

    @FXML
    private Label iconLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label headerLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Label newLabel;
    @FXML
    private PasswordField newField;
    @FXML
    private Label confirmLabel;
    @FXML
    private PasswordField confirmField;
    @FXML
    private Button cancelButton;
    @FXML
    private Button confirmButton;

    private char[] newPassword;
    private boolean submitted;

    /**
     * Configures dialog labels.
     */
    public void init() {
        iconLabel.setText("\uD83D\uDD12");
        titleLabel.setText(I18n.get("changePassword.title"));
        headerLabel.setText(I18n.get("changePassword.header"));
        messageLabel.setText(I18n.get("changePassword.message"));
        newLabel.setText(I18n.get("changePassword.new"));
        confirmLabel.setText(I18n.get("changePassword.confirm"));
        cancelButton.setText(I18n.get("btn.cancel"));
        confirmButton.setText(I18n.get("changePassword.submit"));
    }

    @FXML
    private void onConfirm() {
        char[] newPwd = newField.getText().toCharArray();
        char[] confirm = confirmField.getText().toCharArray();

        if (newPwd.length == 0) {
            messageLabel.setText(I18n.get("error.vault.masterPasswordEmpty"));
            wipe(newPwd, confirm);
            return;
        }
        if (!Arrays.equals(newPwd, confirm)) {
            messageLabel.setText(I18n.get("error.passwords.mismatch"));
            wipe(newPwd, confirm);
            return;
        }

        newPassword = newPwd;
        wipe(confirm);
        submitted = true;
        close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    /**
     * Returns whether the user submitted the form.
     */
    public boolean isSubmitted() {
        return submitted;
    }

    /**
     * Returns the new master password entered by the user.
     */
    public char[] getNewPassword() {
        return newPassword;
    }

    private void close() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        stage.close();
    }

    private static void wipe(char[]... arrays) {
        for (char[] chars : arrays) {
            PasswordManager.wipe(chars);
        }
    }
}
