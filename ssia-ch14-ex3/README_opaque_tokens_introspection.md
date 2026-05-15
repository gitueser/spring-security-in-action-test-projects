# Spring Authorization Server Example (Opaque Tokens + Introspection)

Этот проект демонстрирует использование непрозрачных OAuth2 access tokens
с помощью Spring Authorization Server.

В примере используется:

- OAuth2 Client Credentials Grant
- opaque/reference access token
- token introspection endpoint

---

# Что демонстрирует проект

Проект показывает, как OAuth2 client может получить access token,
который не является JWT.

Такой токен называется opaque token или reference token.

В отличие от JWT, opaque token:

- не содержит readable payload;
- не состоит из трех частей, разделенных точками;
- не может быть самостоятельно распарсен resource server'ом;
- проверяется через Authorization Server.

---

# Какой тип гранта используется

Используется:

```text
client_credentials
```

Этот grant type подходит для machine-to-machine взаимодействия,
когда пользователь не участвует в процессе авторизации.

---

# Какой тип токена используется

В проекте используется opaque/reference access token:

```java
.tokenSettings(TokenSettings.builder()
        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
        .build())
```

Это означает, что access token будет не JWT, а короткой reference-строкой.

---

# Пример конфигурации клиента

```java
@Bean
public RegisteredClientRepository registeredClientRepository() {
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

    return new InMemoryRegisteredClientRepository(registeredClient);
}
```

---

# Зачем нужны opaque tokens

Opaque tokens удобны, когда resource server не должен самостоятельно
читать содержимое токена.

Вместо этого resource server обращается к Authorization Server
и спрашивает:

```text
этот токен активен или нет?
```

Этот процесс называется:

```text
introspection
```

---

# Преимущества opaque tokens

## 1. Данные токена не раскрываются клиенту

JWT можно декодировать и посмотреть claims.

Opaque token сам по себе ничего не раскрывает.

---

## 2. Проверка всегда идет через Authorization Server

Authorization Server остается центральным источником истины.

Это удобно, если нужно централизованно проверять:

- active status;
- expiration;
- scope;
- client_id;
- subject;
- token metadata.

---

## 3. Можно быстрее отозвать доступ

С JWT resource server часто может проверить токен локально,
пока он не истечет.

С opaque token resource server обычно делает introspection request,
поэтому Authorization Server может вернуть:

```json
{
  "active": false
}
```

---

# Недостатки opaque tokens

## 1. Нужен сетевой вызов

Чтобы проверить opaque token, resource server должен обратиться
к Authorization Server.

Это медленнее, чем локальная проверка JWT.

---

## 2. Authorization Server становится runtime dependency

Если Authorization Server недоступен,
resource server может не суметь проверить opaque token.

---

# Получение opaque access token

Для получения токена нужно выполнить POST request
на token endpoint:

```text
http://localhost:8080/oauth2/token
```

Важно: параметры нужно отправлять в POST body как:

```text
application/x-www-form-urlencoded
```

---

# Authorization header

Для HTTP Basic Authentication используется:

```text
client:secret
```

Base64 от этой строки:

```text
Y2xpZW50OnNlY3JldA==
```

Итоговый header:

```text
Authorization: Basic Y2xpZW50OnNlY3JldA==
```

---

# Правильный cURL request для получения токена

```bash
curl -i -X POST "http://localhost:8080/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=CUSTOM"
```

---

# Пример успешного ответа

```json
{
  "access_token": "iED8-aUd5QLTfihDOTGUhKgKwzhJFzYWnGdpNT2UZWO3VVDqtMONNdozq1",
  "scope": "CUSTOM",
  "token_type": "Bearer",
  "expires_in": 300
}
```

Главное отличие от JWT: значение `access_token` не имеет структуры:

```text
header.payload.signature
```

То есть это не JWT.

---

# Интроспекция токена

Так как opaque token нельзя прочитать напрямую,
его нужно проверять через introspection endpoint:

```text
http://localhost:8080/oauth2/introspect
```

---

# Правильный cURL request для introspection

Подставить полученный `access_token` в параметр `token`.

```bash
curl -i -X POST "http://localhost:8080/oauth2/introspect" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "token=ACCESS_TOKEN"
```

Пример:

```bash
curl -i -X POST "http://localhost:8080/oauth2/introspect" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "token=iED8-aUd5QLTfihDOTGUhKgKwzhJFzYWnGdpNT2UZWO3VVDqtMONNdozq1"
```

---

# Пример ответа для активного токена

```json
{
  "active": true,
  "sub": "client",
  "aud": [
    "client"
  ],
  "nbf": 1682941720,
  "scope": "CUSTOM",
  "iss": "http://localhost:8080",
  "exp": 1682942020,
  "iat": 1682941720,
  "jti": "ff14b844-1627-4567-8657-bba04cac0370",
  "client_id": "client",
  "token_type": "Bearer"
}
```

---

# Пример ответа для неактивного токена

Если токен неправильный, не существует или истек,
introspection endpoint вернет:

```json
{
  "active": false
}
```

---

# Увеличение времени жизни токена

По умолчанию access token живет около 300 секунд.

Для учебных примеров это может быть неудобно,
потому что токен быстро истекает.

Можно увеличить время жизни токена:

```java
.tokenSettings(TokenSettings.builder()
        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
        .accessTokenTimeToLive(Duration.ofHours(12))
        .build())
```

Полный пример:

```java
RegisteredClient registeredClient = RegisteredClient
        .withId(UUID.randomUUID().toString())
        .clientId("client")
        .clientSecret("secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .tokenSettings(TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                .accessTokenTimeToLive(Duration.ofHours(12))
                .build())
        .scope("CUSTOM")
        .build();
```

Важно: большое время жизни удобно для учебных примеров,
но в production так делать не стоит.

Обычно access token живет примерно:

```text
10-30 minutes
```

---

# Что важно запомнить

- JWT token можно проверить локально.
- Opaque token нельзя прочитать напрямую.
- Opaque token проверяется через introspection endpoint.
- Для opaque token Authorization Server остается источником истины.
- `client_credentials` flow не использует пользователя.
- В ответе не будет `id_token`.
- Параметры для `/oauth2/token` и `/oauth2/introspect` нужно отправлять как `application/x-www-form-urlencoded` body.

---

# Минимальный flow

## 1. Получить opaque access token

```bash
curl -i -X POST "http://localhost:8080/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=CUSTOM"
```

---

## 2. Скопировать access_token из ответа

```json
{
  "access_token": "ACCESS_TOKEN"
}
```

---

## 3. Проверить token через introspection

```bash
curl -i -X POST "http://localhost:8080/oauth2/introspect" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "token=ACCESS_TOKEN"
```

---

## 4. Проверить поле active

Если токен действительный:

```json
{
  "active": true
}
```

Если токен недействительный или истек:

```json
{
  "active": false
}
```


---

# Расшифровка claims в introspection response

Пример introspection response:

```json
{
  "active": true,
  "sub": "client",
  "aud": [
    "client"
  ],
  "nbf": 1682941720,
  "scope": "CUSTOM",
  "iss": "http://localhost:8080",
  "exp": 1682942020,
  "iat": 1682941720,
  "jti": "ff14b844-1627-4567-8657-bba04cac0370",
  "client_id": "client",
  "token_type": "Bearer"
}
```

Большинство этих полей являются стандартными JWT/OAuth2 claims.

---

# Что означает RFC

RFC = Request For Comments

Это официальные технические спецификации и стандарты интернета.

Например:

- RFC 6749 — OAuth2
- RFC 7519 — JWT
- RFC 7662 — OAuth2 Token Introspection

---

# Расшифровка claims

## sub

```json
"sub": "client"
```

`sub` = Subject

Означает:

```text
кому принадлежит токен
```

В `client_credentials` flow:

```json
"sub": "client"
```

потому что пользователя нет.

В `authorization_code` flow обычно было бы:

```json
"sub": "bill"
```

---

## aud

```json
"aud": ["client"]
```

`aud` = Audience

Означает:

```text
для кого предназначен токен
```

Обычно:
- API;
- resource server;
- service;
- OAuth2 client.

---

## nbf

```json
"nbf": 1682941720
```

`nbf` = Not Before

Означает:

```text
токен нельзя использовать раньше этого времени
```

---

## iss

```json
"iss": "http://localhost:8080"
```

`iss` = Issuer

Означает:

```text
кто выпустил токен
```

В данном примере issuer:

```text
http://localhost:8080
```

---

## exp

```json
"exp": 1682942020
```

`exp` = Expiration Time

Означает:

```text
когда токен истекает
```

После этого времени:

```json
{
  "active": false
}
```

---

## iat

```json
"iat": 1682941720
```

`iat` = Issued At

Означает:

```text
когда токен был создан
```

---

## jti

```json
"jti": "ff14b844-1627-4567-8657-bba04cac0370"
```

`jti` = JWT ID

Это уникальный идентификатор токена.

Используется для:
- revocation;
- blacklist;
- audit;
- tracing;
- anti-replay protection.

---

## scope

```json
"scope": "CUSTOM"
```

Показывает scopes,
которые были выданы токену.

---

## client_id

```json
"client_id": "client"
```

OAuth2 client,
который получил токен.

---

## token_type

```json
"token_type": "Bearer"
```

Тип токена.

`Bearer` означает:

```text
кто владеет токеном — тот и авторизован
```

---

# Важный момент про opaque token

Хотя opaque token выглядит как случайная строка:

```text
iED8-aUd5QLTfihDOTGUhKgKwzhJFzY...
```

Authorization Server всё равно хранит metadata токена у себя.

Поэтому через introspection endpoint можно получить:

- claims;
- expiration;
- scope;
- issuer;
- client_id;
- active status;
- и другие metadata.
