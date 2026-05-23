# Spring Security OAuth2 Login: Google and GitHub

Этот проект демонстрирует OAuth2 Login в Spring Security.

Рассматриваются два сценария из главы 16:

- вход через одного OAuth2 provider, например Google;
- вход через несколько OAuth2 providers, например Google и GitHub.

---

# Что демонстрирует проект

Проект показывает, как Spring Security может использовать внешнего провайдера аутентификации вместо собственной формы входа.

Пользователь не вводит логин и пароль в нашем приложении. Вместо этого он перенаправляется на внешний сервис:

- Google
- GitHub

После успешной аутентификации внешний provider возвращает пользователя обратно в приложение.

Spring Security выполняет OAuth2 Authorization Code Flow автоматически.

---

# Используемые зависимости

В проект нужно добавить OAuth2 Client starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

Главная новая зависимость здесь:

```xml
spring-boot-starter-oauth2-client
```

Она добавляет поддержку OAuth2 Login на стороне web application.

---

# Demo controller

В проекте используется простой controller:

```java
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index.html";
    }
}
```

Он возвращает домашнюю страницу:

```text
index.html
```

---

# Demo HTML page

Пример простой страницы:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Home</title>
</head>
<body>
    <h1>Home</h1>
</body>
</html>
```

Эта страница будет доступна только после успешной OAuth2 authentication.

---

# Security configuration

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.oauth2Login(Customizer.withDefaults());

        http.authorizeHttpRequests(c -> c
                .anyRequest().authenticated()
        );

        return http.build();
    }
}
```

---

# Что делает oauth2Login()

```java
http.oauth2Login(Customizer.withDefaults());
```

Эта строка включает OAuth2 Login в приложении.

Spring Security автоматически:

1. создает login endpoint;
2. перенаправляет пользователя к OAuth2 provider;
3. принимает callback от provider;
4. получает authorization code;
5. обменивает authorization code на access token;
6. получает информацию о пользователе;
7. создает authenticated session.

---

# Что делает authenticated()

```java
http.authorizeHttpRequests(c -> c
        .anyRequest().authenticated()
);
```

Эта настройка требует authentication для всех endpoint'ов.

То есть при открытии:

```text
http://localhost:8080/
```

пользователь сначала будет перенаправлен на OAuth2 Login.

---

# Authorization Code Flow под капотом

Когда используется:

```java
oauth2Login()
```

Spring Security автоматически выполняет Authorization Code Flow.

Упрощенно процесс выглядит так:

```text
Browser
  -> Spring application
  -> OAuth2 provider login page
  -> user login
  -> provider redirects back with authorization code
  -> Spring exchanges code for access token
  -> Spring loads user info
  -> user becomes authenticated
```

---

# Well-known providers

Spring Security заранее знает настройки для некоторых популярных OAuth2 providers.

В книге перечислены:

- Google
- GitHub
- Okta
- Facebook

Эти настройки находятся в Spring Security в классе:

```java
CommonOAuth2Provider
```

Для таких providers Spring Security уже знает:

- authorization endpoint;
- token endpoint;
- user info endpoint;
- default scopes;
- redirect URI pattern.

Поэтому для Google и GitHub обычно достаточно указать только:

- client-id;
- client-secret.

---

# Сценарий 1: вход через одного provider

В главе 16.1.1 рассматривается самый простой вариант:

```text
только Google Login
```

В этом случае в `application.properties` нужно добавить только Google credentials.

---

## Google configuration

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
```

---

## Где получить Google client-id и client-secret

Нужно зарегистрировать OAuth2 application в Google Cloud Console.

Ссылка из книги для настройки Google OAuth2 application:

```text
http://mng.bz/eEvz
```

Также можно использовать официальную документацию Google:

```text
https://developers.google.com/identity/protocols/oauth2
```

---

## Redirect URI для Google

В настройках OAuth2 application в Google нужно добавить redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

Spring Security использует такой default redirect URI pattern:

```text
/login/oauth2/code/{registrationId}
```

Для Google:

```text
registrationId = google
```

Поэтому итоговый callback URL:

```text
http://localhost:8080/login/oauth2/code/google
```

---

## Как работает сценарий с Google

1. Пользователь открывает:

```text
http://localhost:8080/
```

2. Spring Security видит, что endpoint требует authentication.

3. Пользователь перенаправляется на Google Login.

4. Пользователь входит через Google account.

5. Google перенаправляет пользователя обратно:

```text
http://localhost:8080/login/oauth2/code/google?code=...
```

6. Spring Security получает authorization code.

7. Spring Security обменивает code на access token.

8. Spring Security получает user info.

9. Пользователь получает доступ к:

```text
/
```

---

# Сценарий 2: вход через несколько providers

В главе 16.1.2 рассматривается вариант, когда пользователь может выбрать provider.

Например:

- Google
- GitHub

В этом случае в `application.properties` нужно добавить credentials для обоих providers.

---

## Google + GitHub configuration

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET

spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.client-secret=YOUR_GITHUB_CLIENT_SECRET
```

---

# GitHub configuration

## Где получить GitHub client-id и client-secret

Нужно зарегистрировать OAuth App в GitHub.

Ссылка из книги для настройки GitHub OAuth App:

```text
http://mng.bz/p1YG
```

Также можно использовать официальную документацию GitHub:

```text
https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app
```

---

## Redirect URI для GitHub

В настройках OAuth App в GitHub нужно добавить callback URL:

```text
http://localhost:8080/login/oauth2/code/github
```

Для GitHub:

```text
registrationId = github
```

Поэтому Spring Security ожидает callback по адресу:

```text
http://localhost:8080/login/oauth2/code/github
```

---

# Что происходит при нескольких providers

Если настроен только один provider, Spring Security может сразу перенаправить пользователя к нему.

Если настроено несколько providers, Spring Security показывает страницу выбора login provider.

Пользователь увидит варианты:

```text
Google
GitHub
```

После выбора provider Spring Security начнет OAuth2 Login Flow для выбранного provider.

---

# Login endpoints

Spring Security автоматически создает endpoints для начала OAuth2 Login.

## Google

```text
http://localhost:8080/oauth2/authorization/google
```

## GitHub

```text
http://localhost:8080/oauth2/authorization/github
```

Эти endpoints можно открыть напрямую.

---

# Callback endpoints

Provider возвращает пользователя обратно в приложение на callback endpoint.

## Google callback

```text
http://localhost:8080/login/oauth2/code/google
```

## GitHub callback

```text
http://localhost:8080/login/oauth2/code/github
```

Эти URLs нужно указывать в настройках OAuth2 application на стороне provider.

---

# Минимальный application.properties для Google

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
```

---

# Минимальный application.properties для Google + GitHub

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET

spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.client-secret=YOUR_GITHUB_CLIENT_SECRET
```

---

# Важное замечание про secrets

Не стоит хранить реальные `client-secret` прямо в Git repository.

Для учебного проекта это допустимо только локально.

В реальном приложении лучше использовать:

- environment variables;
- external configuration;
- secret manager;
- vault.

Например:

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}

spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID}
spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET}
```

---

# Запуск приложения

Через IDE:

```text
Main.java
```

Через Maven:

```bash
mvn spring-boot:run
```

После запуска приложение будет доступно на:

```text
http://localhost:8080
```

---

# Проверка Google Login

1. Настроить Google credentials в `application.properties`.

2. Запустить приложение.

3. Открыть:

```text
http://localhost:8080/
```

4. Пройти login через Google.

5. После успешного login должна открыться страница:

```text
Home
```

---

# Проверка GitHub Login

1. Настроить GitHub credentials в `application.properties`.

2. Запустить приложение.

3. Открыть:

```text
http://localhost:8080/
```

4. Выбрать GitHub на странице выбора provider.

5. Пройти login через GitHub.

6. После успешного login должна открыться страница:

```text
Home
```

---

# Что важно запомнить

- `oauth2Login()` включает OAuth2 Login в Spring Security.
- Spring Security автоматически выполняет Authorization Code Flow.
- Для well-known providers не нужно вручную задавать authorization endpoint и token endpoint.
- Для Google и GitHub достаточно указать `client-id` и `client-secret`.
- При одном provider пользователь перенаправляется на него.
- При нескольких providers Spring Security показывает страницу выбора.
- Redirect URI должен совпадать с тем, что настроено у provider.
- Реальные secrets не стоит хранить в Git.

---

# Полезные ссылки из главы

## Google OAuth2 application setup

```text
http://mng.bz/eEvz
```

## GitHub OAuth App setup

```text
http://mng.bz/p1YG
```

---

# Flow summary

## Один provider

```text
User opens /
  -> Spring Security redirects to Google
  -> user logs in
  -> Google redirects back
  -> Spring Security authenticates user
  -> Home page is shown
```

## Несколько providers

```text
User opens /
  -> Spring Security shows provider selection page
  -> user selects Google or GitHub
  -> selected provider authenticates user
  -> provider redirects back
  -> Spring Security authenticates user
  -> Home page is shown
```
