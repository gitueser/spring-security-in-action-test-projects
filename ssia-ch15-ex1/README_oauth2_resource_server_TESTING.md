# Глава 18.5 — Тестирование OAuth2 Resource Server с JWT

## Описание

В этой главе рассматривается тестирование OAuth2 Resource Server
в Spring Security с использованием фиктивных JWT токенов.

Spring Security предоставляет специальные инструменты тестирования,
которые позволяют:

- тестировать Resource Server
- НЕ генерировать реальные JWT
- НЕ запускать Authorization Server
- НЕ отправлять реальные access token
- быстро проверять правила аутентификации и авторизации

Проект основан на примере:

```text
ssia-ch15-ex1
```

---

# Что тестируется

В этой главе тестируется:

- аутентификация OAuth2 Resource Server
- использование фиктивного JWT
- MockMvc + Spring Security Test
- успешный доступ к защищенному endpoint
- настройка authorities внутри JWT

---

# Maven зависимости

## Основные зависимости

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
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

---

## Зависимости для тестирования

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

# Тестовый класс

## MainTests.java

```java
package com.laurentiuspilca.ssia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MainTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void demoEndpointSuccessfulAuthenticationTest() throws Exception {
        mvc.perform(get("/demo").with(jwt()))
                .andExpect(status().isOk());
    }
}
```

---

# Что делает jwt()

Метод:

```java
jwt()
```

из:

```java
SecurityMockMvcRequestPostProcessors.jwt()
```

создает фиктивный JWT token для тестирования.

Spring Security:

- считает запрос аутентифицированным
- автоматически создает JwtAuthenticationToken
- помещает Authentication в SecurityContext

---

# Важный момент

В тесте НЕ используется:

- реальный JWT
- реальный Authorization Server
- реальная подпись токена
- реальный JWK endpoint

Все выполняется внутри тестового SecurityContext.

---

# Что проверяет тест

## demoEndpointSuccessfulAuthenticationTest()

```java
mvc.perform(get("/demo").with(jwt()))
        .andExpect(status().isOk());
```

Проверяется:

- endpoint доступен
- JWT аутентификация успешна
- Spring Security считает пользователя authenticated
- сервер ресурсов пропускает запрос

---

# Что делает with(jwt())

Метод:

```java
.with(jwt())
```

добавляет к запросу:

```text
JwtAuthenticationToken
```

как будто запрос пришел с реальным Bearer Token.

---

# Настройка authorities внутри JWT

Spring Security позволяет задавать authorities
для фиктивного JWT.

Пример:

```java
.with(
    jwt().authorities(() -> "read")
)
```

Это полезно для тестирования:

- hasAuthority()
- hasRole()
- @PreAuthorize()
- access()
- SecurityFilterChain правил

---

# Импорты для MockMvc тестов

Для работы теста требуются static imports:

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
```

---

# MockMvc

MockMvc позволяет:

- тестировать Spring MVC
- тестировать Spring Security
- НЕ запускать настоящий сервер
- выполнять HTTP-запросы внутри тестов

---

# Что проверяет глава

Глава показывает:

- как тестировать OAuth2 Resource Server
- как использовать jwt() в тестах
- как создавать фиктивные JWT
- как тестировать Resource Server без Authorization Server
- как использовать MockMvc со Spring Security
- как задавать authorities внутри JWT

---

# Итог

В этой главе показано:

- тестирование OAuth2 Resource Server
- использование SecurityMockMvcRequestPostProcessors.jwt()
- работа MockMvc со Spring Security
- создание фиктивного JWT
- тестирование JWT authentication
- тестирование защищенных endpoint
