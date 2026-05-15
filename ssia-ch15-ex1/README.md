# Spring OAuth2 Resource Server Example (JWT Validation)

Этот проект демонстрирует настройку OAuth2 Resource Server,
который проверяет JWT access tokens, выданные Spring Authorization Server.

Проект `ssia-ch15-ex1` работает вместе с проектом:

```text
ssia-ch14-ex1
```

---

# Что демонстрирует проект

Проект показывает:

- как настроить OAuth2 Resource Server;
- как защитить REST endpoint;
- как проверять JWT access token;
- как использовать JWK Set endpoint Authorization Server;
- как отправлять Bearer token в запросе к Resource Server.

---

# Общая схема

В примере используются два приложения.

## Authorization Server

Проект:

```text
ssia-ch14-ex1
```

Порт:

```text
8080
```

Он отвечает за:

- login пользователя;
- выдачу authorization code;
- выдачу JWT access token;
- публикацию public keys через JWK Set endpoint.

---

## Resource Server

Проект:

```text
ssia-ch15-ex1
```

Порт:

```text
9090
```

Он отвечает за:

- защиту endpoint'ов;
- проверку JWT access token;
- разрешение доступа только authenticated requests.

---

# Почему нужны два порта

Authorization Server и Resource Server запускаются одновременно.

Поэтому:

```text
Authorization Server -> http://localhost:8080
Resource Server      -> http://localhost:9090
```

---

# Demo endpoint

В проекте есть тестовый endpoint:

```java
@RestController
public class DemoController {

    @GetMapping("/demo")
    public String demo() {
        return "Demo";
    }
}
```

Endpoint доступен по адресу:

```text
http://localhost:9090/demo
```

Но доступ к нему разрешен только с valid JWT access token.

---

# application.properties

```properties
server.port=9090
keySetURI=http://localhost:8080/oauth2/jwks
```

---

# Что такое keySetURI

```properties
keySetURI=http://localhost:8080/oauth2/jwks
```

Это URL, по которому Resource Server получает public keys
от Authorization Server.

Эти public keys нужны Resource Server'у,
чтобы проверить подпись JWT access token.

---

# Что такое JWK Set

JWK = JSON Web Key

JWK Set endpoint:

```text
http://localhost:8080/oauth2/jwks
```

возвращает набор public keys.

Resource Server использует эти ключи, чтобы убедиться:

- token действительно выдан Authorization Server;
- token не был подделан;
- JWT signature валидна.

---

# Security configuration

```java
@Configuration
public class ProjectConfig {

    @Value("${keySetURI}")
    private String keySetURI;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(c -> c.jwt(j -> j.jwkSetUri(keySetURI)));
        http.authorizeHttpRequests(c -> c.anyRequest().authenticated());
        return http.build();
    }
}
```

---

# Что делает oauth2ResourceServer()

```java
http.oauth2ResourceServer(...)
```

Включает режим OAuth2 Resource Server.

---

# Что делает jwt()

```java
c.jwt(...)
```

Говорит Spring Security,
что Resource Server будет принимать JWT tokens.

---

# Что делает jwkSetUri()

```java
j.jwkSetUri(keySetURI)
```

Указывает, где взять public keys
для проверки JWT signature.

---

# Что делает authenticated()

```java
c.anyRequest().authenticated()
```

Требует authentication для всех HTTP requests.

Если запрос пришел без valid Bearer token,
Resource Server вернет:

```text
401 Unauthorized
```

---

# Как работает проверка JWT

JWT access token состоит из трех частей:

```text
header.payload.signature
```

Resource Server:

1. получает token из header `Authorization`;
2. читает JWT header;
3. определяет key id;
4. получает public key из JWK Set;
5. проверяет signature;
6. проверяет срок жизни token;
7. создает Authentication object;
8. разрешает или запрещает доступ.

---

# Что такое Bearer token

Bearer token отправляется так:

```text
Authorization: Bearer ACCESS_TOKEN
```

`Bearer` означает:

```text
кто владеет токеном — тот может его использовать
```

Поэтому access token нужно хранить безопасно.

---

# Запуск проекта

Для проверки нужно запустить два приложения.

---

## 1. Запустить Authorization Server

Запустить проект:

```text
ssia-ch14-ex1
```

Через IDE:

```text
Main.java
```

или через Maven:

```bash
mvn spring-boot:run
```

Authorization Server должен работать на:

```text
http://localhost:8080
```

---

## 2. Запустить Resource Server

Запустить проект:

```text
ssia-ch15-ex1
```

Через IDE:

```text
Main.java
```

или через Maven:

```bash
mvn spring-boot:run
```

Resource Server должен работать на:

```text
http://localhost:9090
```

---

# Проверка Authorization Server

Открыть:

```text
http://localhost:8080/.well-known/openid-configuration
```

В ответе должен быть параметр:

```json
{
  "jwks_uri": "http://localhost:8080/oauth2/jwks"
}
```

Именно этот URL используется Resource Server'ом для проверки JWT.

---

# Получение JWT access token

JWT access token нужно получить через Authorization Server `ssia-ch14-ex1`.

В проекте `ssia-ch14-ex1` есть тест:

```text
src/test/java/com/laurentiuspilca/ssia/PkceGeneratorTest.java
```

Он генерирует:

- CODE VERIFIER
- CODE CHALLENGE
- authorization URL

---

# Краткий flow получения token

## 1. Запустить PkceGeneratorTest

Скопировать `AUTHORIZATION URL`.

---

## 2. Открыть authorization URL в браузере

---

## 3. Выполнить login

```text
username: bill
password: password
```

---

## 4. Скопировать authorization code

После login будет redirect:

```text
https://www.manning.com/authorized?code=AUTHORIZATION_CODE
```

Скопировать значение параметра:

```text
code
```

---

## 5. Выполнить token request

```bash
curl -i -X POST "http://localhost:8080/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "client_id=client" \
-d "redirect_uri=https://www.manning.com/authorized" \
-d "grant_type=authorization_code" \
-d "code=AUTHORIZATION_CODE" \
-d "code_verifier=CODE_VERIFIER"
```

---

# Пример успешного ответа от Authorization Server

```json
{
  "access_token": "eyJraWQiOiI4ODlhNGFmOS1...",
  "scope": "openid",
  "id_token": "eyJraWQiOiI4ODlhNGFmOS1...",
  "token_type": "Bearer",
  "expires_in": 300
}
```

Для вызова Resource Server нужен именно:

```text
access_token
```

---

# Вызов Resource Server

После получения `access_token` выполнить:

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

---

# Что будет без token

Если вызвать endpoint без token:

```bash
curl -i "http://localhost:9090/demo"
```

Resource Server вернет:

```text
HTTP/1.1 401
```

---

# Что будет с неправильным token

Если token:

- поврежден;
- истек;
- подписан другим Authorization Server;
- не может быть проверен через JWK Set;

Resource Server также вернет:

```text
HTTP/1.1 401
```

---

# Почему Resource Server доверяет Authorization Server

Resource Server не логинит пользователя сам.

Он доверяет JWT access token,
который был выдан Authorization Server.

Доверие основано на проверке подписи:

- Authorization Server подписывает JWT private key;
- Resource Server получает public key через JWK Set endpoint;
- Resource Server проверяет signature.

---

# JWT vs Opaque Token

## JWT

Плюсы:

- self-contained;
- можно проверить локально;
- меньше network calls;
- быстрее для Resource Server.

Минусы:

- claims видны клиенту;
- сложнее немедленно отозвать token.

---

## Opaque Token

Плюсы:

- token ничего не раскрывает сам по себе;
- проще централизованно проверять active status;
- удобнее для revocation.

Минусы:

- нужен introspection request;
- больше сетевых вызовов;
- Resource Server сильнее зависит от Authorization Server.

