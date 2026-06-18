package com.safebox;

import com.safebox.controller.LoginController;
import com.safebox.repository.DatabaseConnection;
import com.safebox.repository.PasswordRepository;
import com.safebox.service.ClipboardService;
import com.safebox.service.CryptoService;
import com.safebox.service.GeneratorService;
import com.safebox.service.PasswordManager;
import com.safebox.service.VaultTransferService;
import com.safebox.util.AppPaths;
import com.safebox.util.AppVersion;
import com.safebox.util.I18n;
import com.safebox.util.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.util.Objects;

/**
 * SafeBox application entry point.
 */
public class MainApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(MainApp.class);

    private final CryptoService cryptoService = new CryptoService();
    private final DatabaseConnection databaseConnection = new DatabaseConnection();
    private final PasswordRepository passwordRepository = new PasswordRepository(databaseConnection, cryptoService);
    private final PasswordManager passwordManager = new PasswordManager(databaseConnection, passwordRepository, cryptoService);
    private final VaultTransferService vaultTransferService = new VaultTransferService(cryptoService);
    private final GeneratorService generatorService = new GeneratorService();
    private final ClipboardService clipboardService = new ClipboardService();
    private final SessionManager sessionManager = new SessionManager();

    private Stage primaryStage;
    private String currentTheme = "light";
    private Runnable languageRefresh;

    /**
     * Application main method.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        I18n.init();
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);
        loadThemePreference();
        showLogin();
        stage.setTitle(AppVersion.windowTitle(I18n.get("app.name")));
        stage.setMinWidth(420);
        stage.setMinHeight(320);
        try (var iconStream = Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png"))) {
            byte[] iconBytes = iconStream.readAllBytes();
            for (int size : new int[]{16, 32, 48, 256}) {
                stage.getIcons().add(new Image(new ByteArrayInputStream(iconBytes), size, size, true, true));
            }
        } catch (Exception e) {
            LOG.debug("Application icon not found, using default");
        }
        stage.show();
    }

    @Override
    public void stop() {
        sessionManager.lock();
        clipboardService.clearNow();
        databaseConnection.close();
    }

    /**
     * Shows the login / setup window.
     */
    public void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.init(this);
            setScene(root, 420, 360);
        } catch (IOException e) {
            LOG.error("Failed to load login view", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Shows the main vault window.
     */
    public void showMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            var controller = (com.safebox.controller.MainController) loader.getController();
            controller.init(this);
            setScene(root, 900, 600);
            primaryStage.setMaximized(false);
        } catch (IOException e) {
            LOG.error("Failed to load main view", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies a scene with the current theme stylesheet.
     *
     * @param root   scene root node
     * @param width  window width
     * @param height window height
     */
    public void setScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    /**
     * Switches between light and dark themes.
     *
     * @param theme theme name: "light" or "dark"
     */
    public void setTheme(String theme) {
        this.currentTheme = theme;
        saveThemePreference(theme);
        Scene scene = primaryStage.getScene();
        if (scene != null) {
            applyTheme(scene);
        }
    }

    /**
     * Returns the current theme name.
     *
     * @return "light" or "dark"
     */
    public String getCurrentTheme() {
        return currentTheme;
    }

    public PasswordManager getPasswordManager() {
        return passwordManager;
    }

    public VaultTransferService getVaultTransferService() {
        return vaultTransferService;
    }

    public GeneratorService getGeneratorService() {
        return generatorService;
    }

    public ClipboardService getClipboardService() {
        return clipboardService;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Registers UI refresh after language change.
     *
     * @param refresh refresh callback
     */
    public void setLanguageRefresh(Runnable refresh) {
        this.languageRefresh = refresh;
    }

    /**
     * Toggles UI language between English and Russian.
     */
    public void toggleLanguage() {
        I18n.toggleLocale();
        primaryStage.setTitle(AppVersion.windowTitle(I18n.get("app.name")));
        if (languageRefresh != null) {
            languageRefresh.run();
        }
    }

    private void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        String css = "dark".equals(currentTheme) ? "/css/dark.css" : "/css/light.css";
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(css)).toExternalForm());
    }

    private void loadThemePreference() {
        try {
            if (Files.exists(AppPaths.themeFile())) {
                String theme = Files.readString(AppPaths.themeFile()).trim();
                if ("dark".equals(theme) || "light".equals(theme)) {
                    currentTheme = theme;
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to load theme preference", e);
        }
    }

    private void saveThemePreference(String theme) {
        try {
            Files.createDirectories(AppPaths.dataDir());
            Files.writeString(AppPaths.themeFile(), theme);
        } catch (IOException e) {
            LOG.warn("Failed to save theme preference", e);
        }
    }
}
