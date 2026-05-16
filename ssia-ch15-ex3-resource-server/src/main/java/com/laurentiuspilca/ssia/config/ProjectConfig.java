package com.laurentiuspilca.ssia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ProjectConfig {

    @Value("${keySetURI}")
    private String keySetURI;

    private final CustomJwtAuthenticationConverter converter;

    public ProjectConfig(CustomJwtAuthenticationConverter converter) {
        this.converter = converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(c -> c
                .jwt(j -> j
                        .jwkSetUri(keySetURI)
                        .jwtAuthenticationConverter(converter)
                )
        );
        http.authorizeHttpRequests(c -> c.anyRequest().authenticated());
        return http.build();
    }
}
