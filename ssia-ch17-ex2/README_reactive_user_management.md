# Spring Security Reactive Applications: User Management

Этот пример относится к главе 17.2 и показывает, как Spring Security работает с управлением пользователями в реактивных приложениях на базе Spring WebFlux.

Проект:

```text
ssia-ch17-ex2
```

---

# Что изучается в этой главе

В главе рассматриваются:

- `ReactiveUserDetailsService`
- `MapReactiveUserDetailsService`
- `ReactiveAuthenticationManager`
- `ReactiveSecurityContextHolder`
- получение `Authentication` в реактивном приложении
- работа `SecurityContext` в WebFlux
- отличия реактивной безопасности от servlet-based безопасности

---

# Главная идея главы

В обычных Spring MVC приложениях часто работает модель:

```text
один request -> один thread
```

Поэтому классический Spring Security может хранить `SecurityContext` через `ThreadLocal`.

В реактивном приложении один request может обрабатываться несколькими потоками. Поэтому `ThreadLocal` больше не является подходящим механизмом хранения security context.

Для реактивного стека Spring Security использует:

```java
ReactiveSecurityContextHolder
```

---

# Зависимости pom.xml

Для этой главы нужны зависимости:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

`spring-boot-starter-web` добавлять не нужно, потому что глава работает с реактивным стеком WebFlux.

---

# ReactiveUserDetailsService

В обычных приложениях используется:

```java
UserDetailsService
```

В реактивных приложениях используется:

```java
ReactiveUserDetailsService
```

Обычный контракт возвращает `UserDetails`:

```java
UserDetails loadUserByUsername(String username)
```

Реактивный контракт возвращает `Mono<UserDetails>`:

```java
Mono<UserDetails> findByUsername(String username)
```

Главное отличие: результат возвращается как reactive publisher.

---

# ProjectConfig

В проекте создается один пользователь:

```text
username: john
password: 12345
authority: read
```

Пример конфигурации:

```java
package com.laurentiuspilca.ssia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ProjectConfig {

    @Bean
    public ReactiveUserDetailsService userDetailsService() {
        var u = User.withUsername("john")
                .password("12345")
                .authorities("read")
                .build();

        return new MapReactiveUserDetailsService(u);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
```

---

# Как работает реактивная аутентификация

Spring Security использует `AuthenticationWebFilter`. Этот фильтр перехватывает HTTP request и делегирует аутентификацию компоненту:

```java
ReactiveAuthenticationManager
```

Если аутентификация основана на username/password, то `ReactiveAuthenticationManager` использует:

- `ReactiveUserDetailsService` для поиска пользователя;
- `PasswordEncoder` для проверки пароля.

Упрощенная схема:

```text
HTTP Request
      |
      v
AuthenticationWebFilter
      |
      v
ReactiveAuthenticationManager
      |
      +--> ReactiveUserDetailsService
      |
      +--> PasswordEncoder
```

После успешной аутентификации объект `Authentication` сохраняется в реактивном security context.

---

# HelloController

В твоем проекте два варианта получения `Authentication` объединены в одном контроллере: первый вариант закомментирован, второй активен.

```java
package com.laurentiuspilca.ssia.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HelloController {

//    @GetMapping("/hello")
//    public Mono<String> hello(Mono<Authentication> auth) {
//        Mono<String> message = auth.map(a -> "Hello " + a.getName());
//        return message;
//    }

    @GetMapping("/hello")
    public Mono<String> hello() {
        Mono<String> message = ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> "Hello from Context " + auth.getName());
        return message;
    }
}
```

---

# Вариант 1: Authentication через параметр метода

Первый способ из главы — попросить Spring Security внедрить authentication как параметр метода.

```java
@GetMapping("/hello")
public Mono<String> hello(Mono<Authentication> auth) {
    Mono<String> message = auth.map(a -> "Hello " + a.getName());
    return message;
}
```

Важно: в реактивном приложении параметр имеет тип:

```java
Mono<Authentication>
```

а не просто:

```java
Authentication
```

---

# Как проверить вариант 1

В файле:

```text
ssia-ch17-ex2/src/main/java/com/laurentiuspilca/ssia/controllers/HelloController.java
```

нужно:

1. раскомментировать метод:

```java
public Mono<String> hello(Mono<Authentication> auth)
```

2. закомментировать метод:

```java
public Mono<String> hello()
```

После этого перезапустить приложение.

Проверка:

```bash
curl -u john:12345 http://localhost:8080/hello
```

Ожидаемый ответ:

```text
Hello john
```

---

# Вариант 2: Authentication через ReactiveSecurityContextHolder

Второй способ — получить security context напрямую из:

```java
ReactiveSecurityContextHolder
```

Активный вариант в проекте:

```java
@GetMapping("/hello")
public Mono<String> hello() {
    Mono<String> message = ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication())
            .map(auth -> "Hello from Context " + auth.getName());
    return message;
}
```

Здесь происходит цепочка:

```text
ReactiveSecurityContextHolder
        |
        v
Mono<SecurityContext>
        |
        v
Authentication
        |
        v
String response
```

---

# Как проверить вариант 2

В файле `HelloController.java` нужно оставить активным метод:

```java
public Mono<String> hello()
```

и закомментировать метод:

```java
public Mono<String> hello(Mono<Authentication> auth)
```

После этого перезапустить приложение.

Проверка:

```bash
curl -u john:12345 http://localhost:8080/hello
```

Ожидаемый ответ:

```text
Hello from Context john
```

---

# Проверка неверных credentials

Если указать неправильный пароль:

```bash
curl -i -u john:wrong http://localhost:8080/hello
```

Ожидаемый результат:

```text
HTTP/1.1 401 Unauthorized
```

---

# Почему Basic Authentication работает без явного SecurityFilterChain

После добавления `spring-boot-starter-security` Spring Boot автоматически включает security auto-configuration.

Так как в контексте есть:

```java
ReactiveUserDetailsService
```

Spring Security может настроить basic authentication для WebFlux-приложения.

---

# ReactiveSecurityContextHolder vs SecurityContextHolder

В servlet-приложениях обычно используется:

```java
SecurityContextHolder
```

Он опирается на `ThreadLocal`.

В реактивных приложениях используется:

```java
ReactiveSecurityContextHolder
```

Он работает с reactive context, а не с thread-local storage.

---

# Запуск проекта

Из корня проекта:

```bash
mvn spring-boot:run
```

---

# Итог

Глава 17.2 показывает, что в реактивных приложениях Spring Security использует другие контракты и другой способ хранения security context.

Главные отличия:

- `UserDetailsService` заменяется на `ReactiveUserDetailsService`;
- `SecurityContextHolder` заменяется на `ReactiveSecurityContextHolder`;
- `Authentication` часто передается как `Mono<Authentication>`;
- security context больше не хранится через `ThreadLocal`;
- аутентификация выполняется через `AuthenticationWebFilter` и `ReactiveAuthenticationManager`.
