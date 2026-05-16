package com.laurentiuspilca.ssia.controllers;

import com.laurentiuspilca.ssia.config.CustomAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DemoController {

//    @GetMapping("/demo")
//    public Authentication demo(Authentication authentication) {
//        return authentication;
//    }


//    To return only the required fields, rather than the entire Authentication object
    @GetMapping("/demo")
    public Map<String, String> demo(CustomAuthentication authentication) {
        return Map.of(
                "name", authentication.getName(),
                "priority", authentication.getPriority()
        );
    }
}
