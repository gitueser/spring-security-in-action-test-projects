package com.laurentiuspilca.ssia.security;

import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Component
public class Sha512PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return hashWithSHA512(rawPassword.toString());
    }

    private String hashWithSHA512(String input) {
        StringBuilder result = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digested = md.digest(input.getBytes(StandardCharsets.UTF_8));
            for (byte b : digested) {
                result.append(String.format("%02x", b));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Bad algorithm", e);
        }
        return result.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {

        StringKeyGenerator keyGeneratorStringKeyGenerator = KeyGenerators.string();
        String salt = keyGeneratorStringKeyGenerator.generateKey();
        System.out.println("MY StringKeyGenerator's salt is: " + salt);

//        BytesKeyGenerator keyGeneratorBytesKeyGenerator = KeyGenerators.secureRandom(16);
        BytesKeyGenerator keyGeneratorBytesKeyGenerator = KeyGenerators.shared(16);
        byte [] key_01 = keyGeneratorBytesKeyGenerator.generateKey();
        byte [] key_02 = keyGeneratorBytesKeyGenerator.generateKey();
        int keyLength = keyGeneratorBytesKeyGenerator.getKeyLength();
        System.out.println("MY BytesKeyGenerator's key_01 is: " + Arrays.toString(key_01));
        System.out.println("MY BytesKeyGenerator's key_02 is: " + Arrays.toString(key_02));
        System.out.println("MY BytesKeyGenerator's key length is: " + keyLength);


        String hashedRawPassword = encode(rawPassword);
        return hashedRawPassword.equals(encodedPassword);
    }
}
