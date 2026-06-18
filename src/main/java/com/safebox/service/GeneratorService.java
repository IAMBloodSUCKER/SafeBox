package com.safebox.service;

import com.safebox.util.I18n;

import java.security.SecureRandom;

/**
 * Secure random password generator.
 */
public class GeneratorService {
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a password with the given options.
     *
     * @param length      password length (8–128)
     * @param useUpper    include uppercase letters
     * @param useLower    include lowercase letters
     * @param useDigits   include digits
     * @param useSpecial  include special characters
     * @return generated password
     */
    public String generate(int length, boolean useUpper, boolean useLower, boolean useDigits, boolean useSpecial) {
        if (length < 8 || length > 128) {
            throw new IllegalArgumentException(I18n.get("error.generator.length"));
        }
        StringBuilder alphabet = new StringBuilder();
        if (useUpper) {
            alphabet.append(UPPER);
        }
        if (useLower) {
            alphabet.append(LOWER);
        }
        if (useDigits) {
            alphabet.append(DIGITS);
        }
        if (useSpecial) {
            alphabet.append(SPECIAL);
        }
        if (alphabet.isEmpty()) {
            throw new IllegalArgumentException(I18n.get("error.generator.charset"));
        }

        String chars = alphabet.toString();
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
