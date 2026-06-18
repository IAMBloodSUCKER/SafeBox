package com.safebox.service;

import com.safebox.model.PasswordEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VaultTransferServiceTest {

    private final VaultTransferService transferService = new VaultTransferService(new CryptoService());

    @Test
    void exportAndImportRoundTrip(@TempDir Path tempDir) {
        PasswordEntry entry = new PasswordEntry("github.com", "user", "p@ss\"word\n", "note");
        Path file = tempDir.resolve("backup.safebox");
        char[] exportPassword = "export-pass".toCharArray();

        transferService.export(file, List.of(entry), exportPassword);
        List<PasswordEntry> imported = transferService.importFrom(file, exportPassword);

        assertEquals(1, imported.size());
        assertEquals(entry.getSite(), imported.get(0).getSite());
        assertEquals(entry.getLogin(), imported.get(0).getLogin());
        assertEquals(entry.getPassword(), imported.get(0).getPassword());
        assertEquals(entry.getNotes(), imported.get(0).getNotes());
        PasswordManager.wipe(exportPassword);
    }

    @Test
    void importFailsWithWrongPassword(@TempDir Path tempDir) {
        Path file = tempDir.resolve("backup.safebox");
        char[] exportPassword = "export-pass".toCharArray();
        transferService.export(file, List.of(new PasswordEntry("site", "login", "password", "")), exportPassword);

        assertThrows(VaultTransferService.TransferException.class,
                () -> transferService.importFrom(file, "wrong-pass".toCharArray()));
        PasswordManager.wipe(exportPassword);
    }
}
