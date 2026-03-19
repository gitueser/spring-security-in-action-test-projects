package com.laurentiuspilca.ssia.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

//    @GetMapping("/hello")
//    public String hello() {
//        return "Hello!";
//    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hello!");
    }
}
