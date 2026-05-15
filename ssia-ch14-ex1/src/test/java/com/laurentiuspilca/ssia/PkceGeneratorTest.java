package com.laurentiuspilca.ssia;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceGeneratorTest {

    @Test
    void generatePkceValues() throws Exception {

        // ---------- CODE VERIFIER ----------

        SecureRandom secureRandom = new SecureRandom();

        byte[] code = new byte[32];
        secureRandom.nextBytes(code);

        String codeVerifier = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(code);

        // ---------- CODE CHALLENGE ----------

        MessageDigest messageDigest =
                MessageDigest.getInstance("SHA-256");

        byte[] digested = messageDigest.digest(
                codeVerifier.getBytes(StandardCharsets.US_ASCII)
        );

        String codeChallenge = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(digested);

        // ---------- OUTPUT ----------

        System.out.println();
        System.out.println("CODE VERIFIER:");
        System.out.println(codeVerifier);

        System.out.println();
        System.out.println("CODE CHALLENGE:");
        System.out.println(codeChallenge);

        System.out.println();
        System.out.println("AUTHORIZATION URL:");

        String url =
                "http://localhost:8080/oauth2/authorize" +
                        "?response_type=code" +
                        "&client_id=client" +
                        "&scope=openid" +
                        "&redirect_uri=https://www.manning.com/authorized" +
                        "&code_challenge=" + codeChallenge +
                        "&code_challenge_method=S256";

        System.out.println(url);
    }
}
