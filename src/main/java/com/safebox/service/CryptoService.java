package com.safebox.service;

import com.safebox.util.I18n;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cryptographic operations: PBKDF2 key derivation and AES-256-GCM encryption.
 */
public class CryptoService {

    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 600_000;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a random salt for PBKDF2.
     *
     * @return salt bytes
     */
    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Derives an AES-256 key from a master password and salt.
     *
     * @param masterPassword master password characters
     * @param salt           PBKDF2 salt
     * @return derived secret key
     */
    public SecretKey deriveKey(char[] masterPassword, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(masterPassword, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new CryptoException(I18n.get("error.crypto.key"), e);
        }
    }

    /**
     * Creates a verifier hash for the master password (stored instead of the password).
     *
     * @param masterPassword master password characters
     * @param salt           PBKDF2 salt
     * @return base64-encoded SHA-256 hash of derived key bytes
     */
    public String createVerifier(char[] masterPassword, byte[] salt) {
        SecretKey key = deriveKey(masterPassword, salt);
        try {
            byte[] keyBytes = key.getEncoded();
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(keyBytes);
            Arrays.fill(keyBytes, (byte) 0);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new CryptoException(I18n.get("error.crypto.verifier"), e);
        }
    }

    /**
     * Verifies a master password against a stored verifier.
     *
     * @param masterPassword master password characters
     * @param salt           PBKDF2 salt
     * @param expectedVerifier stored verifier
     * @return true if password matches
     */
    public boolean verifyPassword(char[] masterPassword, byte[] salt, String expectedVerifier) {
        String actual = createVerifier(masterPassword, salt);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expectedVerifier.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Encrypts plaintext with AES-256-GCM.
     *
     * @param plaintext plaintext string
     * @param key       encryption key
     * @return base64-encoded IV + ciphertext
     */
    public String encrypt(String plaintext, SecretKey key) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new CryptoException(I18n.get("error.crypto.encrypt"), e);
        }
    }

    /**
     * Decrypts AES-256-GCM ciphertext.
     *
     * @param encrypted base64-encoded IV + ciphertext
     * @param key       encryption key
     * @return decrypted plaintext
     */
    public String decrypt(String encrypted, SecretKey key) {
        if (encrypted == null || encrypted.isBlank()) {
            return "";
        }
        try {
            byte[] data = Base64.getDecoder().decode(encrypted);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException(I18n.get("error.crypto.decrypt"), e);
        }
    }

    /**
     * Runtime exception for cryptographic failures.
     */
    public static class CryptoException extends RuntimeException {
        public CryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
