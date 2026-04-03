package com.laurentiuspilca.ssia.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }

    @GetMapping("/ciao")
    public String ciao() {
        return "Ciao!";
    }

    @GetMapping("/hola")
    public String hola() {
        return "hola!";
    }

    @PostMapping("/a")
    public String postEndpointA() {
        return "Works postEndpointA!";
    }

    @GetMapping("/a")
    public String getEndpointA() {
        return "Works getEndpointA!";
    }

    @GetMapping("/a/b")
    public String getEnpointB() {
        return "Works getEnpointB!";
    }

    @GetMapping("/a/b/c")
    public String getEnpointC() {
        return "Works getEnpointC!";
    }
}
