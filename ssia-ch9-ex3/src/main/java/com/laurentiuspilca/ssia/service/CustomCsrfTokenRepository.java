package com.laurentiuspilca.ssia.service;

import com.laurentiuspilca.ssia.entities.Token;
import com.laurentiuspilca.ssia.repository.JpaTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class CustomCsrfTokenRepository implements CsrfTokenRepository {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    private final JpaTokenRepository jpaTokenRepository;

    public CustomCsrfTokenRepository(JpaTokenRepository jpaTokenRepository) {
        this.jpaTokenRepository = jpaTokenRepository;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        String uuid = UUID.randomUUID().toString();
        return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", uuid);
    }

    @Override
    public void saveToken(CsrfToken csrfToken, HttpServletRequest request, HttpServletResponse response) {
        String identifier = request.getHeader("X-IDENTIFIER");

        if (identifier == null || identifier.isBlank()) {
            return;
        }

        if (csrfToken == null) {
            jpaTokenRepository.findTokenByIdentifier(identifier)
                    .ifPresent(jpaTokenRepository::delete);
            return;
        }

        Instant expiresAt = Instant.now().plus(TOKEN_TTL);

        Optional<Token> existingToken = jpaTokenRepository.findTokenByIdentifier(identifier);

        if (existingToken.isPresent()) {
            Token token = existingToken.get();
            token.setToken(csrfToken.getToken());
            token.setExpiresAt(expiresAt);
            jpaTokenRepository.save(token);
        } else {
            Token token = new Token();
            token.setIdentifier(identifier);
            token.setToken(csrfToken.getToken());
            token.setExpiresAt(expiresAt);
            jpaTokenRepository.save(token);
        }
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        String identifier = request.getHeader("X-IDENTIFIER");

        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        Optional<Token> existingToken = jpaTokenRepository.findTokenByIdentifier(identifier);

        if (existingToken.isEmpty()) {
            return null;
        }

        Token token = existingToken.get();

        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
            jpaTokenRepository.delete(token);
            return null;
        }

        return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", token.getToken());
    }
}
