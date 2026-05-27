# Spring Security Reactive Applications: Reactive Method Security

README для проекта ssia-ch17-ex5.

## Основная идея

В reactive приложениях можно использовать безопасность методов через:

```java
@EnableReactiveMethodSecurity
```

и аннотации:

```java
@PreAuthorize
```

---

## Maven зависимости

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

---

## HelloController

```java
@RestController
public class HelloController {

    @GetMapping("/hello")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<String> hello() {
        return Mono.just("Hello!");
    }
}
```

---

## ProjectConfig

```java
@Configuration
@EnableReactiveMethodSecurity
public class ProjectConfig {

    @Bean
    public ReactiveUserDetailsService userDetailsService() {
        var u1 = User.withUsername("john")
                .password("12345")
                .roles("ADMIN")
                .build();

        var u2 = User.withUsername("bill")
                .password("12345")
                .roles("REGULAR_USER")
                .build();

        return new MapReactiveUserDetailsService(u1, u2);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
```

---

## Проверка работы

### John

```bash
curl -u john:12345 http://localhost:8080/hello
```

Ответ:

```text
Hello!
```

---

### Bill

```bash
curl -i -u bill:12345 http://localhost:8080/hello
```

Ответ:

```text
HTTP/1.1 403 Forbidden
```

---

## Что происходит за кадром

Reactive method security использует aspect.

Spring Security:

1. перехватывает вызов метода;
2. проверяет authorization rule;
3. вызывает метод только если authorization passed.

---

## Итог

Проект демонстрирует:

- @EnableReactiveMethodSecurity
- @PreAuthorize
- reactive method security
- authorization на уровне методов
- работу security aspect в WebFlux


---

## Важное замечание из главы

Хотя в примере аннотация:

```java
@PreAuthorize
```

используется непосредственно над endpoint методом controller'а:

```java
@GetMapping("/hello")
@PreAuthorize("hasRole('ADMIN')")
public Mono<String> hello() {
    return Mono.just("Hello!");
}
```

в реальном приложении method security обычно применяется
не только на controller layer.

Spring Security позволяет использовать reactive method security
в любых Spring bean-компонентах:

- service layer;
- business layer;
- proxy layer;
- repository layer;
- utility components.

Например:

```java
@Service
public class PaymentService {

    @PreAuthorize("hasRole('ADMIN')")
    public Mono<String> processPayment() {
        return Mono.just("payment processed");
    }
}
```

Это особенно полезно, потому что authorization rules
можно размещать ближе к business logic,
а не только на уровне HTTP endpoint'ов.

Таким образом:

- endpoint security защищает HTTP layer;
- method security защищает business methods независимо от HTTP.
