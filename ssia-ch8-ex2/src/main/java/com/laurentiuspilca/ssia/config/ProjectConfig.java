package com.laurentiuspilca.ssia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ProjectConfig {

    @Bean
    SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http.httpBasic(Customizer.withDefaults());

        http.authorizeHttpRequests(c -> c
                .requestMatchers(HttpMethod.GET, "/a").authenticated()
                .requestMatchers(HttpMethod.POST, "/a").permitAll()
                .anyRequest().denyAll()
        );

        http.csrf(c -> c.disable());

        return http.build();
    }
}
