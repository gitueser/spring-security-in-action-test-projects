package com.laurentiuspilca.ssia.controllers;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String getHello() {
        return "Get Hello!";
    }

    @GetMapping("/csrf")
    public String csrf(CsrfToken token) {
        return token.getToken();
    }

    @PostMapping("/hello")
    public String postHello() {
        return "Post Hello!";
    }
}
