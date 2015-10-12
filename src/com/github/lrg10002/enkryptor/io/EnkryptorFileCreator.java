package com.github.lrg10002.enkryptor.io;

import com.github.lrg10002.enkryptor.console.ConsoleWrapper;
import com.github.lrg10002.enkryptor.core.Enkryptor;
import com.github.lrg10002.enkryptor.encryption.Encryption;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;

public class EnkryptorFileCreator {

    private File file;
    private ConsoleWrapper console = Enkryptor.console;

    public EnkryptorFileCreator(File f) {
        file = f;
    }

    public void createFile(String primaryPass, String[] questions, String[] answers) throws IOException {
        console.println("Hashing password and security questions...");
        String primaryHash = Encryption.generateHash(primaryPass);
        String[] questionsEncrypted = new String[questions.length];
        for (int i = 0; i < questions.length; i++) {
            questionsEncrypted[i] = Encryption.encryptWithPrimary(questions[i]);
        }
        String[] answersHashed = new String[answers.length];
        for (int i = 0; i < answers.length; i++) {
            answersHashed[i] = Encryption.generateHash(answers[i]);
        }

        console.println("Generating and encrypting key...");

        char[] key = Encryption.generateRandomKey();
        String keystr = new String(key);

        Encryption.setPrimaryPass(primaryPass);
        Encryption.setAnswers(answers);
        Encryption.setKey(key);

        keystr = Encryption.encryptWithAnswers(keystr);
        keystr = Encryption.encryptWithPrimary(keystr);

        console.println("Writing to file...");
        writeToFile(primaryHash, questionsEncrypted, answersHashed, keystr);
    }

    private void writeToFile(String passhash, String[] eq, String[] ea, String enkey) throws IOException {
        FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE);

        BigByteBuffer header = new BigByteBuffer();

        ByteBuffer passhashLength = ByteBuffer.allocate(Integer.BYTES);
        ByteBuffer passhashBuff = ByteBuffer.wrap(passhash.getBytes(Charset.forName("UTF-8")));

        passhashLength.putInt(passhashBuff.capacity());
        passhashLength.flip();

        header.addAll(passhashLength, passhashBuff);

        ByteBuffer numOfQuestions = ByteBuffer.allocate(Integer.BYTES);
        numOfQuestions.putInt(eq.length);
        numOfQuestions.flip();

        header.addAll(numOfQuestions);

        for (int i = 0; i < eq.length; i++) {
            ByteBuffer questionLength = ByteBuffer.allocate(Integer.BYTES);
            ByteBuffer questionBuff = ByteBuffer.wrap(eq[i].getBytes(Charset.forName("UTF-8")));
            questionLength.putInt(questionBuff.capacity());
            questionLength.flip();

            ByteBuffer answerLength = ByteBuffer.allocate(Integer.BYTES);
            ByteBuffer answerBuff = ByteBuffer.wrap(ea[i].getBytes(Charset.forName("UTF-8")));
            answerLength.putInt(answerBuff.capacity());
            answerLength.flip();

            header.addAll(questionLength, questionBuff, answerLength, answerBuff);
        }

        ByteBuffer keyLength = ByteBuffer.allocate(Integer.BYTES);
        ByteBuffer keyBuff = ByteBuffer.wrap(enkey.getBytes(Charset.forName("UTF-8")));
        keyLength.putInt(keyBuff.capacity());
        keyLength.flip();

        header.addAll(keyLength, keyBuff);

        ByteBuffer fileCount = ByteBuffer.allocate(Integer.BYTES);
        fileCount.putInt(0);
        fileCount.flip();

        header.addAll(fileCount);

        write(header.makeBigBufferWithLength(), channel);

        channel.close();
    }

    private void write(ByteBuffer buff, FileChannel c) throws IOException {
        while (buff.hasRemaining()) {
            c.write(buff);
        }
    }
}
