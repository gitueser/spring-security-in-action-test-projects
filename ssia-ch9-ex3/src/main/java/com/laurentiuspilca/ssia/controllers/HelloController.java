package com.laurentiuspilca.ssia.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String postHello() {
        return "Post Hello!";
    }

    @GetMapping("/ciao")
    public String postCiao() {
        return "Post Ciao!";
    }
}
