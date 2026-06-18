package com.safebox.controller;

import com.safebox.MainApp;
import com.safebox.model.PasswordEntry;
import com.safebox.util.DialogHelper;
import com.safebox.util.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * Add/edit password entry dialog controller.
 */
public class EditController {

    @FXML
    private Label siteLabel;
    @FXML
    private Label loginLabel;
    @FXML
    private Label passwordLabel;
    @FXML
    private Label notesLabel;
    @FXML
    private TextField siteField;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordVisibleField;
    @FXML
    private TextArea notesArea;
    @FXML
    private Button togglePasswordButton;
    @FXML
    private Button generateButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private MainApp app;
    private PasswordEntry entry;
    private Consumer<PasswordEntry> onSaved;
    private boolean passwordVisible;

    /**
     * Initializes the edit dialog.
     *
     * @param app     main application
     * @param entry   entry to edit, or template for new entry
     * @param onSaved callback after successful save
     */
    public void init(MainApp app, PasswordEntry entry, Consumer<PasswordEntry> onSaved) {
        this.app = app;
        this.onSaved = onSaved;
        this.entry = entry == null ? new PasswordEntry() : copyEntry(entry);

        siteField.setText(nullToEmpty(this.entry.getSite()));
        loginField.setText(nullToEmpty(this.entry.getLogin()));
        passwordField.setText(nullToEmpty(this.entry.getPassword()));
        passwordVisibleField.setText(nullToEmpty(this.entry.getPassword()));
        notesArea.setText(nullToEmpty(this.entry.getNotes()));

        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        passwordField.textProperty().addListener((obs, o, n) -> {
            if (!passwordVisible) {
                passwordVisibleField.setText(n);
            }
        });
        passwordVisibleField.textProperty().addListener((obs, o, n) -> {
            if (passwordVisible) {
                passwordField.setText(n);
            }
        });
        applyLanguage();
    }

    @FXML
    private void onTogglePassword() {
        passwordVisible = !passwordVisible;
        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);
        passwordVisibleField.setVisible(passwordVisible);
        passwordVisibleField.setManaged(passwordVisible);
        updateTogglePasswordText();
        if (passwordVisible) {
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.requestFocus();
        } else {
            passwordField.setText(passwordVisibleField.getText());
        }
    }

    @FXML
    private void onGenerate() {
        openGenerator(password -> {
            passwordField.setText(password);
            passwordVisibleField.setText(password);
        });
    }

    @FXML
    private void onSave() {
        app.getSessionManager().touch();
        entry.setSite(siteField.getText().trim());
        entry.setLogin(loginField.getText().trim());
        entry.setPassword(passwordVisible ? passwordVisibleField.getText() : passwordField.getText());
        entry.setNotes(notesArea.getText());

        try {
            var key = app.getSessionManager().getEncryptionKey();
            app.getPasswordManager().saveEntry(entry, key);
            if (onSaved != null) {
                onSaved.accept(entry);
            }
            close();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void applyLanguage() {
        siteLabel.setText(I18n.get("edit.site"));
        loginLabel.setText(I18n.get("edit.login"));
        passwordLabel.setText(I18n.get("edit.password"));
        notesLabel.setText(I18n.get("edit.notes"));
        generateButton.setText(I18n.get("edit.generate"));
        saveButton.setText(I18n.get("edit.save"));
        cancelButton.setText(I18n.get("edit.cancel"));
        updateTogglePasswordText();
    }

    private void updateTogglePasswordText() {
        togglePasswordButton.setText(passwordVisible ? I18n.get("edit.hide") : I18n.get("edit.show"));
    }

    private void openGenerator(Consumer<String> onGenerated) {
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/generator.fxml"));
            var root = loader.<javafx.scene.Parent>load();
            GeneratorController controller = loader.getController();
            controller.init(app, onGenerated);

            Stage dialog = new Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initOwner(((Stage) siteField.getScene().getWindow()));
            dialog.setTitle(I18n.get("generator.title"));
            var scene = new javafx.scene.Scene(root);
            String css = "dark".equals(app.getCurrentTheme()) ? "/css/dark.css" : "/css/light.css";
            scene.getStylesheets().add(getClass().getResource(css).toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            showError(I18n.get("edit.error.generator"));
        }
    }

    private void close() {
        Stage stage = (Stage) siteField.getScene().getWindow();
        stage.close();
    }

    private static PasswordEntry copyEntry(PasswordEntry source) {
        PasswordEntry copy = new PasswordEntry();
        copy.setId(source.getId());
        copy.setSite(source.getSite());
        copy.setLogin(source.getLogin());
        copy.setPassword(source.getPassword());
        copy.setNotes(source.getNotes());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        DialogHelper.showError(app.getPrimaryStage(), app.getCurrentTheme(), message);
    }
}
