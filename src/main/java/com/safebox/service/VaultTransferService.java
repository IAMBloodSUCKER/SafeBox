package com.safebox.service;

import com.safebox.model.PasswordEntry;
import com.safebox.util.I18n;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;

/**
 * Encrypted export/import of password entries between devices.
 */
public class VaultTransferService {

    private static final byte[] MAGIC = "SBX1".getBytes(StandardCharsets.US_ASCII);
    private static final String FILE_EXTENSION = ".safebox";

    private final CryptoService cryptoService;

    public VaultTransferService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    /**
     * Returns the recommended file extension for export files.
     *
     * @return extension including dot
     */
    public String fileExtension() {
        return FILE_EXTENSION;
    }

    /**
     * Writes entries to an encrypted portable file.
     *
     * @param target         destination path
     * @param entries        decrypted entries to export
     * @param exportPassword password that will be required on import
     */
    public void export(Path target, List<PasswordEntry> entries, char[] exportPassword) {
        validateExportPassword(exportPassword);
        try {
            byte[] payload = VaultTransferCodec.serialize(entries);
            byte[] salt = cryptoService.generateSalt();
            SecretKey key = cryptoService.deriveKey(exportPassword, salt);
            String encrypted = cryptoService.encrypt(Base64.getEncoder().encodeToString(payload), key);
            byte[] fileBytes = buildFileBytes(salt, encrypted);
            Files.createDirectories(target.getParent() == null ? Path.of(".") : target.getParent());
            Files.write(target, fileBytes);
        } catch (IOException e) {
            throw new TransferException(I18n.get("error.transfer.export"), e);
        }
    }

    /**
     * Reads entries from an encrypted portable file.
     *
     * @param source         export file path
     * @param exportPassword password set during export
     * @return decrypted entries without database ids
     */
    public List<PasswordEntry> importFrom(Path source, char[] exportPassword) {
        validateExportPassword(exportPassword);
        try {
            byte[] fileBytes = Files.readAllBytes(source);
            byte[] salt = extractSalt(fileBytes);
            String encrypted = extractEncryptedPayload(fileBytes);
            SecretKey key = cryptoService.deriveKey(exportPassword, salt);
            String payloadBase64 = cryptoService.decrypt(encrypted, key);
            byte[] payload = Base64.getDecoder().decode(payloadBase64);
            return VaultTransferCodec.deserialize(payload);
        } catch (CryptoService.CryptoException e) {
            throw new TransferException(I18n.get("error.transfer.invalidPassword"), e);
        } catch (IOException e) {
            throw new TransferException(I18n.get("error.transfer.import"), e);
        }
    }

    private static byte[] buildFileBytes(byte[] salt, String encryptedPayload) throws IOException {
        byte[] encryptedBytes = encryptedPayload.getBytes(StandardCharsets.UTF_8);
        byte[] file = new byte[MAGIC.length + salt.length + encryptedBytes.length];
        System.arraycopy(MAGIC, 0, file, 0, MAGIC.length);
        System.arraycopy(salt, 0, file, MAGIC.length, salt.length);
        System.arraycopy(encryptedBytes, 0, file, MAGIC.length + salt.length, encryptedBytes.length);
        return file;
    }

    private static byte[] extractSalt(byte[] fileBytes) {
        if (fileBytes.length < MAGIC.length + 16) {
            throw new TransferException(I18n.get("error.transfer.invalidFile"));
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (fileBytes[i] != MAGIC[i]) {
                throw new TransferException(I18n.get("error.transfer.invalidFile"));
            }
        }
        return Arrays.copyOfRange(fileBytes, MAGIC.length, MAGIC.length + 16);
    }

    private static String extractEncryptedPayload(byte[] fileBytes) {
        byte[] encryptedBytes = Arrays.copyOfRange(fileBytes, MAGIC.length + 16, fileBytes.length);
        if (encryptedBytes.length == 0) {
            throw new TransferException(I18n.get("error.transfer.invalidFile"));
        }
        return new String(encryptedBytes, StandardCharsets.UTF_8);
    }

    private static void validateExportPassword(char[] exportPassword) {
        if (exportPassword == null || exportPassword.length < 8) {
            throw new TransferException(I18n.get("error.transfer.passwordLength"));
        }
    }

    /**
     * Export/import failure.
     */
    public static class TransferException extends RuntimeException {
        public TransferException(String message) {
            super(message);
        }

        public TransferException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
