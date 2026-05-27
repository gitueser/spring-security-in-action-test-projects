# Spring Security Reactive Applications: Endpoint Authorization

Этот README относится к первой логической части главы 17.3.1 и проекту:

```text
ssia-ch17-ex3
```

Проект показывает, как настраивать авторизацию на уровне endpoint'ов в реактивном Spring WebFlux приложении.

---

# Главная идея

В servlet-based приложениях мы использовали:

```java
SecurityFilterChain
```

и настраивали endpoint authorization через:

```java
authorizeHttpRequests()
```

В реактивных приложениях используется другой контракт:

```java
SecurityWebFilterChain
```

А вместо `HttpSecurity` используется:

```java
ServerHttpSecurity
```

---

# Servlet vs Reactive Security

## Servlet application

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(c -> c.anyRequest().authenticated());
    return http.build();
}
```

## Reactive application

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http.authorizeExchange(c -> c.anyExchange().authenticated());
    return http.build();
}
```

---

# Основные соответствия

| Servlet stack | Reactive stack |
|---|---|
| `SecurityFilterChain` | `SecurityWebFilterChain` |
| `HttpSecurity` | `ServerHttpSecurity` |
| `authorizeHttpRequests()` | `authorizeExchange()` |
| `requestMatchers()` | `pathMatchers()` |
| `anyRequest()` | `anyExchange()` |

---

# Зависимости Maven

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

# Контроллер

В проекте есть две конечные точки:

```java
@RestController
public class HelloController {

    @GetMapping("/hello")
    public Mono<String> hello(Mono<Authentication> auth) {
        Mono<String> message = auth.map(a -> "Hello " + a.getName());
        return message;
    }

    @GetMapping("/ciao")
    public Mono<String> ciao() {
        return Mono.just("Ciao!");
    }
}
```

---

# Поведение endpoint'ов

## `/hello`

```text
GET /hello
```

Доступен только authenticated user.

## `/ciao`

```text
GET /ciao
```

Доступен всем без authentication.

---

# ReactiveUserDetailsService

В реактивных приложениях используется:

```java
ReactiveUserDetailsService
```

а не:

```java
UserDetailsService
```

Пример:

```java
@Bean
public ReactiveUserDetailsService userDetailsService() {
    var u = User.withUsername("john")
            .password("12345")
            .authorities("read")
            .build();

    return new MapReactiveUserDetailsService(u);
}
```

---

# PasswordEncoder

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return NoOpPasswordEncoder.getInstance();
}
```

В учебном примере используется `NoOpPasswordEncoder`, чтобы пароль `12345` хранился как plain text.

В production так делать нельзя.

---

# SecurityWebFilterChain

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http.httpBasic(Customizer.withDefaults());

    http.authorizeExchange(
            c -> c.pathMatchers(HttpMethod.GET, "/hello")
                    .authenticated()
                    .anyExchange()
                    .permitAll()
    );

    return http.build();
}
```

---

# Что здесь происходит

## Включение HTTP Basic

```java
http.httpBasic(Customizer.withDefaults());
```

Позволяет проверять endpoint через curl:

```bash
curl -u john:12345 http://localhost:8080/hello
```

## Защита `/hello`

```java
.pathMatchers(HttpMethod.GET, "/hello")
.authenticated()
```

GET-запрос на `/hello` доступен только после authentication.

## Разрешение всех остальных запросов

```java
.anyExchange()
.permitAll()
```

Все остальные запросы разрешены без authentication.

Например:

```text
/ciao
```

---

# Проверка работы

## Проверка `/ciao` без credentials

```bash
curl http://localhost:8080/ciao
```

Ожидаемый ответ:

```text
Ciao!
```

## Проверка `/hello` без credentials

```bash
curl -i http://localhost:8080/hello
```

Ожидаемый результат:

```text
HTTP/1.1 401 Unauthorized
```

## Проверка `/hello` с credentials

```bash
curl -u john:12345 http://localhost:8080/hello
```

Ожидаемый ответ:

```text
Hello john
```

---

# AuthorizationWebFilter

После успешной аутентификации в реактивном приложении запрос обрабатывает:

```text
AuthorizationWebFilter
```

Он отвечает за authorization и использует настройки из:

```java
SecurityWebFilterChain
```

---

# ReactiveAuthorizationManager

Для принятия решения об authorization используется:

```java
ReactiveAuthorizationManager
```

В простых случаях Spring Security сам выбирает нужную реализацию на основе DSL-конфигурации:

```java
authenticated()
permitAll()
denyAll()
hasRole()
hasAuthority()
```

---

# Методы авторизации

В реактивной конфигурации доступны знакомые методы:

```java
authenticated()
permitAll()
denyAll()
hasRole()
hasAnyRole()
hasAuthority()
hasAnyAuthority()
access()
```

---

# Когда использовать access()

Метод:

```java
access()
```

дает максимальную гибкость, но делает конфигурацию сложнее.

Его лучше использовать только тогда, когда простых методов недостаточно:

```java
authenticated()
hasRole()
hasAuthority()
```

Для `access()` в главе создан отдельный проект:

```text
ssia-ch17-ex4
```

---

# Итог

Проект `ssia-ch17-ex3` показывает стандартный способ настройки endpoint authorization в реактивных приложениях:

- использовать `SecurityWebFilterChain`;
- использовать `ServerHttpSecurity`;
- настраивать правила через `authorizeExchange()`;
- использовать `pathMatchers()` вместо `requestMatchers()`;
- использовать `anyExchange()` вместо `anyRequest()`.
