package com.safebox.util;

import com.safebox.service.ClipboardService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the support / donation details dialog.
 */
public class SupportDialogController {

    @FXML
    private Label iconLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label headerLabel;
    @FXML
    private Label introLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private TextField addressField;
    @FXML
    private TextArea detailsArea;
    @FXML
    private Button copyAddressButton;
    @FXML
    private Button copyAllButton;
    @FXML
    private Button okButton;

    private ClipboardService clipboardService;
    private Runnable onCopied;

    /**
     * Configures the support dialog.
     */
    public void init(ClipboardService clipboardService, Runnable onCopied) {
        this.clipboardService = clipboardService;
        this.onCopied = onCopied;

        iconLabel.setText("i");
        titleLabel.setText(I18n.get("support.title"));
        headerLabel.setText(I18n.get("support.header"));
        introLabel.setText(I18n.get("support.intro"));
        addressLabel.setText(I18n.get("support.address.label"));
        addressField.setText(DonationInfo.WALLET_ADDRESS);
        detailsArea.setText(buildDetailsText());
        copyAddressButton.setText(I18n.get("support.copy"));
        copyAllButton.setText(I18n.get("support.copy.all"));
        okButton.setText(I18n.get("btn.ok"));
    }

    @FXML
    private void onCopyAddress() {
        copyToClipboard(DonationInfo.WALLET_ADDRESS);
    }

    @FXML
    private void onCopyAll() {
        copyToClipboard(buildFullText());
    }

    @FXML
    private void onOk() {
        close();
    }

    private void copyToClipboard(String text) {
        if (clipboardService == null) {
            return;
        }
        clipboardService.copy(text);
        if (onCopied != null) {
            onCopied.run();
        }
    }

    private static String buildDetailsText() {
        return I18n.get("support.networks.recommended")
                + "\n\n"
                + I18n.get("support.tokens")
                + "\n\n"
                + I18n.get("support.networks.all");
    }

    private static String buildFullText() {
        return I18n.get("support.intro")
                + "\n\n"
                + I18n.get("support.address.label")
                + ": "
                + DonationInfo.WALLET_ADDRESS
                + "\n\n"
                + buildDetailsText();
    }

    private void close() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }
}
