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
{"access_token":"","scope":"openid","id_token":"eyJraWQiOiI5MTdjMWExYy03ZDA0LTQ4MTUtYWJmZi1lMjNjYjE1NTFhMjIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJiaWxsIiwiYXVkIjoiY2xpZW50IiwiYXpwIjoiY2xpZW50IiwiYXV0aF90aW1lIjoxNzc4ODc0ODU0LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJleHAiOjE3Nzg4NzY3MjYsImlhdCI6MTc3ODg3NDkyNiwianRpIjoiMzJkYmNlNjQtOTc3OS00ZDQ4LWFkNmUtZTQxZWI2MmFjNzM5Iiwic2lkIjoiTXdROUNBUUxEbkxXNmFRaVo3d1dfZFBNWUI1LWc1aUs1VFVEcEtOcEJ4WSJ9.LyEym-e7vthFLTuIb8FSyKDvHGWmJj4CgRMIlVCVFXkOpVjlIrCc26LfOvTjSe8efiQK4UAaNgDzT6yIgIVdbaJ71LePXnultapJzBlDeZh7eTS9IYRpyXrlf8oXhoK3243KqO3Os9Yi0ivyLyO1PDluOgqzsbOGZmk69dUvwbQopCtfF30dYePO_0s8mOK6SMxlGPFwxoIJlUEeNngVb76Iq0TBjGjY-PRdpBe6L4hzzKt5rw_Y2OSf7bp4tZ4ozt094V4W3dptR2YGBsCFcnSCzCmB2ejRjVjRBOwue-n4Bt67rPrJPleVz3hKHhGQ_FZ94KW4y2oJfgEUA9zxkQ","token_type":"Bearer","expires_in":300}

curl -i "http://localhost:9090/demo" \
        -H "Authorization: Bearer eyJraWQiOiI5MTdjMWExYy03ZDA0LTQ4MTUtYWJmZi1lMjNjYjE1NTFhMjIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJiaWxsIiwiYXVkIjoiY2xpZW50IiwibmJmIjoxNzc4ODc0OTI2LCJzY29wZSI6WyJvcGVuaWQiXSwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiZXhwIjoxNzc4ODc1MjI2LCJpYXQiOjE3Nzg4NzQ5MjYsImp0aSI6ImI3MWFhYjM0LTM1YzYtNDA3OS05ZGY4LTZmMjJiZjkxOTI2NSJ9.d2lHBpW-7nmqfc1OAMSTWvRQwqpOf4D7ENk_kx-R1N6UvntrSy9zfXGcEMHk7KBkTBeKxLw5TW_mxxliepofYZvnehLZT-xyCD4HgEy8D-sVEJIKA0y7ho822S0KyoZXExU9UKp4Be6A660XEUwsD1rg8ucP34iQAzAADl-rbRVSaKPrCsqiKZVsxnCoTZXkliIYfD-rcAPUTErGrTG1ukpjIfdfJI0hY22vVjHMCSyl3Z4LGeVcW8ZHE5xC0zpBTKS67trs2BaUMmQcgSbG8755nzfBaoIrmzshmc-dudK7I7IJ6H_4bynRNbcyQwNvpflLljffnldRS9hYOjmPAg"