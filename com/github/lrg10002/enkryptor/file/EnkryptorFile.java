package com.github.lrg10002.enkryptor.file;

import com.github.lrg10002.enkryptor.encryption.Encryption;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class EnkryptorFile {

    private static final int ENCRYPTION_BLOCK_SIZE = 1024 * 32 - 1;

    private File file;
    private FileChannel channel;

    private String primaryPassHash;
    private String[] questionsEncrypted;
    private String[] answersHashed;
    private String encryptedKey;

    private boolean okPassword = false, okAnswers = false, okUnlocked = false;

    private int cachedHeaderLength;
    private int cachedNumFiles = 0;
    private Map<String, Long> cachedFileMappings = new HashMap<>();


    public EnkryptorFile(File f) throws IOException {
        file = f;
        channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
    }

    public void readHeader() throws IOException {
        ByteBuffer headerLength = ByteBuffer.allocateDirect(Integer.BYTES);
        channel.read(headerLength);
        headerLength.flip();

        int hlength = headerLength.getInt();
        cachedHeaderLength = hlength;

        //load entire header into memory
        ByteBuffer header = ByteBuffer.allocateDirect(hlength);
        channel.read(header);
        header.flip();

        primaryPassHash = readStringWithLength(header);
        int nquestions = header.getInt();
        questionsEncrypted = new String[nquestions];
        answersHashed = new String[nquestions];
        for (int i = 0; i < nquestions; i++) {
            questionsEncrypted[i] = readStringWithLength(header);
            answersHashed[i] = readStringWithLength(header);
        }

        encryptedKey = readStringWithLength(header);

        cachedNumFiles = header.getInt();

        channel.position(0);
    }

    public boolean validatePrimaryPass(String pass) {
        if (Encryption.checkPassword(pass, primaryPassHash)) {
            Encryption.setPrimaryPass(pass);
            okPassword = true;
            return true;
        } else {
            return false;
        }
    }

    public int getNumberOfQuestions() {
        return questionsEncrypted.length;
    }

    public String getQuestionDecrypted(int i) {
        return Encryption.decryptWithPrimary(questionsEncrypted[i]);
    }

    public boolean validateAnswer(int i, String ans) {
        return Encryption.checkPassword(ans, answersHashed[i]);
    }

    public void setAnswers(String[] answers) {
        Encryption.setAnswers(answers);
        okAnswers = true;
    }

    public void unlock() {
        if (!(okPassword && okAnswers)) {
            return;
        }
        String decr = Encryption.decryptWithPrimary(encryptedKey);
        decr = Encryption.decryptWithAnswers(decr);
        Encryption.setKey(decr.toCharArray());
        okUnlocked = true;
    }

    public void cacheFiles() throws IOException {
        if (!okUnlocked) {
            throw new IllegalStateException("not unlocked!");
        }
        channel.position(cachedHeaderLength); //position past header in file section

        cachedFileMappings.clear();
        for (int i = 0; i < cachedNumFiles; i++) {
            long startingpos = channel.position();
            ByteBuffer fslen = ByteBuffer.allocate(Integer.BYTES);
            channel.read(fslen);
            fslen.flip();
            int sectionlen = fslen.getInt();
            ByteBuffer namelen = ByteBuffer.allocate(Integer.BYTES);
            channel.read(namelen);
            namelen.flip();
            ByteBuffer namebuff = ByteBuffer.allocateDirect(namelen.getInt());
            channel.read(namebuff);
            namebuff.flip();

            cachedFileMappings.put(Encryption.decryptWithKey(new String(namebuff.array(), "UTF-8")), startingpos);

            channel.position(startingpos + sectionlen); //skip to next file section
        }
    }

    public boolean fileExists(String fn) {
        return cachedFileMappings.containsKey(fn);
    }

    public void addFile(String name, String[] tags, File other) throws IOException {
        if (!okUnlocked) {
            throw new IllegalStateException("not unlocked!");
        }
        if (cachedFileMappings.containsKey(name)) {
            throw new IllegalArgumentException("File already exists!");
        }

        FileChannel in = FileChannel.open(other.toPath(), StandardOpenOption.READ);

        channel.position(channel.size());
        final long top = channel.size();

        long fileSectionLength;

        //HEADER
        ByteBuffer namebuff = ByteBuffer.wrap(Encryption.encryptWithKey(name).getBytes(Charset.forName("UTF-8")));
        ByteBuffer namelen = ByteBuffer.allocate(Integer.BYTES);
        namelen.putInt(namebuff.capacity());
        namelen.flip();

        String joinedTags = String.join(",", tags);
        ByteBuffer tagsbuff = ByteBuffer.wrap(Encryption.encryptWithKey(joinedTags).getBytes(Charset.forName("UTF-8")));
        ByteBuffer tagslen = ByteBuffer.allocate(Integer.BYTES);
        tagslen.putInt(tagsbuff.capacity());
        tagslen.flip();

        //FILE
        ByteBuffer filelen = ByteBuffer.allocate(Long.BYTES);
        filelen.putLong(in.size() + (long) (Math.ceil((double) in.size()/(double) ENCRYPTION_BLOCK_SIZE)*Integer.BYTES));
        filelen.flip();

        fileSectionLength = namebuff.capacity() + namelen.capacity()
                + tagslen.capacity() + tagsbuff.capacity()
                + filelen.capacity() + in.size();

        ByteBuffer sectionlen = ByteBuffer.allocate(Long.BYTES);
        sectionlen.putLong(fileSectionLength);
        sectionlen.flip();

        write(sectionlen);
        write(namelen);
        write(namebuff);
        write(tagslen);
        write(tagsbuff);
        write(filelen);

        transferAndEncrypt(in);
        channel.force(false);

        in.close();

        cachedFileMappings.put(name, top);
        cachedNumFiles++;

    }

    private void transferAndEncrypt(FileChannel in) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(ENCRYPTION_BLOCK_SIZE);
        ByteBuffer len = ByteBuffer.allocate(Integer.BYTES);
        for (int bread; (bread = in.read(buffer)) >= 0; ) {
            buffer.flip();
            ByteBuffer wrapped;
            if (bread != buffer.capacity()) {
                byte[] buff = new byte[bread];
                System.arraycopy(buffer.array(), 0, buff, 0, bread);
                wrapped = ByteBuffer.wrap(Encryption.encryptWithKey(buff));
            } else {
                wrapped = ByteBuffer.wrap(Encryption.encryptWithKey(buffer.array()));
            }
            len.putInt(wrapped.capacity());
            len.flip();
            channel.write(len);
            channel.write(wrapped);

            buffer.clear();
            buffer.flip();
        }
    }

    private void transferAndDecrypt(long bytes, FileChannel out) throws IOException {
        long start = channel.position();
        ByteBuffer len = ByteBuffer.allocate(Integer.BYTES);
        while (channel.position() < start + bytes) {
            int bread = channel.read(len);
            len.flip();
            int blocklen = len.getInt();
            len.clear();
            len.flip();

            ByteBuffer buffer = ByteBuffer.allocateDirect(blocklen);
            channel.read(buffer);
            buffer.flip();
            byte[] decr = Encryption.decryptWithKey(buffer.array());
            ByteBuffer wrapped = ByteBuffer.wrap(decr);
            out.write(wrapped);
        }
    }

    private void write(ByteBuffer bb) throws IOException {
        while (bb.hasRemaining()) channel.write(bb);
    }

    private String readStringWithLength(ByteBuffer bb) throws IOException {
        byte[] bytes = new byte[bb.getInt()];
        bb.get(bytes);
        return new String(bytes, "UTF-8");
    }
}
