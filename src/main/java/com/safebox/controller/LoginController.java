package com.safebox.controller;

import com.safebox.MainApp;
import com.safebox.service.PasswordManager;
import com.safebox.util.DialogHelper;
import com.safebox.util.AppVersion;
import com.safebox.util.DonationInfo;
import com.safebox.util.I18n;
import com.safebox.util.WindowChrome;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.crypto.SecretKey;
import java.util.Arrays;

/**
 * Login and first-run vault setup controller.
 */
public class LoginController {

    @FXML
    private Label appNameLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Label masterPasswordLabel;
    @FXML
    private Label confirmPasswordLabel;
    @FXML
    private VBox setupBox;
    @FXML
    private PasswordField masterPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField masterPasswordVisibleField;
    @FXML
    private TextField confirmPasswordVisibleField;
    @FXML
    private Button loginButton;
    @FXML
    private Button languageButton;
    @FXML
    private Hyperlink forgotPasswordLink;
    @FXML
    private HBox titleBar;
    @FXML
    private Button minimizeButton;
    @FXML
    private Button closeButton;
    @FXML
    private Label supportLoginLabel;
    @FXML
    private Label authorLoginLabel;
    @FXML
    private Label walletLoginLabel;
    @FXML
    private Button copyWalletLoginButton;
    @FXML
    private Hyperlink supportDetailsLink;
    @FXML
    private Label versionLabel;

    private MainApp app;
    private boolean firstRun;

    /**
     * Initializes the controller with application context.
     *
     * @param app main application
     */
    public void init(MainApp app) {
        this.app = app;
        this.firstRun = app.getPasswordManager().isFirstRun();
        setupBox.setVisible(firstRun);
        setupBox.setManaged(firstRun);
        confirmPasswordField.setVisible(firstRun);
        confirmPasswordField.setManaged(firstRun);
        confirmPasswordVisibleField.setVisible(firstRun);
        confirmPasswordVisibleField.setManaged(firstRun);
        bindPasswordVisibility(masterPasswordField, masterPasswordVisibleField);
        bindPasswordVisibility(confirmPasswordField, confirmPasswordVisibleField);
        WindowChrome.install(app.getPrimaryStage(), titleBar, minimizeButton, closeButton);
        languageButton.setOnAction(event -> app.toggleLanguage());
        app.setLanguageRefresh(this::applyLanguage);
        applyLanguage();
    }

    @FXML
    private void onLogin() {
        char[] password = masterPasswordField.getText().toCharArray();
        try {
            if (firstRun) {
                char[] confirm = confirmPasswordField.getText().toCharArray();
                if (!Arrays.equals(password, confirm)) {
                    showError(I18n.get("error.passwords.mismatch"));
                    return;
                }
                PasswordManager.wipe(confirm);
                SecretKey key = app.getPasswordManager().setupVault(password);
                app.getSessionManager().start(key, password);
            } else {
                SecretKey key = app.getPasswordManager().unlockVault(password);
                app.getSessionManager().start(key, password);
            }
            PasswordManager.wipe(password);
            masterPasswordField.clear();
            confirmPasswordField.clear();
            app.showMain();
        } catch (PasswordManager.VaultException e) {
            PasswordManager.wipe(password);
            showError(e.getMessage());
        }
    }

    @FXML
    private void onForgotPassword() {
        DialogHelper.showWarning(
                app.getPrimaryStage(),
                app.getCurrentTheme(),
                I18n.get("login.forgot.title"),
                I18n.get("login.forgot.header"),
                I18n.get("login.forgot.text"));
    }

    @FXML
    private void onCopyWallet() {
        DonationInfo.copyWalletAddress(app.getClipboardService());
    }

    @FXML
    private void onSupportInfo() {
        DonationInfo.showSupportDialog(
                app.getPrimaryStage(),
                app.getCurrentTheme(),
                app.getClipboardService(),
                null);
    }

    private void applyLanguage() {
        app.getPrimaryStage().setTitle(AppVersion.windowTitle(I18n.get("app.name")));
        appNameLabel.setText(AppVersion.windowTitle(I18n.get("app.name")));
        languageButton.setText(I18n.get("main.language"));
        titleLabel.setText(firstRun ? I18n.get("login.title.setup") : I18n.get("login.title.signin"));
        subtitleLabel.setText(firstRun ? I18n.get("login.subtitle.setup") : I18n.get("login.subtitle.signin"));
        masterPasswordLabel.setText(I18n.get("login.master.password"));
        confirmPasswordLabel.setText(I18n.get("login.confirm.password"));
        masterPasswordField.setPromptText(I18n.get("login.prompt.min"));
        masterPasswordVisibleField.setPromptText(I18n.get("login.prompt.min"));
        confirmPasswordField.setPromptText(I18n.get("login.prompt.repeat"));
        confirmPasswordVisibleField.setPromptText(I18n.get("login.prompt.repeat"));
        loginButton.setText(firstRun ? I18n.get("login.button.create") : I18n.get("login.button.signin"));
        forgotPasswordLink.setText(I18n.get("login.forgot"));
        supportLoginLabel.setText(I18n.get("support.label"));
        authorLoginLabel.setText(I18n.get("support.author"));
        walletLoginLabel.setText(DonationInfo.WALLET_ADDRESS);
        copyWalletLoginButton.setText(I18n.get("support.copy"));
        supportDetailsLink.setText(I18n.get("support.details"));
        versionLabel.setText(AppVersion.formatted());
    }

    private void bindPasswordVisibility(PasswordField hidden, TextField visible) {
        hidden.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!visible.isFocused()) {
                visible.setText(newVal);
            }
        });
        visible.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!hidden.isFocused()) {
                hidden.setText(newVal);
            }
        });
    }

    private void showError(String message) {
        DialogHelper.showError(app.getPrimaryStage(), app.getCurrentTheme(), message);
    }
}
