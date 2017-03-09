package com.insertcoolnamehere.showandsell.logic;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;

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

    /**
     * Decrypts a String using Base 64 and then an un-Caesar shift
     * @param original encrypted String
     * @return decrypted, human-readable String
     */
    public static String decrypt(String original) {
        // undo base 64
        original = new String(Base64.decode(original.getBytes(), Base64.NO_WRAP));

        // undo Caesar shift
        String decrypted = "";
        for(int i = 0; i < original.length(); i++) {
            decrypted += Character.toChars(original.charAt(i)-1)[0];
        }

        return decrypted;
    }
}
