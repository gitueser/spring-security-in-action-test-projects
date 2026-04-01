package com.laurentiuspilca.ssia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

@Configuration
public class ProjectConfig {

    @Bean
    SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http.httpBasic(Customizer.withDefaults());
        String expression = "hasAuthority('read') and !hasAuthority('delete')";

        http.authorizeHttpRequests(c -> c.anyRequest()
                .access(new WebExpressionAuthorizationManager(expression))
        );

        return http.build();
    }
}
