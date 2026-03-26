package com.laurentiuspilca.ssia.config;

import com.laurentiuspilca.ssia.security.CustomAuthenticationFailureHandler;
import com.laurentiuspilca.ssia.security.CustomAuthenticationSuccessHandler;
import com.laurentiuspilca.ssia.security.CustomEntryPoint;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableAsync
public class ProjectConfig {

//    private final AuthenticationProvider authenticationProvider;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    public ProjectConfig(/*AuthenticationProvider authenticationProvider,*/
                         CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                         CustomAuthenticationFailureHandler customAuthenticationFailureHandler) {
//        this.authenticationProvider = authenticationProvider;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
    }

//    @Bean
//    public InitializingBean initializingBean() {
//        return () -> SecurityContextHolder.setStrategyName(
//                SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
//    }

    @Bean
    public UserDetailsService userDetailsService() {
        var uds = new InMemoryUserDetailsManager();
        uds.createUser(
                User.withUsername("john")
                        .password("12345")
                        .authorities("read")
                        .build()
        );
        uds.createUser(
                User.withUsername("bill")
                        .password("12345")
                        .authorities("write")
                        .build()
        );
        return uds;
    }

    @Bean
    SecurityFilterChain configure(HttpSecurity http) throws Exception {
//        http.formLogin(Customizer.withDefaults());
//        http.formLogin(c -> c.defaultSuccessUrl("/home", true));
        http.formLogin(c -> c
                .successHandler(customAuthenticationSuccessHandler)
                .failureHandler(customAuthenticationFailureHandler)
        );
        http.httpBasic(Customizer.withDefaults());
        http.authorizeHttpRequests(c -> c.anyRequest().authenticated());
        return http.build();
    }
}
