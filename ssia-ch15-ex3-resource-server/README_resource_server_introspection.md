# Spring Security OAuth2 Example: Resource Server Token Introspection

Этот пример демонстрирует связку двух приложений:

- Authorization Server
- Resource Server

Главная цель — показать, как Resource Server проверяет opaque/reference access token через introspection endpoint Authorization Server.

---

# Проекты

## Authorization Server

Проект:

```text
ssia-ch15-ex3-authorization-server
```

Порт:

```text
8080
```

Отвечает за:

- регистрацию OAuth2 clients;
- выдачу opaque/reference access tokens;
- предоставление introspection endpoint;
- проверку токенов через `/oauth2/introspect`.

---

## Resource Server

Проект:

```text
ssia-ch15-ex3-resource-server
```

Порт:

```text
9090
```

Отвечает за:

- защиту endpoint `/demo`;
- получение Bearer token;
- отправку token introspection request;
- разрешение доступа только при active token.

---

# Что демонстрирует пример

В JWT Resource Server токен проверяется локально через public key.

В этом примере используется другой подход:

1. Client получает opaque token от Authorization Server.
2. Client вызывает Resource Server с этим token.
3. Resource Server не может прочитать opaque token самостоятельно.
4. Resource Server отправляет token на `/oauth2/introspect`.
5. Authorization Server отвечает, active token или нет.
6. Если token active, Resource Server разрешает доступ.

---

# Opaque token

Opaque token — это reference token.

Он:

- не содержит readable payload;
- не имеет структуры `header.payload.signature`;
- не может быть проверен локально;
- проверяется через Authorization Server.

---

# Authorization Server configuration

В Authorization Server зарегистрированы два clients.

---

## Основной client

Этот client получает access token:

```java
RegisteredClient registeredClient = RegisteredClient
        .withId(UUID.randomUUID().toString())
        .clientId("client")
        .clientSecret("secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .tokenSettings(TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                .build())
        .scope("CUSTOM")
        .build();
```

Главная настройка:

```java
.accessTokenFormat(OAuth2TokenFormat.REFERENCE)
```

Она говорит Authorization Server выдавать opaque/reference token вместо JWT.

---

## Resource Server как OAuth2 client

Resource Server тоже регистрируется как OAuth2 client:

```java
RegisteredClient resourceServer = RegisteredClient
        .withId(UUID.randomUUID().toString())
        .clientId("resource_server")
        .clientSecret("resource_server_secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .build();
```

Это нужно потому, что Resource Server вызывает introspection endpoint
и должен аутентифицироваться перед Authorization Server.

Credentials Resource Server:

```text
client_id: resource_server
client_secret: resource_server_secret
```

---

# Resource Server application.properties

```properties
server.port=9090
introspectionUri=http://localhost:8080/oauth2/introspect
resourceserver.clientID=resource_server
resourceserver.secret=resource_server_secret
```

---

# Resource Server security configuration

```java
@Configuration
public class ProjectConfig {

    @Value("${introspectionUri}")
    private String introspectionUri;

    @Value("${resourceserver.clientID}")
    private String resourceServerClientID;

    @Value("${resourceserver.secret}")
    private String resourceServerSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(c -> c
                .opaqueToken(
                        o -> o
                                .introspectionUri(introspectionUri)
                                .introspectionClientCredentials(
                                        resourceServerClientID,
                                        resourceServerSecret
                                )
                )
        );
        http.authorizeHttpRequests(c -> c.anyRequest().authenticated());
        return http.build();
    }
}
```

---

# Что делает opaqueToken()

```java
http.oauth2ResourceServer(c -> c.opaqueToken(...))
```

Эта настройка говорит Spring Security,
что Resource Server должен проверять opaque tokens через introspection.

---

# Что делает introspectionUri()

```java
.introspectionUri(introspectionUri)
```

Указывает endpoint Authorization Server,
куда Resource Server отправляет token для проверки.

---

# Что делает introspectionClientCredentials()

```java
.introspectionClientCredentials(resourceServerClientID, resourceServerSecret)
```

Указывает credentials Resource Server,
с которыми он аутентифицируется перед Authorization Server.

---

# Demo endpoint

```java
@RestController
public class DemoController {

    @GetMapping("/demo")
    public String demo() {
        return "Demo";
    }
}
```

Endpoint:

```text
http://localhost:9090/demo
```

Доступен только с valid Bearer token.

---

# Проверка работы

Нужно запустить два приложения.

---

## 1. Запустить Authorization Server

Проект:

```text
ssia-ch15-ex3-authorization-server
```

Запустить:

```text
Main.java
```

или:

```bash
mvn spring-boot:run
```

Authorization Server должен работать на:

```text
http://localhost:8080
```

---

## 2. Запустить Resource Server

Проект:

```text
ssia-ch15-ex3-resource-server
```

Запустить:

```text
Main.java
```

или:

```bash
mvn spring-boot:run
```

Resource Server должен работать на:

```text
http://localhost:9090
```

---

# Получение opaque access token

Выполнить token request к Authorization Server.

Важно: параметры нужно отправлять как `application/x-www-form-urlencoded` в POST body.

```bash
curl -i -X POST "http://localhost:8080/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=CUSTOM"
```

---

# Почему Authorization header такой

Для основного client:

```text
client_id: client
client_secret: secret
```

Строка для Basic Authentication:

```text
client:secret
```

Base64:

```text
Y2xpZW50OnNlY3JldA==
```

Header:

```text
Authorization: Basic Y2xpZW50OnNlY3JldA==
```

---

# Пример успешного token response

```json
{
  "access_token": "2zLyYA8b6Q54...",
  "scope": "CUSTOM",
  "token_type": "Bearer",
  "expires_in": 300
}
```

Если access token не похож на JWT и не имеет структуры:

```text
header.payload.signature
```

значит Authorization Server выдал opaque/reference token.

---

# Вызов Resource Server

Скопировать `access_token` из ответа Authorization Server.

Затем выполнить:

```bash
curl -i "http://localhost:9090/demo" \
-H "Authorization: Bearer ACCESS_TOKEN"
```

---

# Пример успешного ответа

```text
HTTP/1.1 200

Demo
```

Это означает:

1. Resource Server получил Bearer token.
2. Resource Server отправил token на `/oauth2/introspect`.
3. Authorization Server подтвердил, что token active.
4. Resource Server разрешил доступ к `/demo`.

---

# Что будет без token

```bash
curl -i "http://localhost:9090/demo"
```

Ответ:

```text
HTTP/1.1 401
```

---

# Что будет с неправильным или истекшим token

Если token:

- неправильный;
- истек;
- был отозван;
- не найден Authorization Server;

Resource Server вернет:

```text
HTTP/1.1 401
```

---

# Ручная проверка introspection

Можно вручную проверить token через introspection endpoint.

Для этого используется client `resource_server`.

Строка для Basic Authentication:

```text
resource_server:resource_server_secret
```

Base64:

```text
cmVzb3VyY2Vfc2VydmVyOnJlc291cmNlX3NlcnZlcl9zZWNyZXQ=
```

Пример:

```bash
curl -i -X POST "http://localhost:8080/oauth2/introspect" \
-H "Authorization: Basic cmVzb3VyY2Vfc2VydmVyOnJlc291cmNlX3NlcnZlcl9zZWNyZXQ=" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "token=ACCESS_TOKEN"
```

---

# Пример active introspection response

```json
{
  "active": true,
  "sub": "client",
  "aud": [
    "client"
  ],
  "scope": "CUSTOM",
  "iss": "http://localhost:8080",
  "client_id": "client",
  "token_type": "Bearer"
}
```

---

# Пример inactive introspection response

```json
{
  "active": false
}
```

---

# Важное отличие от JWT Resource Server

## JWT Resource Server

Resource Server проверяет token локально через public key:

```text
/oauth2/jwks
```

## Opaque Token Resource Server

Resource Server проверяет token удаленно через introspection:

```text
/oauth2/introspect
```

---

# Плюсы introspection

- Authorization Server остается источником истины.
- Можно проверять active status токена.
- Удобнее работать с token revocation.
- Opaque token ничего не раскрывает клиенту.

---

# Минусы introspection

- Каждый protected request может требовать сетевой вызов.
- Resource Server зависит от доступности Authorization Server.
- Может увеличиться latency.
- Может увеличиться нагрузка на Authorization Server.

---

# Когда использовать introspection

Introspection особенно полезна, если:

- используются opaque tokens;
- нужно поддерживать token revocation;
- важно централизованно контролировать active status;
- нельзя раскрывать данные в самом token.

---

# Flow summary

## 1. Запустить Authorization Server

```text
ssia-ch15-ex3-authorization-server
```

---

## 2. Запустить Resource Server

```text
ssia-ch15-ex3-resource-server
```

---

## 3. Получить opaque token

```bash
curl -i -X POST "http://localhost:8080/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=CUSTOM"
```

---

## 4. Вызвать Resource Server

```bash
curl -i "http://localhost:9090/demo" \
-H "Authorization: Bearer ACCESS_TOKEN"
```

---

## 5. Получить ответ

```text
Demo
```
