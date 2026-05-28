# Глава 18.5 — Тестирование formLogin() и Authentication Handlers

## Описание

В этой главе тестируется аутентификация Spring Security при использовании:

- formLogin()
- AuthenticationSuccessHandler
- AuthenticationFailureHandler
- MockMvc
- Spring Security Test

Проект основан на примере `ssia-ch6-ex4`.

---

# Что тестируется

В проекте проверяются следующие сценарии:

1. Аутентификация с неправильными учетными данными
2. Аутентификация с правильными учетными данными и правильными authority
3. Аутентификация с правильными учетными данными, но неправильными authority

---

# Используемые компоненты Spring Security

## AuthenticationFailureHandler

При неуспешной аутентификации:

- пользователь НЕ аутентифицируется
- в HTTP-response добавляется заголовок:

```text
failed
```

---

## AuthenticationSuccessHandler

После успешной аутентификации приложение:

- перенаправляет пользователя на `/home`,
  если у него есть authority `read`
- перенаправляет пользователя на `/error`,
  если authority отсутствует

---

# Maven зависимости

Для тестирования Spring Security требуется:

```xml
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
@SpringBootTest
@AutoConfigureMockMvc
class MainTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void loggingInWithWrongUser() throws Exception {
        mvc.perform(formLogin()
                        .user("wronguser")
                        .password("12345")
                )
                .andExpect(header().exists("failed"))
                .andExpect(unauthenticated());
    }

    @Test
    void loggingInWithCorrectAuthority() throws Exception {
        mvc.perform(formLogin()
                        .user("john")
                        .password("12345")
                )
                .andExpect(redirectedUrl("/home"))
                .andExpect(status().isFound())
                .andExpect(authenticated());
    }

    @Test
    void loggingInWithWrongAuthority() throws Exception {
        mvc.perform(formLogin()
                        .user("bill")
                        .password("12345")
                )
                .andExpect(redirectedUrl("/error"))
                .andExpect(status().isFound())
                .andExpect(authenticated());
    }
}
```

---

# Что делает formLogin()

Метод:

```java
formLogin()
```

из Spring Security Test:

```java
SecurityMockMvcRequestBuilders.formLogin()
```

имитирует отправку формы логина.

Spring Security автоматически выполняет POST-запрос на:

```text
/login
```

с параметрами:

```text
username
password
```

---

# Разбор тестов

## 1. Неправильный пользователь

```java
void loggingInWithWrongUser()
```

Проверяется:

- аутентификация НЕ прошла
- пользователь остался unauthenticated
- появился заголовок failed

Проверки:

```java
.andExpect(header().exists("failed"))
.andExpect(unauthenticated())
```

---

## 2. Пользователь с правильными правами

```java
void loggingInWithCorrectAuthority()
```

Проверяется:

- аутентификация успешна
- пользователь authenticated
- произошел redirect на `/home`

Проверки:

```java
.andExpect(redirectedUrl("/home"))
.andExpect(status().isFound())
.andExpect(authenticated())
```

---

## 3. Пользователь с неправильными authority

```java
void loggingInWithWrongAuthority()
```

Проверяется:

- пользователь успешно аутентифицирован
- пароль корректный
- AuthenticationSuccessHandler выполнился
- authority пользователя НЕ подходят
- произошел redirect на `/error`

Проверки:

```java
.andExpect(redirectedUrl("/error"))
.andExpect(status().isFound())
.andExpect(authenticated())
```

---

# Важный момент

Третий тест НЕ является ошибкой authentication.

Authentication прошла успешно,
но логика внутри AuthenticationSuccessHandler
определила, что authority пользователя недостаточны.

Поэтому:

```text
/error
```

используется как redirect URL.

При этом пользователь всё равно считается:

```text
authenticated
```

---

# Как работает тестирование

## MockMvc

MockMvc позволяет:

- тестировать Spring MVC
- тестировать Spring Security
- НЕ запускать реальный сервер

---

## authenticated()

Проверяет, что пользователь аутентифицирован.

---

## unauthenticated()

Проверяет, что пользователь НЕ аутентифицирован.

---

## redirectedUrl()

Проверяет redirect после AuthenticationSuccessHandler.

---

# Что проверяет глава

Глава показывает, как тестировать:

- formLogin()
- AuthenticationSuccessHandler
- AuthenticationFailureHandler
- redirect после логина
- authenticated/unauthenticated состояния
- HTTP headers после аутентификации

---

# Итог

В этой главе показано:

- как тестировать formLogin()
- как тестировать redirect после аутентификации
- как тестировать AuthenticationSuccessHandler
- как тестировать AuthenticationFailureHandler
- как использовать MockMvc вместе со Spring Security
- как проверять authenticated() и unauthenticated()
