# Spring Security Reactive OAuth2 Resource Server

Этот README относится к проекту:

```text
ssia-ch17-ex6
```

Проект демонстрирует создание реактивного OAuth2 Resource Server
с использованием:

- Spring WebFlux
- Spring Security
- JWT
- OAuth2 Resource Server
- Reactive Security

---

# Главная идея

В этой главе создается:

```text
Reactive OAuth2 Resource Server
```

который:

1. принимает JWT access token;
2. проверяет подпись токена;
3. аутентифицирует запрос;
4. разрешает доступ к endpoint'ам только при валидном токене.

---

# Reactive Resource Server

В отличие от servlet-based resource server:

- используется WebFlux;
- используется Netty;
- используется SecurityWebFilterChain;
- используется ServerHttpSecurity.

---

# ProjectConfig

```java
@Configuration
public class ProjectConfig {

    @Value("${jwk.endpoint}")
    private String jwkEndpoint;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {

        http.oauth2ResourceServer(
                c -> c.jwt(
                        j -> j.jwkSetUri(jwkEndpoint)
                )
        );

        http.authorizeExchange(
                c -> c.anyExchange().authenticated()
        );

        return http.build();
    }
}
```

---

# Что делает oauth2ResourceServer()

```java
http.oauth2ResourceServer(...)
```

включает OAuth2 Resource Server functionality.

Spring Security начинает:

- искать Bearer token;
- валидировать JWT;
- проверять подпись;
- создавать Authentication object.

---

# Что делает jwt()

```java
.jwt(...)
```

говорит Spring Security:

```text
использовать JWT access tokens
```

---

# Что делает jwkSetUri()

```java
j.jwkSetUri(jwkEndpoint)
```

указывает endpoint, откуда Resource Server
получает public keys для проверки JWT signature.

---

# application.properties

## Вариант из книги (Keycloak)

```properties
server.port=9090
jwk.endpoint=http://localhost:8080/auth/realms/master/protocol/openid-connect/certs
```

---

## Вариант с собственным Authorization Server

Если использовать Authorization Server из предыдущих глав:

```text
ssia-ch16-ex2-authorization-server
```

то configuration будет такой:

```properties
server.port=9090
jwk.endpoint=http://localhost:7070/oauth2/jwks
```

---

# Как запускать приложение

## Вариант 1 — Keycloak

Сначала запускается Keycloak.

После этого запускается:

```text
ssia-ch17-ex6
```

---

## Вариант 2 — собственный Authorization Server

Можно использовать проект:

```text
ssia-ch16-ex2-authorization-server
```

из предыдущих глав.

Сначала запускается:

```text
ssia-ch16-ex2-authorization-server
```

на порту:

```text
7070
```

После этого запускается:

```text
ssia-ch17-ex6
```

на порту:

```text
9090
```

---

# Как получить access token

Если используется:

```text
ssia-ch16-ex2-authorization-server
```

то можно получить token так:

```bash
curl -i -X POST "http://localhost:7070/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=openid"
```

---

# Что означает Authorization header

```text
Authorization: Basic Y2xpZW50OnNlY3JldA==
```

Это:

```text
Base64(client:secret)
```

---

# Пример ответа

```json
{
  "access_token": "eyJraWQiOi...",
  "token_type": "Bearer",
  "expires_in": 299
}
```

---

# Как вызвать Resource Server

После получения token:

```bash
curl -H "Authorization: Bearer TOKEN_HERE" \
http://localhost:9090/hello
```

---

# Пример

```bash
curl -H "Authorization: Bearer eyJraWQiOi..." \
http://localhost:9090/hello
```

---

# Ожидаемый ответ

```text
Hello!
```

---

# Что будет без token

```bash
curl -i http://localhost:9090/hello
```

Ответ:

```text
HTTP/1.1 401 Unauthorized
```

---

# Что происходит за кадром

## 1. Client отправляет Bearer token

```text
Authorization: Bearer JWT
```

---

## 2. Resource Server перехватывает запрос

Reactive Spring Security filter chain:

```text
SecurityWebFilterChain
```

перехватывает request.

---

## 3. JWT проверяется

Spring Security:

- загружает public key;
- проверяет signature;
- проверяет expiration;
- проверяет claims.

---

## 4. Создается Authentication

Если token валиден:

```text
Authentication
```

сохраняется в reactive security context.

---

## 5. Endpoint получает доступ

После успешной authentication endpoint выполняется.

---

# Reactive Security Stack

В reactive OAuth2 Resource Server используются:

- SecurityWebFilterChain
- ServerHttpSecurity
- Reactive Security Context
- JWT Decoder
- OAuth2 Resource Server
- WebFlux
- Reactor

---

# Servlet vs Reactive Resource Server

## Servlet

```java
SecurityFilterChain
HttpSecurity
```

---

## Reactive

```java
SecurityWebFilterChain
ServerHttpSecurity
```

---

# Итог

Проект `ssia-ch17-ex6` демонстрирует:

- reactive OAuth2 Resource Server;
- JWT authentication;
- JWT signature validation;
- integration with Authorization Server;
- reactive Spring Security;
- WebFlux security;
- Bearer token authentication.
