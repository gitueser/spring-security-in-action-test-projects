package com.laurentiuspilca.ssia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@SpringBootTest
@AutoConfigureWebTestClient
class MainTests {

    @Autowired
    private WebTestClient client;

    @Test
    @WithMockUser
    void testCallHelloWithValidUser() {
        client.get()
                .uri("/hello")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testCallHelloWithValidUserWithMockUser() {
        client.mutateWith(mockUser())
                .get()
                .uri("/hello")
                .exchange()
                .expectStatus().isOk();
    }
}
