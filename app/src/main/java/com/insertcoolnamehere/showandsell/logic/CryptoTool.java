package com.insertcoolnamehere.showandsell.logic;

import android.util.Base64;

/**
 * A tool to help with encryption and decryption
 */

public class CryptoTool {
    /**
     * Encrypts a String using a simple Caesar shift and then Base 64
     * @param original the String to encrypt
     * @return encrypted String
     */
    public static String encrypt(String original) {
        String encrypted = "";

        // Caesar shift
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            c = Character.toChars(Character.valueOf(c)+1)[0];
            encrypted += c;
        }

        // base 64
        encrypted = Base64.encodeToString(encrypted.getBytes(), Base64.NO_WRAP);

        return encrypted;
    }
}
