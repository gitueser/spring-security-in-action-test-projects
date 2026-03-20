package com.laurentiuspilca.ssia.config;

import com.laurentiuspilca.ssia.security.StaticKeyAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterRegistrationConfig {

    @Bean
    FilterRegistrationBean<StaticKeyAuthenticationFilter> staticKeyAuthenticationFilterRegistration(
            StaticKeyAuthenticationFilter filter) {
        FilterRegistrationBean<StaticKeyAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
