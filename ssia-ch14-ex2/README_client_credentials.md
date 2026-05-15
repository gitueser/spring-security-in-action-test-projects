# Spring Authorization Server Example (Client Credentials Grant)

Этот проект демонстрирует использование OAuth2 Client Credentials Grant
с помощью Spring Authorization Server.

Используемые технологии:

- Spring Boot 3
- Spring Security 6
- Spring Authorization Server

---

# Что демонстрирует проект

Проект показывает, как OAuth2 client может получить access token
без участия пользователя.

В данном flow:

- пользователь не логинится;
- login form не используется;
- authorization code не используется;
- PKCE не используется;
- клиент получает access token напрямую через свои credentials.

---

# Какой тип гранта используется

Используется:

```text
client_credentials
```

---

# Когда используется Client Credentials Grant

Этот flow обычно используется для:

- server-to-server communication;
- backend services;
- machine-to-machine authentication;
- service accounts;
- internal APIs;
- scheduled jobs;
- health/liveness checks.

---

# Важные особенности

## Нет пользователя

В этом flow нет аутентификации пользователя.

Токен выдается самому OAuth2 client.

Поэтому в ответе:

- нет `id_token`;
- нет OpenID Connect login;
- нет authorization page.

---

## Используйте отдельные clients

Не рекомендуется использовать один и тот же OAuth2 client одновременно для:

- authorization_code
- client_credentials

Лучше регистрировать отдельные clients
и использовать разные scopes.

---

## Используйте scopes

Scopes помогают различать назначение токена.

В этом проекте используется:

```text
CUSTOM
```

---

# OAuth2 Client Configuration

В проекте зарегистрирован OAuth2 client:

```text
client_id: client
client_secret: secret
scope: CUSTOM
grant_type: client_credentials
```

---

# Почему UserDetailsService больше не нужен

В `client_credentials` flow пользователь не участвует.

Поэтому `UserDetailsService` можно удалить:

```java
@Bean
public UserDetailsService userDetailsService() {
    var uds = new InMemoryUserDetailsManager();
    uds.createUser(
            User.withUsername("bill")
                    .password("password")
                    .roles("USER")
                    .build()
    );
    return uds;
}
```

Потому что:

- login form больше не используется;
- пользователь не проходит authentication;
- authorization code flow не используется;
- access token выдается самому OAuth2 client.

---

# Почему PasswordEncoder всё ещё нужен

Хотя пользователя больше нет, OAuth2 client всё ещё проходит authentication.

Когда выполняется запрос:

```text
Authorization: Basic Y2xpZW50OnNlY3JldA==
```

Spring Authorization Server получает:

```text
client:secret
```

и должен проверить `client_secret`.

Для проверки secret используется `PasswordEncoder`.

Поэтому этот bean всё ещё нужен:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return NoOpPasswordEncoder.getInstance();
}
```

---

# Что произойдет если убрать PasswordEncoder

Если удалить `PasswordEncoder`, Spring Security не сможет проверить:

```java
.clientSecret("secret")
```

и появится ошибка примерно такого вида:

```text
There is no PasswordEncoder mapped for the id "null"
```

---

# Альтернативный вариант

Можно убрать `PasswordEncoder`, если использовать:

```java
.clientSecret("{noop}secret")
```

Тогда Spring Security поймет, что используется plain text secret.

Но для учебного примера проще оставить:

```java
.clientSecret("secret")
```

и:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return NoOpPasswordEncoder.getInstance();
}
```

---

# Authorization header

Для HTTP Basic Authentication используется формат:

```text
client_id:client_secret
```

В текущем проекте:

```text
client_id: client
client_secret: secret
```

Значит, строка для Base64 encoding:

```text
client:secret
```

После Base64 encoding:

```text
Y2xpZW50OnNlY3JldA==
```

И поэтому используется header:

```text
Authorization: Basic Y2xpZW50OnNlY3JldA==
```

---

# Важно: Base64 кодируется не только secret

Base64 нужно делать не от одного `client_secret`, а от всей строки:

```text
client_id:client_secret
```

То есть не так:

```text
secret
```

а так:

```text
client:secret
```

---

# Пример с другим client_secret

Можно заменить:

```java
.clientSecret("secret")
```

на, например:

```java
.clientSecret("HJKdghuhn38tjfgs_3=")
```

Тогда строка для Base64 encoding будет:

```text
client:HJKdghuhn38tjfgs_3=
```

Base64 для этой строки:

```text
Y2xpZW50OkhKS2RnaHVobjM4dGpmZ3NfMz0=
```

Тогда Authorization header будет таким:

```text
Authorization: Basic Y2xpZW50OkhKS2RnaHVobjM4dGpmZ3NfMz0=
```

И curl команда будет такой:

```bash
curl -i -X POST "http://localhost:8080/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OkhKS2RnaHVobjM4dGpmZ3NfMz0=" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=CUSTOM"
```

После изменения `clientSecret` нужно перезапустить приложение.

---

# Как самому получить Base64 значение

В Git Bash / Linux / macOS:

```bash
echo -n "client:HJKdghuhn38tjfgs_3=" | base64
```

В Java:

```java
String value = "client:HJKdghuhn38tjfgs_3=";

String encoded = Base64.getEncoder()
        .encodeToString(value.getBytes());

System.out.println(encoded);
```

---

# Правильный cURL request

Важно: параметры должны отправляться как:

```text
application/x-www-form-urlencoded
```

в POST body.

Правильная команда для текущего secret `secret`:

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
  "access_token": "eyJraWQiOiI4N2E3YjJiNS…",
  "scope": "CUSTOM",
  "token_type": "Bearer",
  "expires_in": 300
}
```

---

# Что означают поля ответа

## access_token

JWT access token.

Используется для вызова resource server:

```text
Authorization: Bearer ACCESS_TOKEN
```

---

## scope

Показывает scopes, выданные токену.

В данном примере:

```text
CUSTOM
```

---

## token_type

Тип токена:

```text
Bearer
```

---

## expires_in

Время жизни токена в секундах.

В данном примере:

```text
300
```
