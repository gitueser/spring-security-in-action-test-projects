# Spring Security OAuth2 Login with Custom Authorization Server

Этот пример относится к главе 16.1.3 и показывает, как реализовать OAuth2 Login в Spring Security с использованием собственного Authorization Server вместо Google, GitHub или других встроенных провайдеров.

В примере используются два приложения:

```text
ssia-ch16-ex1-authorization-server
ssia-ch16-ex1-client
```

---

# Что показывает этот пример

В предыдущих разделах главы использовались встроенные провайдеры:

- Google
- GitHub
- Facebook
- Okta

Spring Security уже знает их:

- authorization endpoint;
- token endpoint;
- JWK Set endpoint;
- issuer;
- user info endpoint.

Поэтому для таких well-known providers обычно достаточно указать только:

```properties
client-id
client-secret
```

Но в реальных проектах часто используется:

- собственный Authorization Server;
- корпоративный Identity Provider;
- внутренний OAuth2 сервер компании;
- Keycloak;
- Auth0;
- Spring Authorization Server.

Для таких случаев Spring Security позволяет зарегистрировать custom provider вручную.

---

# Проекты

## Authorization Server

Проект:

```text
ssia-ch16-ex1-authorization-server
```

Основан на проекте:

```text
ssia-ch14-ex1
```

Этот сервер:

- аутентифицирует пользователя;
- выдает authorization code;
- выдает access token;
- реализует OpenID Connect provider;
- используется как custom OAuth2 provider для web-приложения.

---

## OAuth2 Client Web Application

Проект:

```text
ssia-ch16-ex1-client
```

Это Spring MVC web-приложение.

Оно:

- перенаправляет пользователя на Authorization Server;
- получает authorization code;
- получает access token;
- создает authenticated session;
- показывает домашнюю страницу после login.

---

# Главная идея главы

Для встроенных провайдеров достаточно указать registration:

```properties
spring.security.oauth2.client.registration.google.client-id=...
spring.security.oauth2.client.registration.google.client-secret=...
```

Но для собственного Authorization Server нужно дополнительно сообщить Spring Security, где находится provider.

Если Authorization Server поддерживает OpenID Connect, достаточно указать:

```properties
issuer-uri
```

Spring Security сам получит остальные endpoints через:

```text
/.well-known/openid-configuration
```

---

# Как работает OAuth2 Login flow

## 1. Пользователь открывает web-приложение

```text
http://localhost:8080
```

## 2. Spring Security понимает, что пользователь не аутентифицирован

и автоматически начинает OAuth2 Login Flow.

## 3. Пользователь перенаправляется на Authorization Server

```text
http://127.0.0.1:7070/oauth2/authorize
```

## 4. Пользователь логинится на Authorization Server

```text
bill / password
```

## 5. Authorization Server выдает authorization code

и делает redirect обратно в web-приложение:

```text
http://localhost:8080/login/oauth2/code/my_authorization_server
```

## 6. Spring Security в client-приложении автоматически:

- получает authorization code;
- вызывает token endpoint;
- получает access token;
- получает id token;
- создает authenticated session.

## 7. Пользователь попадает на домашнюю страницу

```text
index.html
```

---

# Почему используются localhost и 127.0.0.1

Это важный момент из книги.

## Authorization Server

```text
http://127.0.0.1:7070
```

## Web Application

```text
http://localhost:8080
```

Хотя:

```text
localhost
127.0.0.1
```

указывают на один и тот же компьютер, браузер считает их разными host/domain.

Это помогает избежать проблем:

- с cookies;
- с session;
- с OAuth2 login flow;
- с тем, что два приложения работают локально одновременно.

---

# Authorization Server project

Проект:

```text
ssia-ch16-ex1-authorization-server
```

---

# Authorization Server application.properties

```properties
server.port=7070
```

Authorization Server должен работать на порту:

```text
7070
```

потому что OAuth2 Client Web Application будет работать на порту:

```text
8080
```

---

# Authorization Server RegisteredClient

Authorization Server должен знать web-приложение как OAuth2 client.

Пример регистрации:

```java
@Bean
public RegisteredClientRepository registeredClientRepository() {
    RegisteredClient registeredClient =
            RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("client")
                    .clientSecret("secret")
                    .clientAuthenticationMethod(
                            ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                    )
                    .authorizationGrantType(
                            AuthorizationGrantType.AUTHORIZATION_CODE
                    )
                    .redirectUri(
                            "http://localhost:8080/login/oauth2/code/my_authorization_server"
                    )
                    .scope(OidcScopes.OPENID)
                    .build();

    return new InMemoryRegisteredClientRepository(registeredClient);
}
```

---

# Важный момент: redirect URI

Redirect URI на стороне Authorization Server:

```text
http://localhost:8080/login/oauth2/code/my_authorization_server
```

Spring Security OAuth2 Client использует стандартный callback endpoint:

```text
/login/oauth2/code/{registrationId}
```

В этом примере:

```text
registrationId = my_authorization_server
```

Поэтому итоговый redirect URI:

```text
http://localhost:8080/login/oauth2/code/my_authorization_server
```

Этот URI должен совпадать:

1. в `RegisteredClient` на стороне Authorization Server;
2. в `application.properties` на стороне OAuth2 Client Web Application.

---

# Почему нужен scope openid

В registered client указан scope:

```java
.scope(OidcScopes.OPENID)
```

Это означает, что используется OpenID Connect поверх OAuth2.

OAuth2 Login в Spring Security обычно ожидает OIDC provider, если используется `issuer-uri`.

Поэтому web-приложение будет запрашивать:

```text
openid
```

---

# OAuth2 Client Web Application project

Проект:

```text
ssia-ch16-ex1-client
```

---

# OAuth2 Client dependencies

В `pom.xml` web-приложения нужны зависимости:

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

Главная зависимость здесь:

```xml
spring-boot-starter-oauth2-client
```

Она включает поддержку OAuth2 Login.

---

# OAuth2 Client SecurityConfig

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.oauth2Login(Customizer.withDefaults());

        http.authorizeHttpRequests(
                c -> c.anyRequest().authenticated()
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

Эта настройка включает OAuth2 Login.

Spring Security автоматически:

- создает endpoint начала OAuth2 login;
- перенаправляет пользователя на Authorization Server;
- обрабатывает redirect обратно;
- получает authorization code;
- вызывает token endpoint;
- получает token;
- создает authenticated session.

---

# OAuth2 Client application.properties

```properties
spring.security.oauth2.client.provider.my_authorization_server.issuer-uri=http://127.0.0.1:7070

spring.security.oauth2.client.registration.my_authorization_server.client-id=client
spring.security.oauth2.client.registration.my_authorization_server.client-name=Custom
spring.security.oauth2.client.registration.my_authorization_server.client-secret=secret
spring.security.oauth2.client.registration.my_authorization_server.provider=my_authorization_server
spring.security.oauth2.client.registration.my_authorization_server.client-authentication-method=client_secret_basic
spring.security.oauth2.client.registration.my_authorization_server.redirect-uri=http://localhost:8080/login/oauth2/code/my_authorization_server
spring.security.oauth2.client.registration.my_authorization_server.scope[0]=openid
```

---

# Provider configuration

```properties
spring.security.oauth2.client.provider.my_authorization_server.issuer-uri=http://127.0.0.1:7070
```

Эта настройка сообщает Spring Security, где находится custom provider.

Spring Security вызывает:

```text
http://127.0.0.1:7070/.well-known/openid-configuration
```

и автоматически получает:

- authorization endpoint;
- token endpoint;
- jwks endpoint;
- issuer;
- userinfo endpoint;
- supported scopes;
- supported grant types.

---

# Client registration

```properties
spring.security.oauth2.client.registration.my_authorization_server.client-id=client
```

Это client id, зарегистрированный на Authorization Server.

```properties
spring.security.oauth2.client.registration.my_authorization_server.client-secret=secret
```

Это client secret, зарегистрированный на Authorization Server.

```properties
spring.security.oauth2.client.registration.my_authorization_server.client-name=Custom
```

Это отображаемое имя provider'а на странице выбора login method.

```properties
spring.security.oauth2.client.registration.my_authorization_server.provider=my_authorization_server
```

Эта настройка связывает registration с provider configuration.

```properties
spring.security.oauth2.client.registration.my_authorization_server.client-authentication-method=client_secret_basic
```

Эта настройка означает, что client будет аутентифицироваться на token endpoint через HTTP Basic.

```properties
spring.security.oauth2.client.registration.my_authorization_server.redirect-uri=http://localhost:8080/login/oauth2/code/my_authorization_server
```

Это callback URL, на который Authorization Server вернет пользователя после login.

```properties
spring.security.oauth2.client.registration.my_authorization_server.scope[0]=openid
```

Web-приложение запрашивает scope:

```text
openid
```

---

# Что такое registrationId

В настройках:

```properties
spring.security.oauth2.client.registration.my_authorization_server...
```

часть:

```text
my_authorization_server
```

это registrationId.

Он используется:

- в redirect URI;
- в login endpoint;
- во внутренней конфигурации Spring Security.

---

# Login endpoint

Spring Security автоматически создает endpoint:

```text
/oauth2/authorization/my_authorization_server
```

Если открыть этот endpoint напрямую, начнется OAuth2 Login Flow.

---

# Redirect endpoint

Spring Security автоматически ожидает callback на:

```text
/login/oauth2/code/my_authorization_server
```

Именно поэтому этот URI должен быть зарегистрирован на Authorization Server.

---

# Запуск приложений

Сначала нужно запустить Authorization Server.

---

# 1. Запустить Authorization Server

В папке:

```text
ssia-ch16-ex1-authorization-server
```

Команда:

```bash
mvn spring-boot:run
```

Authorization Server будет доступен на:

```text
http://127.0.0.1:7070
```

---

# 2. Запустить OAuth2 Client Web Application

В папке:

```text
ssia-ch16-ex1-client
```

Команда:

```bash
mvn spring-boot:run
```

Web-приложение будет доступно на:

```text
http://localhost:8080
```

---

# Использование Maven без отдельной установки

Чтобы команды `mvn` работали в Git Bash без отдельной установки Maven в систему, можно создать alias для встроенного Maven из IntelliJ IDEA.

Выполните команды:

```bash
echo "alias mvn='/c/Users/<USERNAME>/AppData/Roaming/JetBrains/<INTELLIJ_VERSION>/plugins/maven/lib/maven3/bin/mvn.cmd'" >> ~/.bashrc
source ~/.bashrc
```

Где:

- `<USERNAME>` — имя пользователя Windows
- `<INTELLIJ_VERSION>` — версия IntelliJ IDEA, например `IntelliJIdea2026.1`

После этого команда `mvn` станет доступна в Git Bash.

---

# Проверка работы

## 1. Открыть web-приложение

```text
http://localhost:8080
```

## 2. Spring Security перенаправит пользователя на login flow

Можно также открыть login endpoint напрямую:

```text
http://localhost:8080/oauth2/authorization/my_authorization_server
```

## 3. Пользователь будет перенаправлен на Authorization Server

```text
http://127.0.0.1:7070/login
```

## 4. Выполнить login

```text
username: bill
password: password
```

## 5. После успешного login произойдет redirect обратно

```text
http://localhost:8080/login/oauth2/code/my_authorization_server
```

## 6. Откроется домашняя страница

```html
<h1>Home</h1>
```

---

# OpenID Connect Discovery endpoint

Spring Security использует:

```text
http://127.0.0.1:7070/.well-known/openid-configuration
```

Этот endpoint возвращает метаданные Authorization Server.

Пример важных полей:

```json
{
  "issuer": "http://127.0.0.1:7070",
  "authorization_endpoint": "http://127.0.0.1:7070/oauth2/authorize",
  "token_endpoint": "http://127.0.0.1:7070/oauth2/token",
  "jwks_uri": "http://127.0.0.1:7070/oauth2/jwks"
}
```

---

# Что важно понять в этой главе

## OAuth2 Login Client — это не Resource Server

В этом примере web-приложение:

- не проверяет Bearer tokens;
- не защищает REST API через access token;
- не работает как Resource Server.

Оно работает как:

```text
OAuth2 Client
```

и использует Authorization Server для login.

---

# Spring Security сам реализует Authorization Code Flow

Тебе не нужно вручную:

- открывать authorization URL;
- получать authorization code;
- вызывать token endpoint;
- обрабатывать callback;
- создавать session.

Всё это делает:

```java
oauth2Login()
```

---

# issuer-uri — ключевая настройка

Если Authorization Server поддерживает OpenID Connect, то:

```properties
spring.security.oauth2.client.provider.my_authorization_server.issuer-uri=http://127.0.0.1:7070
```

достаточно для получения всех provider endpoints.

Если provider не поддерживает OpenID Connect Discovery, тогда endpoints пришлось бы задавать вручную:

- authorization-uri;
- token-uri;
- jwk-set-uri;
- user-info-uri.

---

# Частые ошибки

## redirect_uri mismatch

Если redirect URI в Authorization Server не совпадает с redirect URI в client-приложении, login flow завершится ошибкой.

Проверить нужно оба места:

Authorization Server:

```java
.redirectUri("http://localhost:8080/login/oauth2/code/my_authorization_server")
```

OAuth2 Client:

```properties
spring.security.oauth2.client.registration.my_authorization_server.redirect-uri=http://localhost:8080/login/oauth2/code/my_authorization_server
```

---

## Неправильный issuer-uri

Если указать:

```properties
issuer-uri=http://localhost:7070
```

вместо:

```properties
issuer-uri=http://127.0.0.1:7070
```

могут возникнуть проблемы с cookie/session при локальном запуске двух приложений.

В этом примере рекомендуется:

```text
Authorization Server: http://127.0.0.1:7070
Client App:           http://localhost:8080
```

---

## Authorization Server не запущен

OAuth2 Client при старте может обращаться к discovery endpoint:

```text
/.well-known/openid-configuration
```

Поэтому сначала нужно запускать Authorization Server, а потом client-приложение.

---

---

# Важное замечание про index.html

В этом примере файл:

```text
index.html
```

должен находиться в директории:

```text
src/main/resources/static/
```

а не в:

```text
src/main/resources/templates/
```

Причина:

- папка `static` используется Spring Boot для автоматической раздачи статических ресурсов;
- папка `templates` предназначена для template engine (Thymeleaf, Mustache, Freemarker и т.д.).

В данном примере Thymeleaf не используется, поэтому файл в `templates` приведет к ошибке:

```text
Whitelabel Error Page
404 Not Found
```

---

# Почему приложение работает даже без HomeController

Если файл расположен здесь:

```text
src/main/resources/static/index.html
```

то Spring Boot автоматически публикует его по адресу:

```text
http://localhost:8080/
```

Поэтому `HomeController` в данном примере не обязателен.

---

# Что делает текущий HomeController

```java
@RestController
public class HomeController {

    @GetMapping("/home")
    public String home() {
        return "index.html";
    }
}
```

Так как используется `@RestController`,
строка `"index.html"` возвращается как обычный текст HTTP response body.

Поэтому при открытии:

```text
http://localhost:8080/home
```

браузер отображает:

```text
index.html
```

а не HTML страницу.

---

# Как выглядел бы controller для templates + Thymeleaf

Если использовать Thymeleaf, тогда:

1. нужно добавить dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

2. файл должен лежать здесь:

```text
src/main/resources/templates/index.html
```

3. controller должен быть таким:

```java
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
```

В этой главе используется более простой вариант со `static/index.html`.