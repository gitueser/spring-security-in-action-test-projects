# Глава 18.8 — Тестирование Spring Security в реактивных приложениях

## Описание

В этой главе рассматривается тестирование Spring Security в реактивных приложениях.

Для реактивных приложений Spring Security предоставляет специальные инструменты тестирования, потому что:

- MockMvc используется только для servlet-based приложений;
- WebFlux использует реактивный стек;
- для WebFlux применяется WebTestClient.

---

# Основные подходы тестирования

В главе рассматриваются два подхода:

1. Использование `@WithMockUser`
2. Использование `WebTestClientConfigurer`

---

# Что используется вместо MockMvc

Для реактивных приложений используется:

```java
WebTestClient
```

а не:

```java
MockMvc
```

---

# Maven зависимости

## Основные зависимости

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

## Тестовые зависимости

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

# Автоматическая настройка WebTestClient

Для реактивных тестов используется:

```java
@AutoConfigureWebTestClient
```

Пример:

```java
@SpringBootTest
@AutoConfigureWebTestClient
class MainTests {
}
```

---

# Внедрение WebTestClient

```java
@Autowired
private WebTestClient client;
```

Spring Boot автоматически создает и конфигурирует WebTestClient.

---

# Тестирование с @WithMockUser

## Пример теста

```java
@SpringBootTest
@AutoConfigureWebTestClient
class MainTests {

    @Autowired
    private WebTestClient client;

    @Test
    @WithMockUser
    void testCallHelloWithValidUser() {

        client.get()
                .uri("/hello")
                .exchange()
                .expectStatus().isOk();
    }
}
```

---

# Как работает @WithMockUser

Аннотация:

```java
@WithMockUser
```

создает тестовый SecurityContext.

Spring Security:

- НЕ выполняет реальную аутентификацию;
- НЕ вызывает UserDetailsService;
- использует фиктивного пользователя.

---

# Что делает exchange()

```java
.exchange()
```

Выполняет реактивный HTTP request.

После этого можно проверять:

- status;
- headers;
- body;
- cookies;
- response content.

---

# Проверка HTTP status

```java
.expectStatus().isOk();
```

Проверяет:

```text
HTTP 200 OK
```

---

# Второй подход — mutateWith()

В реактивных приложениях можно изменять тестовый context через:

```java
mutateWith(...)
```

---

# Тест с mockUser()

```java
@SpringBootTest
@AutoConfigureWebTestClient
class MainTests {

    @Autowired
    private WebTestClient client;

    @Test
    void testCallHelloWithValidUserWithMockUser() {

        client.mutateWith(mockUser())
                .get()
                .uri("/hello")
                .exchange()
                .expectStatus().isOk();
    }
}
```

---

# Что делает mutateWith(mockUser())

```java
client.mutateWith(mockUser())
```

добавляет фиктивного пользователя в SecurityContext для конкретного request.

Это аналог:

```java
.with(user(...))
```

из MockMvc.

---

# Разница между @WithMockUser и mutateWith(mockUser())

## @WithMockUser

Работает на уровне тестового метода:

```java
@WithMockUser
```

---

## mutateWith(mockUser())

Работает на уровне конкретного request:

```java
client.mutateWith(mockUser())
```

---

# Тестирование CSRF в реактивных приложениях

Для CSRF используется:

```java
mutateWith(csrf())
```

Пример:

```java
client.mutateWith(csrf())
        .post()
        .uri("/hello")
        .exchange()
        .expectStatus().isOk();
```

---

# Что делает csrf()

```java
csrf()
```

добавляет тестовый CSRF token в request.

Это реактивный аналог:

```java
.with(csrf())
```

из MockMvc.

---

# Важное отличие от servlet приложений

## Servlet stack

Использует:

```text
MockMvc
```

---

## Reactive stack

Использует:

```text
WebTestClient
```

---

# Что проверяет эта глава

Глава показывает:

- как тестировать Spring Security в WebFlux;
- как использовать WebTestClient;
- как использовать @WithMockUser;
- как использовать mutateWith(mockUser());
- как тестировать CSRF в реактивных приложениях;
- как проверять status response.

---

# Итог

В этой главе показано:

- как тестировать реактивные приложения;
- как тестировать Spring Security в WebFlux;
- как использовать WebTestClient;
- как использовать @WithMockUser;
- как использовать mutateWith(mockUser());
- как использовать mutateWith(csrf());
- чем тестирование WebFlux отличается от MockMvc.
