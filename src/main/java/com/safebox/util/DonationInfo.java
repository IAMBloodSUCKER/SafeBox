package com.safebox.util;

import com.safebox.service.ClipboardService;
import javafx.stage.Stage;

/**
 * Donation wallet details and support dialog.
 */
public final class DonationInfo {

    public static final String WALLET_ADDRESS = "0xe4a1bf07aa8c2194ab94d72812364968ac5b58e3";

    private DonationInfo() {
    }

    /**
     * Copies the donation wallet address to the clipboard.
     */
    public static void copyWalletAddress(ClipboardService clipboardService) {
        clipboardService.copy(WALLET_ADDRESS);
    }

    /**
     * Opens the full support information dialog.
     */
    public static void showSupportDialog(Stage owner, String theme,
                                         ClipboardService clipboardService,
                                         Runnable onCopied) {
        DialogHelper.showSupport(owner, theme, clipboardService, onCopied);
    }
}
