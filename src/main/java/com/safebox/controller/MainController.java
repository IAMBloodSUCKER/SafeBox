package com.safebox.controller;

import com.safebox.MainApp;
import com.safebox.model.PasswordEntry;
import com.safebox.service.PasswordManager;
import com.safebox.util.AppVersion;
import com.safebox.util.ChangePasswordDialogController;
import com.safebox.util.DialogHelper;
import com.safebox.util.DonationInfo;
import com.safebox.util.EntryFormatHelper;
import com.safebox.util.I18n;
import com.safebox.util.WindowChrome;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main vault window controller.
 */
public class MainController {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("mm:ss", Locale.ROOT);

    @FXML
    private Label appNameLabel;
    @FXML
    private TextField searchField;
    @FXML
    private TableView<PasswordEntry> entriesTable;
    @FXML
    private TableColumn<PasswordEntry, String> siteColumn;
    @FXML
    private TableColumn<PasswordEntry, String> loginColumn;
    @FXML
    private TableColumn<PasswordEntry, String> updatedColumn;
    @FXML
    private Label emptyLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private HBox titleBar;
    @FXML
    private Button minimizeButton;
    @FXML
    private Button closeButton;
    @FXML
    private Button languageButton;
    @FXML
    private Button themeButton;
    @FXML
    private Button lockButton;
    @FXML
    private Button changePasswordButton;
    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button bulkDeleteButton;
    @FXML
    private Button generateButton;
    @FXML
    private Button exportButton;
    @FXML
    private Button importButton;
    @FXML
    private Label supportLabel;
    @FXML
    private Label authorLabel;
    @FXML
    private Label walletAddressLabel;
    @FXML
    private Button copyWalletButton;
    @FXML
    private Button supportInfoButton;
    @FXML
    private VBox detailPanel;
    @FXML
    private Label detailHintLabel;
    @FXML
    private Label detailSiteLabel;
    @FXML
    private Label detailSiteValue;
    @FXML
    private Label detailLoginLabel;
    @FXML
    private Label detailLoginValue;
    @FXML
    private Label detailPasswordLabel;
    @FXML
    private Label detailPasswordValue;
    @FXML
    private Button toggleDetailPasswordButton;
    @FXML
    private Label detailNotesLabel;
    @FXML
    private Label detailNotesPreview;
    @FXML
    private ScrollPane detailNotesScroll;
    @FXML
    private Label detailNotesFull;
    @FXML
    private Button toggleNotesButton;
    @FXML
    private Button copyNotesButton;
    @FXML
    private Label toastLabel;

    private static final int NOTES_PREVIEW_MAX_CHARS = 140;
    private static final int NOTES_PREVIEW_MAX_LINES = 2;

    private MainApp app;
    private final ObservableList<PasswordEntry> entries = FXCollections.observableArrayList();
    private PasswordEntry currentEntry;
    private boolean passwordRevealed;
    private boolean notesExpanded;
    private String currentNotes;
    private Timer statusTimer;
    private PauseTransition toastHide;

    /**
     * Initializes the main window.
     *
     * @param app main application
     */
    public void init(MainApp app) {
        this.app = app;
        setupTable();
        WindowChrome.install(app.getPrimaryStage(), titleBar, minimizeButton, closeButton);
        languageButton.setOnAction(event -> app.toggleLanguage());
        app.setLanguageRefresh(this::applyLanguage);
        applyLanguage();
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshEntries());
        app.getSessionManager().setOnLock(ignored -> Platform.runLater(app::showLogin));
        refreshEntries();
        startStatusTimer();
        app.getPrimaryStage().setOnCloseRequest(event -> {
            stopStatusTimer();
            app.getSessionManager().lock();
        });
    }

    @FXML
    private void onAdd() {
        app.getSessionManager().touch();
        openEditDialog(null);
    }

    @FXML
    private void onEdit() {
        app.getSessionManager().touch();
        PasswordEntry selected = entriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo(I18n.get("main.select.edit"));
            return;
        }
        openEditDialog(selected);
    }

    @FXML
    private void onDelete() {
        app.getSessionManager().touch();
        List<PasswordEntry> selected = new ArrayList<>(entriesTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            showInfo(I18n.get("main.select.delete"));
            return;
        }
        if (selected.size() == 1) {
            PasswordEntry entry = selected.get(0);
            boolean confirmed = DialogHelper.showConfirm(
                    app.getPrimaryStage(),
                    app.getCurrentTheme(),
                    I18n.get("main.delete.title"),
                    I18n.get("main.delete.header"),
                    EntryFormatHelper.deleteSingleMessage(entry),
                    I18n.get("btn.delete"),
                    "warning");
            if (confirmed) {
                app.getPasswordManager().deleteEntry(entry.getId());
                refreshEntries();
            }
            return;
        }
        confirmAndDeleteSelected(selected);
    }

    @FXML
    private void onBulkDelete() {
        app.getSessionManager().touch();
        int selectedCount = entriesTable.getSelectionModel().getSelectedItems().size();
        int totalCount;
        try {
            totalCount = app.getPasswordManager().countEntries();
        } catch (Exception e) {
            showError(e.getMessage());
            return;
        }
        if (totalCount == 0) {
            showInfo(I18n.get("main.empty"));
            return;
        }

        if (selectedCount > 0) {
            Optional<Integer> choice = DialogHelper.showChoice(
                    app.getPrimaryStage(),
                    app.getCurrentTheme(),
                    I18n.get("main.bulk.delete.title"),
                    I18n.get("main.bulk.delete.header"),
                    I18n.get("main.bulk.delete.message"),
                    I18n.get("main.bulk.delete.selected", selectedCount),
                    I18n.get("main.bulk.delete.all", totalCount),
                    "warning");
            choice.ifPresent(index -> {
                if (index == 0) {
                    confirmAndDeleteSelected(
                            new ArrayList<>(entriesTable.getSelectionModel().getSelectedItems()));
                } else if (index == 1) {
                    confirmAndDeleteAll(totalCount);
                }
            });
        } else {
            confirmAndDeleteAll(totalCount);
        }
    }

    private void confirmAndDeleteSelected(List<PasswordEntry> selected) {
        if (selected.isEmpty()) {
            return;
        }
        boolean confirmed = DialogHelper.showConfirm(
                app.getPrimaryStage(),
                app.getCurrentTheme(),
                I18n.get("main.bulk.delete.title"),
                I18n.get("main.bulk.delete.selected.header", selected.size()),
                EntryFormatHelper.deleteSelectedMessage(selected),
                I18n.get("btn.delete"),
                "warning");
        if (!confirmed) {
            return;
        }
        List<Long> ids = selected.stream()
                .map(PasswordEntry::getId)
                .toList();
        int deleted = app.getPasswordManager().deleteEntries(ids);
        refreshEntries();
        showInfo(I18n.get("main.bulk.delete.done", deleted));
    }

    private void confirmAndDeleteAll(int count) {
        boolean confirmed = DialogHelper.showConfirm(
                app.getPrimaryStage(),
                app.getCurrentTheme(),
                I18n.get("main.bulk.delete.title"),
                I18n.get("main.bulk.delete.all.header", count),
                I18n.get("main.bulk.delete.all.message", count),
                I18n.get("btn.delete"),
                "warning");
        if (!confirmed) {
            return;
        }
        int deleted = app.getPasswordManager().deleteAllEntries();
        refreshEntries();
        showInfo(I18n.get("main.bulk.delete.done", deleted));
    }

    @FXML
    private void onGenerate() {
        app.getSessionManager().touch();
        openGeneratorDialog(password -> {
            PasswordEntry entry = new PasswordEntry("", "", password, "");
            openEditDialog(entry);
        });
    }

    @FXML
    private void onLock() {
        app.getSessionManager().lock();
    }

    @FXML
    private void onChangePassword() {
        app.getSessionManager().touch();
        Optional<ChangePasswordDialogController> dialog = DialogHelper.showChangePassword(
                app.getPrimaryStage(),
                app.getCurrentTheme());
        if (dialog.isEmpty()) {
            return;
        }
        ChangePasswordDialogController controller = dialog.get();
        char[] current = controller.getCurrentPassword();
        char[] newPassword = controller.getNewPassword();
        try {
            SecretKey oldKey = app.getSessionManager().getEncryptionKey();
            SecretKey newKey = app.getPasswordManager().changeMasterPassword(current, newPassword, oldKey);
            app.getSessionManager().rotateKey(newKey, newPassword);
            showToast(I18n.get("changePassword.done"));
        } catch (Exception e) {
            showError(e.getMessage());
        } finally {
            PasswordManager.wipe(current);
            PasswordManager.wipe(newPassword);
        }
    }

    @FXML
    private void onToggleTheme() {
        app.getSessionManager().touch();
        String next = "dark".equals(app.getCurrentTheme()) ? "light" : "dark";
        app.setTheme(next);
    }

    @FXML
    private void onCopyWallet() {
        DonationInfo.copyWalletAddress(app.getClipboardService());
        showToast(I18n.get("support.copy.done"));
    }

    @FXML
    private void onSupportInfo() {
        DonationInfo.showSupportDialog(
                app.getPrimaryStage(),
                app.getCurrentTheme(),
                app.getClipboardService(),
                () -> showToast(I18n.get("support.copy.done")));
    }

    @FXML
    private void onExport() {
        app.getSessionManager().touch();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("main.export.dialog"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("main.export.filter"), "*.safebox"));
        chooser.setInitialFileName("safebox-export.safebox");
        File file = chooser.showSaveDialog(app.getPrimaryStage());
        if (file == null) {
            return;
        }
        Path target = ensureSafeboxExtension(file.toPath());

        Optional<char[]> passwordOpt = DialogHelper.showPasswordPrompt(
                app.getPrimaryStage(),
                app.getCurrentTheme(),
                I18n.get("transfer.export.title"),
                I18n.get("transfer.export.header"),
                I18n.get("transfer.export.message"),
                true);
        if (passwordOpt.isEmpty()) {
            return;
        }
        char[] password = passwordOpt.get();
        try {
            SecretKey key = app.getSessionManager().getEncryptionKey();
            List<PasswordEntry> all = app.getPasswordManager().listEntries(key, "");
            app.getVaultTransferService().export(target, all, password);
            showInfo(I18n.get("main.export.done", all.size()));
        } catch (Exception e) {
            showError(e.getMessage());
        } finally {
            PasswordManager.wipe(password);
        }
    }

    @FXML
    private void onImport() {
        app.getSessionManager().touch();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("main.import.dialog"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("main.export.filter"), "*.safebox"));
        File file = chooser.showOpenDialog(app.getPrimaryStage());
        if (file == null) {
            return;
        }

        Optional<char[]> passwordOpt = DialogHelper.showPasswordPrompt(
                app.getPrimaryStage(),
                app.getCurrentTheme(),
                I18n.get("transfer.import.title"),
                I18n.get("transfer.import.header"),
                I18n.get("transfer.import.message"),
                false);
        if (passwordOpt.isEmpty()) {
            return;
        }
        char[] password = passwordOpt.get();
        try {
            SecretKey key = app.getSessionManager().getEncryptionKey();
            List<PasswordEntry> imported = app.getVaultTransferService().importFrom(file.toPath(), password);
            int count = app.getPasswordManager().importEntries(imported, key);
            refreshEntries();
            showInfo(I18n.get("main.import.done", count));
        } catch (Exception e) {
            showError(e.getMessage());
        } finally {
            PasswordManager.wipe(password);
        }
    }

    private Path ensureSafeboxExtension(Path path) {
        String fileName = path.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".safebox")) {
            return path.resolveSibling(fileName + ".safebox");
        }
        return path;
    }

    private void applyLanguage() {
        app.getPrimaryStage().setTitle(AppVersion.windowTitle(I18n.get("app.name")));
        appNameLabel.setText(AppVersion.windowTitle(I18n.get("app.name")));
        languageButton.setText(I18n.get("main.language"));
        themeButton.setText(I18n.get("main.theme"));
        lockButton.setText(I18n.get("main.lock"));
        changePasswordButton.setText(I18n.get("main.changePassword"));
        searchField.setPromptText(I18n.get("main.search"));
        siteColumn.setText(I18n.get("main.col.site"));
        loginColumn.setText(I18n.get("main.col.login"));
        updatedColumn.setText(I18n.get("main.col.updated"));
        emptyLabel.setText(I18n.get("main.empty"));
        addButton.setText(I18n.get("main.add"));
        editButton.setText(I18n.get("main.edit"));
        deleteButton.setText(I18n.get("main.delete"));
        bulkDeleteButton.setText(I18n.get("main.bulk.delete"));
        generateButton.setText(I18n.get("main.generate"));
        exportButton.setText(I18n.get("main.export"));
        importButton.setText(I18n.get("main.import"));
        supportLabel.setText(I18n.get("support.label"));
        authorLabel.setText(I18n.get("support.author"));
        walletAddressLabel.setText(DonationInfo.WALLET_ADDRESS);
        copyWalletButton.setText(I18n.get("support.copy"));
        supportInfoButton.setText(I18n.get("support.details"));
        detailHintLabel.setText(I18n.get("main.detail.hint"));
        detailSiteLabel.setText(I18n.get("main.detail.site"));
        detailLoginLabel.setText(I18n.get("main.detail.login"));
        detailPasswordLabel.setText(I18n.get("main.detail.password"));
        detailNotesLabel.setText(I18n.get("main.detail.notes"));
        copyNotesButton.setText(I18n.get("main.detail.notes.copy"));
        updateTogglePasswordText();
        updateDetailFromSelection();
        updateStatusText();
    }

    private void setupTable() {
        siteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSite()));
        loginColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLogin()));
        updatedColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getUpdatedAt()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DATE_FORMAT)));

        entriesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        siteColumn.setMinWidth(100);
        siteColumn.setMaxWidth(500);
        siteColumn.setPrefWidth(300);
        loginColumn.setMinWidth(100);
        loginColumn.setMaxWidth(500);
        loginColumn.setPrefWidth(300);
        updatedColumn.setMinWidth(90);
        updatedColumn.setMaxWidth(220);
        updatedColumn.setPrefWidth(140);

        entriesTable.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double width = newWidth.doubleValue();
            if (width <= 0) {
                return;
            }
            siteColumn.setMaxWidth(Math.min(500, width * 0.55));
            loginColumn.setMaxWidth(Math.min(500, width * 0.55));
            updatedColumn.setMaxWidth(Math.min(220, width * 0.35));
        });

        entriesTable.setItems(entries);
        entriesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        entriesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldEntry, newEntry) -> updateDetailFromSelection());
    }

    @FXML
    private void onCopySite() {
        copyField(currentEntry == null ? null : currentEntry.getSite(), I18n.get("main.copy.site.done"));
    }

    @FXML
    private void onCopyLogin() {
        copyField(currentEntry == null ? null : currentEntry.getLogin(), I18n.get("main.copy.login.done"));
    }

    @FXML
    private void onCopyPassword() {
        copyField(currentEntry == null ? null : currentEntry.getPassword(), I18n.get("main.copy.password.done"));
    }

    private void copyField(String value, String toastMessage) {
        if (value == null || value.isBlank()) {
            return;
        }
        app.getSessionManager().touch();
        app.getClipboardService().copy(value);
        showToast(toastMessage);
    }

    @FXML
    private void onToggleDetailPassword() {
        if (currentEntry == null || currentEntry.getPassword() == null || currentEntry.getPassword().isBlank()) {
            return;
        }
        app.getSessionManager().touch();
        passwordRevealed = !passwordRevealed;
        updatePasswordDisplay();
        updateTogglePasswordText();
    }

    @FXML
    private void onCopyNotes() {
        copyField(currentNotes, I18n.get("main.copy.notes.done"));
    }

    @FXML
    private void onToggleNotes() {
        if (currentNotes == null || currentNotes.isBlank()) {
            return;
        }
        app.getSessionManager().touch();
        notesExpanded = !notesExpanded;
        updateNotesExpandedState();
    }

    private void updateDetailFromSelection() {
        PasswordEntry entry = entriesTable.getSelectionModel().getSelectedItem();
        currentEntry = entry;
        passwordRevealed = false;
        notesExpanded = false;
        if (entry == null) {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
            return;
        }
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
        detailSiteValue.setText(entry.getSite());
        detailLoginValue.setText(entry.getLogin());
        updatePasswordDisplay();
        updateTogglePasswordText();
        updateNotesDisplay(entry.getNotes());
    }

    private void updateNotesDisplay(String notes) {
        currentNotes = notes;
        notesExpanded = false;

        if (notes == null || notes.isBlank()) {
            detailNotesPreview.setText("—");
            detailNotesPreview.setVisible(true);
            detailNotesPreview.setManaged(true);
            detailNotesScroll.setVisible(false);
            detailNotesScroll.setManaged(false);
            toggleNotesButton.setVisible(false);
            toggleNotesButton.setManaged(false);
            copyNotesButton.setVisible(false);
            copyNotesButton.setManaged(false);
            return;
        }

        detailNotesFull.setText(notes);
        copyNotesButton.setVisible(true);
        copyNotesButton.setManaged(true);

        if (isNotesLong(notes)) {
            detailNotesPreview.setText(truncateNotes(notes));
            toggleNotesButton.setText(I18n.get("main.detail.notes.more"));
            toggleNotesButton.setVisible(true);
            toggleNotesButton.setManaged(true);
            detailNotesScroll.setVisible(false);
            detailNotesScroll.setManaged(false);
            detailNotesPreview.setVisible(true);
            detailNotesPreview.setManaged(true);
        } else {
            detailNotesPreview.setText(notes);
            detailNotesPreview.setVisible(true);
            detailNotesPreview.setManaged(true);
            detailNotesScroll.setVisible(false);
            detailNotesScroll.setManaged(false);
            toggleNotesButton.setVisible(false);
            toggleNotesButton.setManaged(false);
        }
    }

    private void updateNotesExpandedState() {
        if (notesExpanded) {
            detailNotesPreview.setVisible(false);
            detailNotesPreview.setManaged(false);
            detailNotesScroll.setVisible(true);
            detailNotesScroll.setManaged(true);
            toggleNotesButton.setText(I18n.get("main.detail.notes.less"));
        } else {
            detailNotesScroll.setVisible(false);
            detailNotesScroll.setManaged(false);
            detailNotesPreview.setVisible(true);
            detailNotesPreview.setManaged(true);
            detailNotesPreview.setText(truncateNotes(currentNotes));
            toggleNotesButton.setText(I18n.get("main.detail.notes.more"));
        }
    }

    private boolean isNotesLong(String notes) {
        return notes.length() > NOTES_PREVIEW_MAX_CHARS || notes.lines().count() > NOTES_PREVIEW_MAX_LINES;
    }

    private String truncateNotes(String notes) {
        String[] lines = notes.split("\n", -1);
        StringBuilder preview = new StringBuilder();
        int lineCount = 0;
        for (String line : lines) {
            if (lineCount >= NOTES_PREVIEW_MAX_LINES) {
                break;
            }
            if (lineCount > 0) {
                preview.append('\n');
            }
            preview.append(line);
            lineCount++;
        }
        String result = preview.toString();
        if (result.length() > NOTES_PREVIEW_MAX_CHARS) {
            result = result.substring(0, NOTES_PREVIEW_MAX_CHARS - 3).stripTrailing() + "...";
        } else if (notes.length() > result.length() || lines.length > NOTES_PREVIEW_MAX_LINES) {
            result = result + "...";
        }
        return result;
    }

    private void updatePasswordDisplay() {
        if (currentEntry == null) {
            detailPasswordValue.setText("—");
            toggleDetailPasswordButton.setDisable(true);
            return;
        }
        String password = currentEntry.getPassword();
        if (password == null || password.isBlank()) {
            detailPasswordValue.setText("—");
            toggleDetailPasswordButton.setDisable(true);
            return;
        }
        toggleDetailPasswordButton.setDisable(false);
        detailPasswordValue.setText(passwordRevealed ? password : maskPassword(password));
    }

    private void updateTogglePasswordText() {
        toggleDetailPasswordButton.setText(passwordRevealed ? I18n.get("edit.hide") : I18n.get("edit.show"));
    }

    private static String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "—";
        }
        return "•".repeat(Math.min(password.length(), 16));
    }

    private void showToast(String message) {
        toastLabel.setText(message);
        toastLabel.setVisible(true);
        toastLabel.setManaged(true);
        if (toastHide != null) {
            toastHide.stop();
        }
        toastHide = new PauseTransition(Duration.seconds(2.5));
        toastHide.setOnFinished(event -> {
            toastLabel.setVisible(false);
            toastLabel.setManaged(false);
        });
        toastHide.play();
    }

    private void refreshEntries() {
        try {
            var key = app.getSessionManager().getEncryptionKey();
            entries.setAll(app.getPasswordManager().listEntries(key, searchField.getText()));
            updateDetailFromSelection();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void openEditDialog(PasswordEntry entry) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit.fxml"));
            Parent root = loader.load();
            EditController controller = loader.getController();
            controller.init(app, entry, saved -> refreshEntries());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(app.getPrimaryStage());
            dialog.setTitle(entry == null || entry.getId() == null
                    ? I18n.get("main.dialog.new") : I18n.get("main.dialog.edit"));
            Scene scene = new Scene(root);
            applyDialogTheme(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (IOException e) {
            showError(I18n.get("main.error.edit"));
        }
    }

    private void openGeneratorDialog(java.util.function.Consumer<String> onGenerated) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/generator.fxml"));
            Parent root = loader.load();
            GeneratorController controller = loader.getController();
            controller.init(app, onGenerated);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(app.getPrimaryStage());
            dialog.setTitle(I18n.get("main.dialog.generator"));
            Scene scene = new Scene(root);
            applyDialogTheme(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (IOException e) {
            showError(I18n.get("main.error.generator"));
        }
    }

    private void applyDialogTheme(Scene scene) {
        String css = "dark".equals(app.getCurrentTheme()) ? "/css/dark.css" : "/css/light.css";
        scene.getStylesheets().add(getClass().getResource(css).toExternalForm());
    }

    private void startStatusTimer() {
        stopStatusTimer();
        statusTimer = new Timer(true);
        statusTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(MainController.this::updateStatusText);
            }
        }, 0, 1000);
    }

    private void updateStatusText() {
        if (statusLabel == null || app == null) {
            return;
        }
        var remaining = app.getSessionManager().remainingTime();
        long seconds = remaining.getSeconds();
        statusLabel.setText(I18n.get("main.status.lock", TIME_FORMAT.format(
                java.time.LocalTime.ofSecondOfDay(seconds))));
        statusLabel.setTooltip(new Tooltip(I18n.get("main.lock")));
    }

    private void stopStatusTimer() {
        if (statusTimer != null) {
            statusTimer.cancel();
            statusTimer = null;
        }
    }

    private void showInfo(String message) {
        DialogHelper.showInfo(
                app.getPrimaryStage(),
                app.getCurrentTheme(),
                I18n.get("app.name"),
                null,
                message);
    }

    private void showError(String message) {
        DialogHelper.showError(app.getPrimaryStage(), app.getCurrentTheme(), message);
    }
}
