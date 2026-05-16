# Spring Security OAuth2 Example: Custom JWT Claims

Этот пример демонстрирует работу двух приложений:

- Authorization Server
- Resource Server

Главная цель примера — показать, как добавить custom claim в JWT access token
на стороне Authorization Server и затем прочитать этот claim на стороне Resource Server.

В примере используется claim:

```text
priority = HIGH
```

---

# Проекты

## Authorization Server

Проект:

```text
ssia-ch15-ex2-authorization-server
```

Порт:

```text
8080
```

Отвечает за:

- login пользователя;
- Authorization Code Flow + PKCE;
- выдачу JWT access token;
- добавление custom claim `priority`;
- публикацию JWK Set endpoint.

---

## Resource Server

Проект:

```text
ssia-ch15-ex2-resource-server
```

Порт:

```text
9090
```

Отвечает за:

- защиту endpoint `/demo`;
- проверку JWT access token;
- чтение custom claim `priority`;
- создание custom Authentication object.

---

# Что демонстрирует пример

Обычный Resource Server умеет проверить JWT и создать стандартный authentication object.

В этом примере мы делаем больше:

1. Authorization Server добавляет custom claim в JWT:

```json
{
  "priority": "HIGH"
}
```

2. Resource Server читает claim `priority`.

3. Resource Server создает custom authentication object:

```java
CustomAuthentication
```

4. Endpoint `/demo` возвращает authentication object.

5. В ответе видно поле:

```json
"priority": "HIGH"
```

---

# Authorization Server: custom claim

В проекте `ssia-ch15-ex2-authorization-server` custom claim добавляется через bean:

```java
@Bean
public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
    return context -> {
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            JwtClaimsSet.Builder claims = context.getClaims();
            claims.claim("priority", "HIGH");
        }
    };
}
```

Этот bean позволяет изменить claims перед тем,
как Authorization Server подпишет и выдаст JWT.

---

# Authorization Server: user

В проекте настроен in-memory user:

```text
username: bill
password: password
```

---

# Authorization Server: OAuth2 client

В проекте зарегистрирован OAuth2 client:

```text
client_id: client
client_secret: secret
grant_type: authorization_code
scope: openid
redirect_uri: https://www.manning.com/authorized
```

---

# Resource Server: application.properties

Resource Server работает на порту `9090`:

```properties
server.port=9090
keySetURI=http://localhost:8080/oauth2/jwks
```

`keySetURI` указывает на JWK Set endpoint Authorization Server.

Resource Server использует этот URL,
чтобы получить public keys и проверить подпись JWT access token.

---

# Resource Server: security configuration

```java
@Configuration
public class ProjectConfig {

    @Value("${keySetURI}")
    private String keySetURI;

    private final CustomJwtAuthenticationConverter converter;

    public ProjectConfig(CustomJwtAuthenticationConverter converter) {
        this.converter = converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(c -> c
                .jwt(j -> j
                        .jwkSetUri(keySetURI)
                        .jwtAuthenticationConverter(converter)
                )
        );
        http.authorizeHttpRequests(c -> c.anyRequest().authenticated());
        return http.build();
    }
}
```

Главная строка здесь:

```java
.jwtAuthenticationConverter(converter)
```

Она говорит Spring Security использовать custom converter,
который преобразует `Jwt` в `CustomAuthentication`.

---

# Resource Server: CustomAuthentication

```java
public class CustomAuthentication extends JwtAuthenticationToken {

    private final String priority;

    public CustomAuthentication(
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities,
            String priority) {

        super(jwt, authorities);
        this.priority = priority;
    }

    public String getPriority() {
        return priority;
    }
}
```

`CustomAuthentication` расширяет стандартный `JwtAuthenticationToken`
и добавляет поле:

```text
priority
```

Это поле потом можно использовать в authorization rules.

---

# Resource Server: CustomJwtAuthenticationConverter

```java
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, CustomAuthentication> {

    @Override
    public CustomAuthentication convert(Jwt source) {
        List<GrantedAuthority> authorities = List.of(() -> "read");
        String priority = String.valueOf(source.getClaims().get("priority"));
        return new CustomAuthentication(source, authorities, priority);
    }
}
```

Converter делает следующее:

1. получает стандартный `Jwt`;
2. читает claim `priority`;
3. создает authorities;
4. возвращает `CustomAuthentication`.

В этом учебном примере authority задается статически:

```text
read
```

В реальном приложении authorities обычно берутся:

- из JWT claims;
- из базы данных;
- из внешней authorization system;
- из business rules.

---

# Resource Server: DemoController

```java
@RestController
public class DemoController {

    @GetMapping("/demo")
    public Authentication demo(Authentication authentication) {
        return authentication;
    }
}
```

Endpoint возвращает объект `Authentication`,
который Spring Security положил в SecurityContext.

Если все работает правильно,
в ответе будет видно поле:

```json
"priority": "HIGH"
```

---

# Проверка работы

Нужно запустить два приложения.

---

## 1. Запустить Authorization Server

Проект:

```text
ssia-ch15-ex2-authorization-server
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
ssia-ch15-ex2-resource-server
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

# Получение JWT access token

В Authorization Server проекте есть тест:

```text
src/test/java/com/laurentiuspilca/ssia/PkceGeneratorTest.java
```

Запустить тест.

Он выведет:

- CODE VERIFIER
- CODE CHALLENGE
- AUTHORIZATION URL

---

# 1. Открыть Authorization URL

Скопировать `AUTHORIZATION URL` из output теста и открыть его в браузере.

---

# 2. Выполнить login

Ввести:

```text
username: bill
password: password
```

---

# 3. Получить authorization code

После login произойдет redirect:

```text
https://www.manning.com/authorized?code=AUTHORIZATION_CODE
```

Страница Manning может показать `404`.
Это нормально для учебного примера.

Нужно скопировать значение параметра:

```text
code
```

---

# 4. Выполнить token request

Подставить:

- `AUTHORIZATION_CODE`
- `CODE_VERIFIER`

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

# Пример успешного token response

```json
{
  "access_token": "eyJraWQiOiI5ZTBjOTQ5Ny0...",
  "scope": "openid",
  "id_token": "eyJraWQiOiI5ZTBjOTQ5Ny0...",
  "token_type": "Bearer",
  "expires_in": 300
}
```

Для вызова Resource Server нужен именно:

```text
access_token
```

---

# Проверка custom claim в JWT

Можно открыть access token на:

```text
https://jwt.io
```

В payload должен быть claim:

```json
{
  "priority": "HIGH"
}
```

---

# 5. Вызвать Resource Server

```bash
curl -i "http://localhost:9090/demo" \
-H "Authorization: Bearer ACCESS_TOKEN"
```

---

# Пример успешного ответа

Ответ будет большим JSON object,
потому что endpoint возвращает весь authentication object.

В нем должны быть поля примерно такого вида:

```json
{
  "authorities": [
    {
      "authority": "read"
    }
  ],
  "authenticated": true,
  "name": "bill",
  "priority": "HIGH"
}
```

Главное проверить:

```json
"priority": "HIGH"
```

Это означает, что:

1. Authorization Server добавил custom claim в JWT.
2. Resource Server прочитал JWT.
3. Custom converter извлек claim `priority`.
4. Resource Server создал `CustomAuthentication`.
5. Endpoint `/demo` вернул authentication object с custom field.

---

# Что будет без token

Если вызвать endpoint без Bearer token:

```bash
curl -i "http://localhost:9090/demo"
```

Resource Server вернет:

```text
HTTP/1.1 401
```

---

# Что будет если priority отсутствует

В текущей реализации:

```java
String priority = String.valueOf(source.getClaims().get("priority"));
```

Если claim отсутствует,
значение будет строкой:

```text
null
```

Для production-кода лучше обработать это явно:

```java
String priority = source.getClaimAsString("priority");

if (priority == null) {
    priority = "LOW";
}
```

---

# Flow summary

## 1. Запустить Authorization Server

```text
ssia-ch15-ex2-authorization-server
```

---

## 2. Запустить Resource Server

```text
ssia-ch15-ex2-resource-server
```

---

## 3. Запустить PkceGeneratorTest

---

## 4. Открыть Authorization URL

---

## 5. Залогиниться

```text
bill / password
```

---

## 6. Скопировать authorization code

---

## 7. Получить access token

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

## 8. Вызвать `/demo`

```bash
curl -i "http://localhost:9090/demo" \
-H "Authorization: Bearer ACCESS_TOKEN"
```

---

## 9. Проверить результат

В ответе должно быть:

```json
"priority": "HIGH"
```
