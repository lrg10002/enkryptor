package com.github.lrg10002.enkryptor;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class Crypto {

    private static SecureRandom random = new SecureRandom();

    public static Cipher genEncryptCipher(Key sk) {
        try {
            Cipher c = Cipher.getInstance("AES/CFB8/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, sk, new IvParameterSpec(new byte[16]));
            return c;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption cipher!", e);
        }
    }

    public static Cipher getDecryptCipher(Key sk) {
        try {
            Cipher c = Cipher.getInstance("AES/CFB8/NoPadding");
            c.init(Cipher.DECRYPT_MODE, sk, new IvParameterSpec(new byte[16]));
            return c;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate decryption cipher!", e);
        }
    }

    public static Key genKey(String password) {
        byte[] pwd = password.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            pwd = digest.digest(pwd);
            pwd = Arrays.copyOf(pwd, 16);
            return new SecretKeySpec(pwd, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to create password key!", e);
        }
    }

    public static String getHash(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(salt);
            byte[] input = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < 998; i++) {
                digest.reset();
                input = digest.digest(input);
            }
            return new String(input, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password!", e);
        }
    }

    public static byte[] genSalt() {
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        return salt;
    }

    public static String getMD5Hash(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            byte[] res = digest.digest(s.getBytes(StandardCharsets.UTF_8));
            String hex = DatatypeConverter.printHexBinary(res).substring(0, 16);
            return hex;
        } catch (Exception e) {
            throw new RuntimeException("Failed hashing operation!", e);
        }
    }
}
