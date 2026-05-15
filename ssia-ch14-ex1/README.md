# Spring Authorization Server Example (Authorization Code + PKCE)

Этот проект демонстрирует работу:

- Spring Authorization Server
- OAuth2 Authorization Code Flow
- PKCE (Proof Key for Code Exchange)
- OpenID Connect (OIDC)

---

# Что реализовано

В проекте настроен OAuth2 Authorization Server со следующими возможностями:

- login form
- authorization endpoint
- token endpoint
- OpenID Connect support
- PKCE support
- in-memory user
- in-memory OAuth2 client

---

# Данные пользователя

В системе зарегистрирован пользователь:

```text
username: bill
password: password
```

---

# OAuth2 client

В системе зарегистрирован OAuth2 client:

```text
client_id: client
client_secret: secret
```

redirect URI:

```text
https://www.manning.com/authorized
```

grant type:

```text
authorization_code
```

scope:

```text
openid
```

---

# Запуск приложения

Запустить:

```bash
mvn spring-boot:run
```

или через IDE:

```text
Main.java
```

После запуска приложение будет доступно на:

```text
http://localhost:8080
```

---

# Проверка OpenID configuration

Открыть в браузере:

```text
http://localhost:8080/.well-known/openid-configuration
```

Должен вернуться JSON с endpoint'ами authorization server.

---

# Генерация PKCE значений

В проекте есть тестовый класс:

```text
src/test/java/com/laurentiuspilca/ssia/PkceGeneratorTest.java
```

Запустить тест:

```text
PkceGeneratorTest
```

Тест сгенерирует:

- CODE VERIFIER
- CODE CHALLENGE
- готовый authorization URL

---

# Authorization Request

Скопировать `AUTHORIZATION URL` из output теста и открыть в браузере.

Пример:

```text
http://localhost:8080/oauth2/authorize?response_type=code&client_id=client&scope=openid&redirect_uri=https://www.manning.com/authorized&code_challenge=CODE_CHALLENGE&code_challenge_method=S256
```

Где `CODE_CHALLENGE` берется из output теста.

---

# Login

После открытия authorization URL Spring Security покажет login form.

Ввести:

```text
username: bill
password: password
```

После успешного login произойдет redirect:

```text
https://www.manning.com/authorized?code=...
```

Страница Manning может показать `404`. Это нормально для этого учебного примера. Главное — скопировать значение параметра `code` из адресной строки браузера.

---

# Получение authorization code

Из redirect URL нужно скопировать значение параметра:

```text
code
```

Например:

```text
https://www.manning.com/authorized?code=abc123xyz
```

Здесь:

```text
authorization code = abc123xyz
```

Важно: authorization code одноразовый и быстро истекает. Если token request завершился ошибкой, лучше заново пройти flow и получить новый `code`.

---

# Получение access token

Нужно выполнить POST request на token endpoint:

```text
http://localhost:8080/oauth2/token
```

Важно: параметры для token endpoint нужно отправлять не в query string, а в теле POST-запроса в формате:

```text
application/x-www-form-urlencoded
```

То есть правильно:

```bash
-d "grant_type=authorization_code"
```

А не так:

```text
/oauth2/token?grant_type=authorization_code
```

---

# Authorization header

Для HTTP Basic Authentication используется:

```text
client:secret
```

Это значение кодируется в Base64:

```text
client:secret
↓
Y2xpZW50OnNlY3JldA==
```

И используется в header:

```text
Authorization: Basic Y2xpZW50OnNlY3JldA==
```

---

# Правильный cURL request

Подставить:

- `AUTHORIZATION_CODE` — значение `code` из redirect URL;
- `CODE_VERIFIER` — значение `CODE VERIFIER` из output теста.

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

# Неправильный cURL request

Так делать не нужно:

```bash
curl -X POST \
"http://localhost:8080/oauth2/token?client_id=client&redirect_uri=https://www.manning.com/authorized&grant_type=authorization_code&code=AUTHORIZATION_CODE&code_verifier=CODE_VERIFIER" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA=="
```

Такой запрос может привести к ошибке:

```json
{
  "error_description": "OAuth 2.0 Parameter: grant_type",
  "error": "unsupported_grant_type"
}
```

Причина: token endpoint ожидает `grant_type` как form parameter в POST body.

---

# Где взять значения

## AUTHORIZATION_CODE

Берется из redirect URL:

```text
https://www.manning.com/authorized?code=...
```

---

## CODE_VERIFIER

Берется из output:

```text
PkceGeneratorTest
```

Важно: `CODE_VERIFIER` должен соответствовать тому `CODE_CHALLENGE`, который использовался в authorization request.

---

# Пример успешного ответа

```json
{
  "access_token": "eyJraWQiOiI4ODlhNGFmOS1...",
  "scope": "openid",
  "id_token": "eyJraWQiOiI4ODlhNGFmOS1...",
  "token_type": "Bearer",
  "expires_in": 300
}
```

---

# Что означают токены

## access_token

Используется для вызова resource server.

Пример header'а:

```text
Authorization: Bearer ACCESS_TOKEN
```

---

## id_token

OIDC token с информацией о пользователе.

Он появляется в ответе, потому что используется scope:

```text
openid
```

---

# Важные замечания

## PKCE

Проект использует PKCE.

Поэтому:

- authorization request должен содержать:
    - `code_challenge`
    - `code_challenge_method`

- token request должен содержать:
    - `code_verifier`

---

## Authorization code

Authorization code:

- одноразовый;
- быстро истекает;
- привязан к конкретному `code_challenge`;
- должен быть использован вместе с соответствующим `code_verifier`.

Если token request завершился ошибкой, лучше заново:

1. запустить `PkceGeneratorTest`;
2. открыть новый authorization URL;
3. залогиниться;
4. получить новый `code`;
5. выполнить новый token request.

---

## Base64

Base64 — это encoding, а не encryption.

Например:

```text
client:secret
```

всегда будет:

```text
Y2xpZW50OnNlY3JldA==
```

---

## HTTP vs HTTPS

В production нельзя использовать Basic Authentication через HTTP.

Для production всегда нужен HTTPS.

---

# Полезные endpoints

## OpenID configuration

```text
http://localhost:8080/.well-known/openid-configuration
```

---

## Authorization endpoint

```text
http://localhost:8080/oauth2/authorize
```

---

## Token endpoint

```text
http://localhost:8080/oauth2/token
```

---

## JWK Set endpoint

```text
http://localhost:8080/oauth2/jwks
```

---

# Flow summary

## 1. Запустить приложение

```text
Main.java
```

---

## 2. Запустить тест

```text
PkceGeneratorTest
```

---

## 3. Скопировать authorization URL

---

## 4. Открыть URL в браузере

---

## 5. Выполнить login

```text
bill / password
```

---

## 6. Скопировать authorization code из адресной строки браузера

Пример:

```text
https://www.manning.com/authorized?code=AUTHORIZATION_CODE
```

---

## 7. Выполнить правильный cURL request

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

## 8. Получить access token

Если всё сделано правильно, ответ будет со статусом:

```text
HTTP/1.1 200
```

и телом примерно такого вида:

```json
{
  "access_token": "...",
  "scope": "openid",
  "id_token": "...",
  "token_type": "Bearer",
  "expires_in": 300
}
```
