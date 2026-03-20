package com.laurentiuspilca.ssia.config;

//import com.laurentiuspilca.ssia.security.CustomAuthenticationProvider;

import com.laurentiuspilca.ssia.security.AuthenticationLoggingFilter;
import com.laurentiuspilca.ssia.security.RequestValidationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class WebAuthorizationConfig {

//    private final CustomAuthenticationProvider authenticationProvider;
//
//    public WebAuthorizationConfig(CustomAuthenticationProvider authenticationProvider) {
//        this.authenticationProvider = authenticationProvider;
//    }

    @Bean
    SecurityFilterChain configure(HttpSecurity http) throws Exception {

        http
//                .addFilterBefore(new MyFilter(), BasicAuthenticationFilter.class)
                .addFilterBefore(new RequestValidationFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(new AuthenticationLoggingFilter(), BasicAuthenticationFilter.class)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(c -> c
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }


//    @Bean
//    SecurityFilterChain configure(HttpSecurity http) throws Exception {
//
//        http.httpBasic(Customizer.withDefaults());
//
////        http.authenticationProvider(authenticationProvider);
//
//        http.authorizeHttpRequests(
//                c -> c.anyRequest().authenticated()
//        );
//
//        return http.build();
//    }
}
