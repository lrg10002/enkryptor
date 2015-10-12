package com.github.lrg10002.enkryptor;

import javax.crypto.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class EKFile {

    /*
    Format:
        8 byte salt
        password hash (prefixstring)
        number of files (int)
        for each file,
            prefixstring file code & prefix bytes file name
            num of tags (int)
            prefbytes tags
     */

    public File folder;
    public File propsFile;
    public EKProperties properties;

    public EKFile(File f) {
        folder = f;
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (!folder.isDirectory()) {
            throw new RuntimeException("The specified file is not a directory!");
        }
        propsFile = new File(folder, "enkryptor.ekp");
    }

    public boolean checkPassword(String pass) {
        try (DataInputStream str = new DataInputStream(new FileInputStream(propsFile))) {
            byte[] salt = new byte[8];
            str.readFully(salt);
            String passhash = str.readUTF();
            if (!Crypto.getHash(pass, salt).equals(passhash)) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean isLoaded() {
        return properties != null;
    }

    public void load(String password) {
        Cipher c = Crypto.getDecryptCipher(Crypto.genKey(password));
        String passhash;
        byte[] salt = new byte[8];
        try (DataInputStream dis = new DataInputStream(new FileInputStream(propsFile))) {
            dis.readFully(salt);
            passhash = dis.readUTF();
            int numFiles = dis.readInt();
            Map<String, String> names = new HashMap<>();
            Map<String, List<String>> tagMap = new HashMap<>();
            for (int i = 0; i < numFiles; i++) {
                String code = dis.readUTF();
                byte[] encName = new byte[dis.readInt()];
                dis.readFully(encName);
                byte[] decName = c.doFinal(encName);
                String name = new String(decName, StandardCharsets.UTF_8);
                names.put(code, name);
                int numTags = dis.readInt();
                List<String> tags = new ArrayList<>();
                for (int ii = 0; ii < numTags; i++) {
                    byte[] encTag = new byte[dis.readInt()];
                    dis.readFully(encTag);
                    byte[] decTag = c.doFinal(encTag);
                    String tag = new String(decTag, StandardCharsets.UTF_8);
                    tags.add(tag);
                }
                tagMap.put(code, tags);
            }

            properties = new EKProperties(passhash, salt, names, tagMap);

        } catch (FileNotFoundException e) {
            throw new RuntimeException("The properties file doesn't exist! Was it renamed or deleted?", e);
        } catch (IOException e) {
            throw new RuntimeException("An IO Exception occurred during loading!", e);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new RuntimeException("A decryption error has occurred", e);
        }
    }

    public void writeProperties(String password) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(propsFile))) {
            Cipher c = Crypto.genEncryptCipher(Crypto.genKey(password));
            dos.write(properties.salt);
            dos.writeUTF(properties.passhash);
            dos.writeInt(properties.nameKeys.size());
            for (Map.Entry<String, String> e : properties.nameKeys.entrySet()) {
                dos.writeUTF(e.getKey());
                byte[] decName = e.getValue().getBytes(StandardCharsets.UTF_8);
                byte[] encName = c.doFinal(decName);
                dos.writeInt(encName.length);
                dos.write(encName);

                dos.writeInt(properties.tags.get(e.getKey()).size());
                for (String tag : properties.tags.get(e.getKey())) {
                    byte[] decTag = tag.getBytes(StandardCharsets.UTF_8);
                    byte[] encTag = c.doFinal(decTag);
                    dos.writeInt(encTag.length);
                    dos.write(encName);
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            throw new RuntimeException("An IO Exception occurred during loading!", e);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new RuntimeException("An encryption error has occurred", e);
        }
    }

    public void deleteFile(String fid) {
        if (!properties.nameKeys.containsKey(fid)) {
            throw new RuntimeException("The file specified does not exist!");
        }

        File f = new File(folder, fid);
        if (!f.exists()) {
            properties.nameKeys.remove(fid);
            properties.tags.remove(fid);
            throw new RuntimeException("The file specified has already been deleted!");
        }

        f.delete();
    }

    public void addFile(File in, String name, List<String> tags, String password) {
        if (!in.exists()) {
            throw new RuntimeException("The file specified does not exist!");
        }
        if (in.isDirectory()) {
            throw new RuntimeException("The file specified is a directory!");
        }
        if (!name.startsWith("/")) {
            name = "/" + name;
        }

        String hash;
        while (properties.nameKeys.containsKey((hash = Crypto.getMD5Hash(name)))) {
            String otherName = properties.nameKeys.get(hash);
            System.out.println("name: " + name + " othername: " + otherName);
            if (otherName.equals(name)) {
                throw new RuntimeException("A file already exists with that name!");
            }
        }
        properties.nameKeys.put(hash, name);

        properties.tags.put(hash, tags);

        writeFile(in, new File(folder, hash), password);
    }

    public void updateFile(String fid, File in, String password) {
        if (!properties.nameKeys.containsKey(fid)) {
            throw new RuntimeException("The file specified does not already exist!");
        }

        File out = new File(folder, properties.nameKeys.get(fid));
        writeFile(in, out, password);
    }

    private void writeFile(File in, File out, String password) {
        Cipher c = Crypto.genEncryptCipher(Crypto.genKey(password));
        try (CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(out), c);
             FileInputStream fis = new FileInputStream(in)) {
            byte[] buff = new byte[1024 * 64];
            for (int bread; (bread = fis.read(buff)) >= 0; ) {
                cos.write(buff, 0, bread);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportFile(String fid, File out, String password) {
        Cipher c = Crypto.getDecryptCipher(Crypto.genKey(password));
        File in = new File(folder, fid);
        if (out.isDirectory()) {
            throw new RuntimeException("Cannot export to specified location because a directory exists with that name!");
        }
        if (!out.exists()) {
            try {
                out.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create output file!", e);
            }
        }
        try (CipherInputStream cis = new CipherInputStream(new FileInputStream(in), c);
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buff = new byte[1024 * 64];
            for (int bread; (bread = cis.read(buff)) >= 0;) {
                fos.write(buff, 0, bread);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static EKFile init(File folder, String password) {
        if (!folder.exists()) {
            throw new RuntimeException("The Enkryptor folder must already exist!");
        }
        byte[] salt = Crypto.genSalt();
        Cipher c = Crypto.genEncryptCipher(Crypto.genKey(password));
        File out = new File(folder, "enkryptor.ekp");
        if (out.exists()) {
            throw new RuntimeException("Cannot initialize an existing Enkryptor folder!");
        }
        try {
            out.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create the properties file!", e);
        }
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(out))) {
            dos.write(salt);
            dos.writeUTF(Crypto.getHash(password, salt));
            dos.writeInt(0);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("The Enkryptor properties file could not be opened!", e);
        } catch (IOException e) {
            throw new RuntimeException("An IO Exception occurred during initialization!", e);
        }

        return new EKFile(folder);
    }

    public List<String> listFiles(String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        final String fpath = path;
        return properties.nameKeys.entrySet().stream()
                .filter(es -> es.getValue().startsWith(fpath) && !es.getValue().replaceFirst(fpath, "").contains("/"))
                .map(es -> es.getValue())
                .collect(Collectors.toList());
    }

    public String pathToId(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        final String fpath = path;
        Optional<String> fid = properties.nameKeys.values().stream().filter(p -> p.equals(fpath)).findFirst();
        if (fid.isPresent()) {
            return fid.get();
        }
        throw new RuntimeException("The name specified does not exist!");
    }

    public List<String> getTags(String fid) {
        return properties.tags.get(fid);
    }


}
