# Spring Security OAuth2 Login: Custom ClientRegistrationRepository

Этот README относится к главе 16.1.4 и объясняет подход, при котором OAuth2 client registrations создаются не автоматически через стандартные Spring Boot properties, а вручную через `ClientRegistration` и `ClientRegistrationRepository`.

Главная идея главы: если стандартной конфигурации через `application.properties` недостаточно, Spring Security позволяет полностью взять управление регистрациями OAuth2 clients на себя.

---

# Зачем нужен этот подход

В предыдущих примерах OAuth2 Login настраивался через стандартные Spring Boot properties:

```properties
spring.security.oauth2.client.registration.google.client-id=...
spring.security.oauth2.client.registration.google.client-secret=...
```

В таком случае Spring Boot сам создает нужные объекты:

```text
ClientRegistration
ClientRegistrationRepository
```

Но иногда этого недостаточно.

Например, может понадобиться:

- хранить credentials в базе данных;
- менять `client-id` и `client-secret` без redeploy приложения;
- включать или выключать OAuth2 providers динамически;
- выбирать providers на основе tenant, региона, роли пользователя или бизнес-логики;
- загружать настройки provider'ов из внешнего сервиса;
- поддерживать multi-tenant OAuth2 Login.

В таких случаях нужно создать собственный `ClientRegistrationRepository`.

---

# Два главных типа

## ClientRegistration

`ClientRegistration` описывает OAuth2 client registration.

В нем находятся данные, которые нужны приложению для работы с Authorization Server:

- `clientId`
- `clientSecret`
- `authorizationUri`
- `tokenUri`
- `redirectUri`
- `scope`
- `clientAuthenticationMethod`
- `authorizationGrantType`
- provider metadata

То есть `ClientRegistration` — это описание одного OAuth2 provider/client.

---

## ClientRegistrationRepository

`ClientRegistrationRepository` — это контракт, через который Spring Security получает client registrations.

Именно из этого repository Spring Security узнает, какие OAuth2 providers доступны для login.

Примеры реализаций:

- `InMemoryClientRegistrationRepository`
- custom repository на базе SQL database
- custom repository на базе REST API
- custom repository на базе Config Server или Vault

---

# Что демонстрирует пример из главы

В примере из главы используется Google provider, но конфигурация делается вручную.

Важно: используются не стандартные Spring Boot properties, а произвольные:

```properties
client-id=YOUR_GOOGLE_CLIENT_ID
client-secret=YOUR_GOOGLE_CLIENT_SECRET
```

Spring Boot сам по себе не понимает эти properties как OAuth2 configuration.

Они работают только потому, что мы сами читаем их через `@Value` и вручную создаем `ClientRegistration`.

---

# Зависимости Maven

Для проекта нужны зависимости:

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

Главная зависимость:

```xml
spring-boot-starter-oauth2-client
```

Она добавляет поддержку OAuth2 Login.

---

# SecurityConfig из главы

```java
package com.laurentiuspilca.ssia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${client-id}")
    private String clientId;

    @Value("${client-secret}")
    private String clientSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.oauth2Login(Customizer.withDefaults());

        http.authorizeHttpRequests(
                c -> c.anyRequest().authenticated()
        );

        return http.build();
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
                this.googleClientRegistration()
        );
    }

    private ClientRegistration googleClientRegistration() {
        return CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
```

---

# Разбор кода

## 1. Внедрение credentials

```java
@Value("${client-id}")
private String clientId;

@Value("${client-secret}")
private String clientSecret;
```

Здесь credentials читаются из `application.properties`.

В реальном приложении вместо `application.properties` можно использовать:

- базу данных;
- environment variables;
- Vault;
- Config Server;
- REST endpoint.

---

## 2. Включение OAuth2 Login

```java
http.oauth2Login(Customizer.withDefaults());
```

Эта настройка включает OAuth2 Login.

Spring Security автоматически выполняет Authorization Code Flow:

1. перенаправляет пользователя на provider;
2. получает authorization code;
3. обменивает code на access token;
4. получает user info;
5. создает authenticated session.

---

## 3. Требование authentication для всех requests

```java
http.authorizeHttpRequests(
        c -> c.anyRequest().authenticated()
);
```

Все endpoints требуют authentication.

Если пользователь не authenticated, Spring Security запускает OAuth2 Login Flow.

---

## 4. Собственный ClientRegistrationRepository

```java
@Bean
public ClientRegistrationRepository clientRegistrationRepository() {
    return new InMemoryClientRegistrationRepository(
            this.googleClientRegistration()
    );
}
```

Здесь мы создаем repository вручную.

Теперь Spring Security берет OAuth2 client registrations из этого bean, а не из auto-configuration Spring Boot.

---

## 5. Создание Google ClientRegistration

```java
private ClientRegistration googleClientRegistration() {
    return CommonOAuth2Provider.GOOGLE
            .getBuilder("google")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .build();
}
```

Метод создает registration для Google.

---

# Что делает CommonOAuth2Provider.GOOGLE

`CommonOAuth2Provider.GOOGLE` — это predefined provider template.

Spring Security уже знает для Google:

- authorization endpoint;
- token endpoint;
- user info endpoint;
- default scopes;
- redirect URI template.

Поэтому вручную указываются только:

```java
.clientId(clientId)
.clientSecret(clientSecret)
```

---

# Что означает getBuilder("google")

```java
CommonOAuth2Provider.GOOGLE.getBuilder("google")
```

Строка `"google"` — это `registrationId`.

Spring Security использует его в URL:

```text
/oauth2/authorization/google
```

и callback URL:

```text
/login/oauth2/code/google
```

---

# Что было бы без CommonOAuth2Provider

Если provider не входит в список well-known providers, пришлось бы создавать registration полностью вручную:

```java
ClientRegistration.withRegistrationId("custom")
        .clientId(clientId)
        .clientSecret(clientSecret)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid")
        .authorizationUri("https://provider.example.com/oauth2/authorize")
        .tokenUri("https://provider.example.com/oauth2/token")
        .jwkSetUri("https://provider.example.com/oauth2/jwks")
        .userInfoUri("https://provider.example.com/userinfo")
        .userNameAttributeName("sub")
        .clientName("Custom")
        .build();
```

Это именно тот подход, который понадобится для собственного или нестандартного Authorization Server.

---

# Как работает flow

## 1. Пользователь открывает приложение

```text
http://localhost:8080
```

## 2. Spring Security требует authentication

Так как настроено:

```java
.anyRequest().authenticated()
```

## 3. Spring Security ищет provider registrations

Он обращается к:

```java
ClientRegistrationRepository
```

## 4. Repository возвращает Google registration

```java
InMemoryClientRegistrationRepository
```

## 5. Пользователь перенаправляется на Google

Spring Security использует данные из `ClientRegistration`.

## 6. Google возвращает authorization code

Callback URL:

```text
/login/oauth2/code/google
```

## 7. Spring Security получает access token

И пользователь становится authenticated.

---

# Почему это важнее, чем просто пример с Google

Google здесь используется только для простоты.

Главная мысль главы:

```text
OAuth2 provider configuration можно создавать программно.
```

То есть вместо жесткой конфигурации в `application.properties` можно реализовать свою логику получения registrations.

---

# Где это полезно на практике

## 1. Multi-tenant OAuth2 Login

Например:

```text
tenant-a -> Google OAuth2 client A
tenant-b -> Google OAuth2 client B
tenant-c -> Azure AD client C
```

## 2. Dynamic provider management

Например, администратор включает или выключает login providers в UI.

## 3. Credentials без redeploy

`client-secret` можно обновить в базе данных или Vault, не пересобирая приложение.

## 4. Интеграция с корпоративным Identity Provider

Например:

- Keycloak
- Okta
- Azure AD
- Auth0
- внутренний Spring Authorization Server

## 5. Хранение registrations вне properties

Например:

```text
Database
Config Server
Vault
REST API
Kubernetes Secret
```

---

# Отличие от Spring Boot auto-configuration

## Автоматический вариант

```properties
spring.security.oauth2.client.registration.google.client-id=...
spring.security.oauth2.client.registration.google.client-secret=...
```

Spring Boot сам создает:

```text
ClientRegistrationRepository
```

## Ручной вариант

```java
@Bean
public ClientRegistrationRepository clientRegistrationRepository() {
    return new InMemoryClientRegistrationRepository(...);
}
```

Ты сам создаешь repository и сам определяешь, откуда брать registrations.

---

# Важный вывод

Spring Boot OAuth2 auto-configuration — это удобный слой поверх Spring Security.

Но под капотом все равно используются:

```text
ClientRegistration
ClientRegistrationRepository
```

Если auto-configuration недостаточно гибкая, можно создать эти объекты вручную.

---

# Итог

Глава показывает, что OAuth2 Login можно настроить не только через стандартные Spring Boot properties.

Можно вручную создать:

```java
ClientRegistration
```

и зарегистрировать его в:

```java
ClientRegistrationRepository
```

Это дает гибкость для реальных production-сценариев:

- динамические providers;
- хранение credentials в БД;
- multi-tenant login;
- runtime enable/disable;
- интеграция с нестандартными Authorization Servers.
