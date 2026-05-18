package com.laurentiuspilca.ssia.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("mixed-token")
public class MixedTokenProjectConfig {

    @Value("${app.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.opaque.introspection-uri}")
    private String introspectionUri;

    @Value("${app.opaque.client-id}")
    private String introspectionClientId;

    @Value("${app.opaque.client-secret}")
    private String introspectionClientSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(c -> c
                .authenticationManagerResolver(
                        authenticationManagerResolver(jwtDecoder(), opaqueTokenIntrospector())
                )
        );

        http.authorizeHttpRequests(c -> c.anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver(
            JwtDecoder jwtDecoder,
            OpaqueTokenIntrospector opaqueTokenIntrospector
    ) {
        AuthenticationManager jwtAuth =
                new ProviderManager(new JwtAuthenticationProvider(jwtDecoder));

        AuthenticationManager opaqueAuth =
                new ProviderManager(new OpaqueTokenAuthenticationProvider(opaqueTokenIntrospector));

        return request -> {
            if ("jwt".equals(request.getHeader("type"))) {
                return jwtAuth;
            }

            return opaqueAuth;
        };
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .build();
    }

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector() {
        return new SpringOpaqueTokenIntrospector(
                introspectionUri,
                introspectionClientId,
                introspectionClientSecret
        );
    }
}
