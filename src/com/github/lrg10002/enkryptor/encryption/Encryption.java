package com.github.lrg10002.enkryptor.encryption;

import org.jasypt.digest.StandardStringDigester;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.salt.RandomSaltGenerator;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

public class Encryption {

    private static char[] primaryPass;
    private static ArrayList<char[]> answers;
    private static char[] key;

    private static StandardStringDigester digester = new StandardStringDigester();
    private static SecureRandom random = new SecureRandom();
    private static StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
    private static String encryptorPass = "";
    private static StandardPBEByteEncryptor bencryptor = new StandardPBEByteEncryptor();
    private static String bencryptorPass = "";

    static {
        initDigester();
        initEncryptor();
        initBencryptor();
    }

    private static void initDigester() {
        digester = new StandardStringDigester();
        digester.initialize();
        digester.setIterations(1000);
        digester.setSaltGenerator(new RandomSaltGenerator());
        digester.setStringOutputType("hexadecimal");
    }

    private static void initEncryptor() {
        encryptor = new StandardPBEStringEncryptor();
        encryptor.initialize();
        encryptor.setStringOutputType("hexadecimal");
        encryptor.setPassword(encryptorPass);
    }

    private static void initBencryptor() {
        bencryptor = new StandardPBEByteEncryptor();
        bencryptor.initialize();
        bencryptor.setPassword(bencryptorPass);
    }

    public static void setPrimaryPass(String p) {
        primaryPass = p.toCharArray();
    }

    public static void setAnswer(int i, String p) {
        if (i < answers.size()) {
            answers.set(i, p.toCharArray());
        } else {
            answers.add(i, p.toCharArray());
        }
    }

    public static void addAnswer(String p) {
        setAnswer(answers.size(), p);
    }

    public static void setAnswers(String[] ps) {
        answers.clear();
        for (String s : ps) {
            addAnswer(s);
        }
    }

    public static void setKey(char[] k) {
        key = k;
        bencryptorPass = new String(k);
        initBencryptor();
    }

    public static boolean checkPassword(String pass, String hash) {
        initDigester();
        return digester.matches(pass, hash);
    }

    public static String generateHash(String pass) {
        initDigester();
        return digester.digest(pass);
    }

    private static final char[] KEYCHARS = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z',
            '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'
    };

    public static char[] generateRandomKey() {
        char[] key = new char[20];
        for (int i = 0; i < key.length; i++) {
            key[i] = KEYCHARS[random.nextInt(KEYCHARS.length)];
        }
        return key;
    }

    public static String encryptWithPrimary(String p) {
        encryptorPass = new String(primaryPass);
        initEncryptor();
        return encryptor.encrypt(p);
    }

    public static String encryptWithAnswers(String p) {
        String encr = p;
        for (int i = answers.size()-1; i <= 0; i--) {
            encryptorPass = new String(answers.get(i));
            initEncryptor();
            encr = encryptor.encrypt(encr);
        }
        return encr;
    }

    public static String decryptWithPrimary(String p) {
        encryptorPass = new String(primaryPass);
        initEncryptor();
        return encryptor.decrypt(p);
    }

    public static String decryptWithAnswers(String p) {
        String decr = p;
        for (int i = 0; i < answers.size(); i++) {
            encryptorPass = new String(answers.get(i));
            initEncryptor();
            decr = encryptor.decrypt(decr);
        }
        return decr;
    }

    public static String decryptWithKey(String s) {
        encryptorPass = new String(key);
        initEncryptor();
        return encryptor.decrypt(s);
    }

    public static String encryptWithKey(String s) {
        encryptorPass = new String(key);
        initEncryptor();
        return encryptor.encrypt(s);
    }

    public static byte[] encryptWithKey(byte[] bytes) {
        initBencryptor();
        return bencryptor.encrypt(bytes);
    }

    public static byte[] decryptWithKey(byte[] bytes) {
        initBencryptor();
        return bencryptor.decrypt(bytes);
    }


}
