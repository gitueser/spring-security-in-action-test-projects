# Глава 18.7 — Тестирование конфигураций CORS

## Описание

В этой главе рассматривается тестирование CORS configuration в Spring Security.

CORS расшифровывается как:

```text
Cross-Origin Resource Sharing
```

CORS используется, когда frontend и backend работают с разных origins.

Например:

```text
Frontend:
http://example.com

Backend:
http://example.org
```

По умолчанию браузер блокирует такие запросы.

Spring Security позволяет явно настроить разрешенные origins, methods и headers.

---

# Проект

Глава использует проект:

```text
ssia-ch10-ex1
```

---

# Что тестируется

В этой главе проверяется:

- корректность CORS response headers;
- поддержка preflight OPTIONS request;
- наличие Access-Control-Allow-Origin;
- наличие Access-Control-Allow-Methods.

---

# Maven зависимости

Для тестирования используются:

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

# CORS конфигурация

## ProjectConfig.java

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http.cors(corsConfigurer -> {
        CorsConfigurationSource source = request -> {
            CorsConfiguration config = new CorsConfiguration();

            config.setAllowedOrigins(List.of(
                    "http://localhost:8080",
                    "example.com",
                    "example.org"
            ));

            config.setAllowedMethods(List.of(
                    "GET",
                    "POST",
                    "PUT",
                    "DELETE"
            ));

            config.setAllowedHeaders(List.of("*"));

            return config;
        };

        corsConfigurer.configurationSource(source);
    });

    http.csrf(c -> c.disable());

    http.authorizeHttpRequests(
            c -> c.anyRequest().permitAll()
    );

    return http.build();
}
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MainTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void testCORSForTestEndpoint() throws Exception {

        mvc.perform(options("/test")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Origin", "example.com")
                )
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "example.com"
                ))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().string(
                        "Access-Control-Allow-Methods",
                        "GET,POST,PUT,DELETE"
                ))
                .andExpect(status().isOk());
    }
}
```

---

# Необходимые static imports

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
```

---

# Что делает OPTIONS request

Для проверки CORS браузер сначала выполняет preflight request:

```text
OPTIONS /test
```

Этот request спрашивает backend:

```text
Можно ли отправить POST request с другого origin?
```

Backend отвечает специальными CORS headers.

---

# Что проверяет тест

## Access-Control-Allow-Origin

```java
.andExpect(header().string(
        "Access-Control-Allow-Origin",
        "example.com"
))
```

Проверяет, что backend разрешил origin:

```text
example.com
```

---

## Access-Control-Allow-Methods

```java
.andExpect(header().string(
        "Access-Control-Allow-Methods",
        "GET,POST,PUT,DELETE"
))
```

Проверяет разрешенные HTTP methods.

---

# Почему в ответе НЕ '*'

В книге использовался вариант:

```java
.andExpect(header().string(
        "Access-Control-Allow-Origin",
        "*"
))
```

Но в текущем проекте используется:

```java
config.setAllowedOrigins(...)
```

с конкретными origins.

Поэтому Spring Security возвращает:

```text
Access-Control-Allow-Origin: example.com
```

а не wildcard:

```text
*
```

---

# Почему в ответе несколько methods

В книге проверялся только:

```text
POST
```

Но в текущем проекте разрешены:

```text
GET
POST
PUT
DELETE
```

Поэтому Spring Security возвращает:

```text
GET,POST,PUT,DELETE
```

---

# Что проверяет эта глава

Глава показывает:

- как тестировать CORS;
- как выполнять preflight OPTIONS request;
- как проверять CORS headers;
- как тестировать Access-Control-Allow-Origin;
- как тестировать Access-Control-Allow-Methods;
- как использовать MockMvc для тестирования CORS.

---

# Итог

В этой главе показано:

- как тестировать CORS configuration;
- как проверять preflight requests;
- как проверять CORS response headers;
- как тестировать allowed origins;
- как тестировать allowed methods;
- как Spring Security обрабатывает CORS.
