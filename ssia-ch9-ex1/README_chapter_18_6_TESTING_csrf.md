# Глава 18.6 — Тестирование конфигураций CSRF

## Описание

В этой главе рассматривается тестирование CSRF protection в Spring Security.

CSRF расшифровывается как:

```text
Cross-Site Request Forgery
```

То есть межсайтовая подделка запроса.

Если приложение уязвимо к CSRF, злоумышленник может заставить уже аутентифицированного пользователя выполнить нежелательное действие в приложении.

Например:

- отправить форму;
- изменить данные;
- выполнить POST request;
- выполнить PUT request;
- выполнить DELETE request.

---

# Главная идея

Spring Security по умолчанию включает CSRF protection для небезопасных HTTP methods:

```text
POST
PUT
PATCH
DELETE
```

Для таких запросов приложение ожидает валидный CSRF token.

Если токена нет, Spring Security возвращает:

```text
403 Forbidden
```

---

# Проект

Глава использует проект:

```text
ssia-ch9-ex1
```

В нем тестируется endpoint:

```text
POST /hello
```

---

# Что тестируется

В главе проверяются два сценария:

1. POST request без CSRF token возвращает `403 Forbidden`.
2. POST request с CSRF token возвращает `200 OK`.

---

# Maven зависимости

Для тестирования Spring Security нужна зависимость:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Также обычно используется:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

# Тестовый класс

```java
package com.laurentiuspilca.ssia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MainTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void testHelloPOST() throws Exception {
        mvc.perform(post("/hello"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testHelloPOSTWithCSRF() throws Exception {
        mvc.perform(post("/hello").with(csrf()))
                .andExpect(status().isOk());
    }
}
```

---

# Необходимые static imports

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
```

---

# Разбор тестов

## 1. POST request без CSRF token

```java
@Test
void testHelloPOST() throws Exception {
    mvc.perform(post("/hello"))
            .andExpect(status().isForbidden());
}
```

Этот тест проверяет, что endpoint защищен от CSRF.

Так как request выполняется методом:

```text
POST
```

и CSRF token не передан, Spring Security блокирует request.

Ожидаемый статус:

```text
403 Forbidden
```

---

## 2. POST request с CSRF token

```java
@Test
void testHelloPOSTWithCSRF() throws Exception {
    mvc.perform(post("/hello").with(csrf()))
            .andExpect(status().isOk());
}
```

Метод:

```java
csrf()
```

добавляет валидный CSRF token в request.

Поэтому Spring Security пропускает request.

Ожидаемый статус:

```text
200 OK
```

---

# Что делает csrf()

Метод:

```java
csrf()
```

из Spring Security Test создает тестовый CSRF token и добавляет его в request.

Он используется как `RequestPostProcessor`:

```java
post("/hello").with(csrf())
```

---

# Почему GET обычно не требует CSRF token

CSRF protection обычно применяется к methods, которые могут изменять состояние приложения:

```text
POST
PUT
PATCH
DELETE
```

HTTP GET считается безопасным method, потому что он не должен изменять данные.

Поэтому GET-запросы обычно не требуют CSRF token.

---

# Что проверяет эта глава

Глава показывает, как тестировать:

- включенную CSRF protection;
- запрет POST request без token;
- успешный POST request с token;
- использование `csrf()` из Spring Security Test;
- поведение Spring Security для небезопасных HTTP methods.

---

# Итог

В этой главе показано:

- как тестировать CSRF protection;
- как использовать MockMvc для POST request;
- как использовать `csrf()`;
- как проверять `403 Forbidden`;
- как проверять `200 OK`;
- как убедиться, что endpoint защищен от CSRF.
