# Spring Security OAuth2 Resource Server: Multi-Tenant Authentication Manager Resolver

Этот пример относится к главе 15.4 и показывает, как Resource Server может работать не с одним фиксированным Authorization Server, а с несколькими вариантами проверки access token.

В главе рассматриваются два сценария:

1. Resource Server принимает JWT tokens от нескольких Authorization Server.
2. Resource Server принимает и JWT tokens, и opaque tokens, выбирая способ проверки по HTTP header `type`.

---

# Проекты

## Authorization Server

Проект:

```text
ssia-ch15-ex4-authorization-server
```

Это универсальный Authorization Server. Один и тот же проект можно запускать несколько раз одновременно с разными параметрами:

- на разных портах;
- с разным issuer;
- с JWT token format;
- с opaque/reference token format.

---

## Resource Server

Проект:

```text
ssia-ch15-ex4-resource-server
```

Порт:

```text
9090
```

В нем есть два режима работы:

```text
multi-jwt
mixed-token
```

Режим выбирается через Spring profile.

---

# Главная идея главы

Обычно Resource Server настраивается одним способом:

```java
http.oauth2ResourceServer(c -> c.jwt(...));
```

или:

```java
http.oauth2ResourceServer(c -> c.opaqueToken(...));
```

Но в multi-tenant системах Resource Server может принимать tokens от разных Authorization Server или даже разные типы tokens.

Для таких случаев Spring Security позволяет использовать:

```java
AuthenticationManagerResolver<HttpServletRequest>
```

Он выбирает нужный `AuthenticationManager` во время обработки HTTP request.

---

# Сценарий 1: несколько JWT Authorization Server

## Идея

Есть два Authorization Server:

```text
http://localhost:7070
http://localhost:8080
```

Оба выдают JWT access tokens.

Resource Server принимает JWT tokens от обоих.

---

# Как Resource Server понимает, откуда token

JWT содержит claim:

```text
iss
```

`iss` = issuer.

Например:

```json
{
  "iss": "http://localhost:7070"
}
```

или:

```json
{
  "iss": "http://localhost:8080"
}
```

Resource Server смотрит на `iss` и выбирает правильный AuthenticationManager.

---

# Готовый resolver Spring Security

Для этого сценария Spring Security предоставляет:

```java
JwtIssuerAuthenticationManagerResolver
```

Пример:

```java
@Bean
public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
    return JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(
            issuer1,
            issuer2
    );
}
```

Он:

1. читает `iss` из JWT;
2. проверяет, что issuer trusted;
3. выбирает нужный AuthenticationManager;
4. проверяет JWT через JWK Set соответствующего issuer.

---

# Resource Server config для multi-jwt

```java
@Configuration
@Profile("multi-jwt")
public class MultiJwtProjectConfig {

    @Value("${app.jwt.issuer-1}")
    private String issuer1;

    @Value("${app.jwt.issuer-2}")
    private String issuer2;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(c -> c
                .authenticationManagerResolver(authenticationManagerResolver())
        );

        http.authorizeHttpRequests(c -> c.anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
        return JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(issuer1, issuer2);
    }
}
```

---

## Использование Maven без отдельной установки

Чтобы команды `mvn` работали в Git Bash без отдельной установки Maven в систему, можно создать alias для встроенного Maven из IntelliJ IDEA.

Выполните команды:

```bash
echo "alias mvn='/c/Users/<USERNAME>/AppData/Roaming/JetBrains/<INTELLIJ_VERSION>/plugins/maven/lib/maven3/bin/mvn.cmd'" >> ~/.bashrc
source ~/.bashrc
```

Где:

- `<USERNAME>` — имя пользователя Windows
- `<INTELLIJ_VERSION>` — версия IntelliJ IDEA (например, `IntelliJIdea2026.1`)

После этого команда `mvn` станет доступна в Git Bash.

---

# Сценарий 1: запуск

Нужно одновременно запустить:

```text
JWT Authorization Server #1 -> 7070
JWT Authorization Server #2 -> 8080
Resource Server           -> 9090
```

## 1. JWT Authorization Server на 7070

В папке `ssia-ch15-ex4-authorization-server`:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=7070 --app.issuer=http://localhost:7070 --app.token-format=jwt"
```

## 2. JWT Authorization Server на 8080

Во втором терминале:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080 --app.issuer=http://localhost:8080 --app.token-format=jwt"
```

## 3. Resource Server в режиме multi-jwt

В папке `ssia-ch15-ex4-resource-server`:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=multi-jwt"
```

---

# Сценарий 1: получение token

## Token от issuer 7070

```bash
curl -i -X POST "http://localhost:7070/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=CUSTOM"
```

## Token от issuer 8080

```bash
curl -i -X POST "http://localhost:8080/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=CUSTOM"
```

---

# Сценарий 1: вызов Resource Server

```bash
curl -i "http://localhost:9090/demo" \
-H "Authorization: Bearer JWT_ACCESS_TOKEN"
```

Для `multi-jwt` сценария header `type` не нужен.

Resource Server сам выбирает issuer по claim `iss`.

Ожидаемый ответ:

```text
HTTP/1.1 200

Demo
```

---

# Сценарий 2: JWT + opaque token

## Идея

Есть два Authorization Server:

```text
http://localhost:7070 -> выдает JWT tokens
http://localhost:6060 -> выдает opaque tokens
```

Resource Server принимает оба типа tokens.

JWT и opaque token проверяются по-разному:

- JWT проверяется через JWK Set;
- opaque token проверяется через introspection endpoint.

---

# Зачем нужен custom resolver

В этом сценарии Resource Server должен сам решить, как проверять token.

Для учебного примера критерий выбора — HTTP header:

```http
type: jwt
```

Если header равен `jwt`, Resource Server использует JWT validation.

Если header отсутствует или имеет другое значение, Resource Server использует opaque token introspection.

---

# Resource Server config для mixed-token

```java
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
```

---

# Сценарий 2: запуск

Нужно одновременно запустить:

```text
JWT Authorization Server    -> 7070
Opaque Authorization Server -> 6060
Resource Server             -> 9090
```

## 1. JWT Authorization Server на 7070

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=7070 --app.issuer=http://localhost:7070 --app.token-format=jwt"
```

## 2. Opaque Authorization Server на 6060

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=6060 --app.issuer=http://localhost:6060 --app.token-format=opaque"
```

## 3. Resource Server в режиме mixed-token

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=mixed-token"
```

---

# Сценарий 2: получение JWT token

JWT token получаем с Authorization Server на 7070:

```bash
curl -i -X POST "http://localhost:7070/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=CUSTOM"
```

JWT обычно имеет структуру:

```text
header.payload.signature
```

---

# Сценарий 2: вызов Resource Server с JWT token

Для JWT обязательно добавить header:

```http
type: jwt
```

```bash
curl -i "http://localhost:9090/demo" \
-H "Authorization: Bearer JWT_ACCESS_TOKEN" \
-H "type: jwt"
```

Если забыть `type: jwt`, Resource Server попробует проверить JWT как opaque token.

---

# Сценарий 2: получение opaque token

Opaque token получаем с Authorization Server на 6060:

```bash
curl -i -X POST "http://localhost:6060/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=CUSTOM"
```

Opaque token не имеет структуры:

```text
header.payload.signature
```

---

# Сценарий 2: вызов Resource Server с opaque token

Header `type: jwt` не добавляем:

```bash
curl -i "http://localhost:9090/demo" \
-H "Authorization: Bearer OPAQUE_ACCESS_TOKEN"
```

Можно явно указать другой type:

```bash
curl -i "http://localhost:9090/demo" \
-H "Authorization: Bearer OPAQUE_ACCESS_TOKEN" \
-H "type: opaque"
```

В текущей логике всё, что не равно `jwt`, идет в opaque branch.

Ожидаемый ответ:

```text
HTTP/1.1 200

Demo
```

---

# Authorization Server: token format

Authorization Server использует property:

```properties
app.token-format=jwt
```

или:

```properties
app.token-format=opaque
```

В коде это превращается в:

```java
OAuth2TokenFormat accessTokenFormat =
        "opaque".equalsIgnoreCase(tokenFormat)
                ? OAuth2TokenFormat.REFERENCE
                : OAuth2TokenFormat.SELF_CONTAINED;
```

---

# SELF_CONTAINED vs REFERENCE

## JWT

```java
OAuth2TokenFormat.SELF_CONTAINED
```

означает JWT access token.

## Opaque token

```java
OAuth2TokenFormat.REFERENCE
```

означает opaque/reference access token.

---

# Issuer

Каждый Authorization Server должен иметь свой issuer:

```text
http://localhost:7070
http://localhost:8080
http://localhost:6060
```

В Authorization Server:

```java
@Bean
public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder()
            .issuer(issuer)
            .build();
}
```

Issuer особенно важен для JWT сценария,
потому что Resource Server выбирает trusted issuer по claim `iss`.

---

# application.properties Resource Server

```properties
server.port=9090

spring.profiles.active=multi-jwt

app.jwt.issuer-1=http://localhost:7070
app.jwt.issuer-2=http://localhost:8080

app.jwt.jwk-set-uri=http://localhost:7070/oauth2/jwks

app.opaque.introspection-uri=http://localhost:6060/oauth2/introspect
app.opaque.client-id=resource_server
app.opaque.client-secret=resource_server_secret
```

Для сценария 1:

```properties
spring.profiles.active=multi-jwt
```

Для сценария 2:

```properties
spring.profiles.active=mixed-token
```

---

# Итог

Глава 15.4 показывает два подхода:

## 1. Готовый resolver

```java
JwtIssuerAuthenticationManagerResolver
```

Используется, когда несколько Authorization Server выдают JWT.

## 2. Custom resolver

```java
AuthenticationManagerResolver<HttpServletRequest>
```

Используется, когда логика выбора AuthenticationManager нестандартная.

В учебном примере:

```text
type: jwt -> JWT authentication
otherwise -> opaque token introspection
```
