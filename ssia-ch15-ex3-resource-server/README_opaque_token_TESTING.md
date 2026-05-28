# Глава 18.5 — Тестирование OAuth2 Resource Server с opaque token

## Описание

В этой части главы рассматривается тестирование OAuth2 Resource Server
с использованием непрозрачных токенов (opaque tokens).

Spring Security позволяет тестировать:

- opaque token authentication
- OAuth2 Resource Server
- SecurityContext
- authorities внутри opaque token
- защищенные endpoint

без:

- реального Authorization Server
- реального opaque token
- introspection endpoint
- запуска внешней инфраструктуры

Проект основан на примере:

```text
ssia-ch15-ex3
```

---

# Что такое opaque token

Opaque token — это непрозрачный access token.

В отличие от JWT:

- opaque token НЕ содержит данные пользователя внутри себя
- Resource Server НЕ может прочитать содержимое токена
- сервер должен выполнять introspection request
  к Authorization Server

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MainTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void demoEndpointSuccessfulAuthenticationTest() throws Exception {
        mvc.perform(get("/demo").with(opaqueToken()))
                .andExpect(status().isOk());
    }
}
```

---

# Что делает opaqueToken()

Метод:

```java
opaqueToken()
```

из:

```java
SecurityMockMvcRequestPostProcessors.opaqueToken()
```

создает фиктивный opaque token
для тестирования OAuth2 Resource Server.

Spring Security:

- создает Authentication
- помещает Authentication в SecurityContext
- считает запрос аутентифицированным

---

# Важный момент

В тесте НЕ используется:

- реальный opaque token
- introspection endpoint
- Authorization Server
- проверка токена через сеть

Все происходит внутри тестового SecurityContext.

---

# Что проверяет тест

## demoEndpointSuccessfulAuthenticationTest()

```java
mvc.perform(get("/demo").with(opaqueToken()))
        .andExpect(status().isOk());
```

Проверяется:

- endpoint доступен
- opaque token authentication успешна
- Resource Server считает пользователя authenticated
- запрос проходит SecurityFilterChain

---

# Настройка authorities внутри opaque token

Spring Security позволяет задавать authorities
для фиктивного opaque token.

Пример:

```java
.with(
    opaqueToken().authorities(() -> "read")
)
```

Это позволяет тестировать:

- hasAuthority()
- hasRole()
- @PreAuthorize()
- access()
- SecurityFilterChain authorization rules

---

# Пример теста с authority

```java
@Test
void demoEndpointWithAuthorityTest() throws Exception {
    mvc.perform(
            get("/demo")
                    .with(
                            opaqueToken()
                                    .authorities(() -> "read")
                    )
    )
    .andExpect(status().isOk());
}
```

---

# MockMvc

MockMvc позволяет:

- тестировать Spring MVC
- тестировать Spring Security
- НЕ запускать реальный сервер
- выполнять HTTP-запросы внутри тестов

---

# Необходимые static imports

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
```

---

# Разница между jwt() и opaqueToken()

## jwt()

Используется для:

```text
JWT Resource Server
```

Spring Security создает:

```text
JwtAuthenticationToken
```

---

## opaqueToken()

Используется для:

```text
Opaque Token Resource Server
```

Spring Security создает:

```text
BearerTokenAuthentication
```

---

# Что проверяет глава

Глава показывает:

- тестирование OAuth2 Resource Server
- тестирование opaque token authentication
- использование opaqueToken()
- создание фиктивного opaque token
- настройку authorities внутри opaque token
- тестирование SecurityContext
- тестирование защищенных endpoint

---

# Итог

В этой главе показано:

- как тестировать OAuth2 Resource Server с opaque token
- как использовать SecurityMockMvcRequestPostProcessors.opaqueToken()
- как тестировать protected endpoints
- как задавать authorities внутри opaque token
- как использовать MockMvc вместе со Spring Security
- как тестировать authentication без реального Authorization Server
