package com.laurentiuspilca.ssia.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HelloController {

//    @GetMapping("/hello")
//    public Mono<String> hello(Mono<Authentication> auth) {
//        Mono<String> message = auth.map(a -> "Hello " + a.getName());
//        return message;
//    }

    @GetMapping("/hello")
    public Mono<String> hello() {
        Mono<String> message = ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> "Hello from Context " + auth.getName());
        return message;
    }
}
