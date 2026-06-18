package com.safebox.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Styled application dialogs matching SafeBox themes.
 */
public final class DialogHelper {

    private static final double DIALOG_RADIUS = 28;

    private DialogHelper() {
    }

    /**
     * Shows an information dialog.
     */
    public static void showInfo(Stage owner, String theme, String title, String header, String message) {
        showStyledInfo(owner, theme, "i", null, title, header, message);
    }

    /**
     * Shows an error dialog.
     */
    public static void showError(Stage owner, String theme, String message) {
        showStyledInfo(owner, theme, "!", "error", I18n.get("error.title"), null, message);
    }

    /**
     * Shows a warning dialog.
     */
    public static void showWarning(Stage owner, String theme, String title, String header, String message) {
        showStyledInfo(owner, theme, "\u26A0", "warning", title, header, message);
    }

    /**
     * Shows a confirmation dialog.
     *
     * @return true if confirmed
     */
    public static boolean showConfirm(Stage owner, String theme,
                                      String title, String header, String message, String confirmText) {
        return showConfirm(owner, theme, title, header, message, confirmText, null);
    }

    /**
     * Shows a confirmation dialog with an optional visual variant.
     *
     * @return true if confirmed
     */
    public static boolean showConfirm(Stage owner, String theme,
                                      String title, String header, String message,
                                      String confirmText, String variant) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/dialogs/confirm-dialog.fxml"));
            Parent root = loader.load();
            ConfirmDialogController controller = loader.getController();
            controller.init("?", title, header, message, confirmText);
            controller.setVariant(variant);

            Stage dialog = createDialog(owner, title);
            setupDialogScene(dialog, root, theme);
            dialog.showAndWait();
            return Boolean.TRUE.equals(controller.getConfirmed());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open confirm dialog", e);
        }
    }

    public static Optional<Integer> showChoice(Stage owner, String theme,
                                               String title, String header, String message,
                                               String firstOption, String secondOption) {
        return showChoice(owner, theme, title, header, message, firstOption, secondOption, null);
    }

    /**
     * Shows a three-option choice dialog with an optional visual variant.
     *
     * @return 0, 1, or empty if cancelled
     */
    public static Optional<Integer> showChoice(Stage owner, String theme,
                                               String title, String header, String message,
                                               String firstOption, String secondOption,
                                               String variant) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/dialogs/choice-dialog.fxml"));
            Parent root = loader.load();
            ChoiceDialogController controller = loader.getController();
            controller.init("?", title, header, message, firstOption, secondOption);
            controller.setVariant(variant);

            Stage dialog = createDialog(owner, title);
            setupDialogScene(dialog, root, theme);
            dialog.showAndWait();
            Integer choice = controller.getChoice();
            return choice == null ? Optional.empty() : Optional.of(choice);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open choice dialog", e);
        }
    }

    /**
     * Shows a password prompt for export/import.
     *
     * @return entered password or empty if cancelled
     */
    public static Optional<char[]> showPasswordPrompt(Stage owner, String theme,
                                                      String title, String header, String message,
                                                      boolean requireConfirmation) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/dialogs/password-prompt.fxml"));
            Parent root = loader.load();
            PasswordPromptController controller = loader.getController();
            controller.init(title, header, message, requireConfirmation);

            Stage dialog = createDialog(owner, title);
            setupDialogScene(dialog, root, theme);
            dialog.showAndWait();
            char[] password = controller.getPassword();
            return password == null ? Optional.empty() : Optional.of(password);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open password prompt", e);
        }
    }

    /**
     * Shows the change master password dialog.
     *
     * @return controller with entered passwords, or empty if cancelled
     */
    public static Optional<ChangePasswordDialogController> showChangePassword(Stage owner, String theme) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogHelper.class.getResource("/fxml/dialogs/change-password-dialog.fxml"));
            Parent root = loader.load();
            ChangePasswordDialogController controller = loader.getController();
            controller.init();

            Stage dialog = createDialog(owner, I18n.get("changePassword.title"));
            setupDialogScene(dialog, root, theme);
            dialog.showAndWait();
            return controller.isSubmitted() ? Optional.of(controller) : Optional.empty();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open change password dialog", e);
        }
    }

    /**
     * Shows the support / donation details dialog.
     */
    public static void showSupport(Stage owner, String theme,
                                   com.safebox.service.ClipboardService clipboardService,
                                   Runnable onCopied) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/dialogs/support-dialog.fxml"));
            Parent root = loader.load();
            SupportDialogController controller = loader.getController();
            controller.init(clipboardService, onCopied);

            Stage dialog = createDialog(owner, I18n.get("support.title"));
            setupDialogScene(dialog, root, theme);
            dialog.showAndWait();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open support dialog", e);
        }
    }

    private static void showStyledInfo(Stage owner, String theme, String icon, String variant,
                                       String title, String header, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/dialogs/info-dialog.fxml"));
            Parent root = loader.load();
            InfoDialogController controller = loader.getController();
            controller.init(icon, title, header, message);
            controller.setVariant(variant);

            Stage dialog = createDialog(owner, title);
            setupDialogScene(dialog, root, theme);
            dialog.showAndWait();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open info dialog", e);
        }
    }

    private static void setupDialogScene(Stage dialog, Parent root, String theme) {
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        applyTheme(scene, theme);
        dialog.setScene(scene);
        WindowClip.applyRounded(root, DIALOG_RADIUS);
    }

    private static Stage createDialog(Stage owner, String title) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(title);
        dialog.setResizable(false);
        return dialog;
    }

    private static void applyTheme(Scene scene, String theme) {
        String css = "dark".equals(theme) ? "/css/dark.css" : "/css/light.css";
        scene.getStylesheets().add(Objects.requireNonNull(DialogHelper.class.getResource(css)).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(DialogHelper.class.getResource("/css/dialogs.css")).toExternalForm());
    }
}
