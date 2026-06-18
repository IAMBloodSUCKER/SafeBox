package com.safebox.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoServiceTest {

    private CryptoService cryptoService;
    private byte[] salt;
    private char[] password;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService();
        salt = cryptoService.generateSalt();
        password = "test-master-password".toCharArray();
    }

    @Test
    void encryptDecrypt_roundTrip() {
        SecretKey key = cryptoService.deriveKey(password, salt);
        String original = "SuperSecret123!";
        String encrypted = cryptoService.encrypt(original, key);
        String decrypted = cryptoService.decrypt(encrypted, key);
        assertEquals(original, decrypted);
    }

    @Test
    void verifyPassword_acceptsCorrectPassword() {
        String verifier = cryptoService.createVerifier(password, salt);
        assertTrue(cryptoService.verifyPassword(password, salt, verifier));
    }

    @Test
    void verifyPassword_rejectsWrongPassword() {
        String verifier = cryptoService.createVerifier(password, salt);
        char[] wrong = "wrong-password".toCharArray();
        assertFalse(cryptoService.verifyPassword(wrong, salt, verifier));
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime() {
        SecretKey key = cryptoService.deriveKey(password, salt);
        String a = cryptoService.encrypt("same", key);
        String b = cryptoService.encrypt("same", key);
        assertNotEquals(a, b);
        assertEquals("same", cryptoService.decrypt(a, key));
        assertEquals("same", cryptoService.decrypt(b, key));
    }
}
