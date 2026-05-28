# Глава 18.4 — Тестирование безопасности метода

## Описание

В этой главе рассматривается тестирование безопасности методов Spring Security.

Ранее тестирование выполнялось через HTTP-эндпоинты с использованием MockMvc, однако безопасность в Spring Security может применяться не только на уровне веб-слоя, но и напрямую на уровне сервисов через:

- @PreAuthorize
- @PostAuthorize
- @Secured
- @RolesAllowed

Поэтому методы сервисов также необходимо тестировать отдельно.

---

# Что тестируется

В проекте используется метод:

```java
@PreAuthorize("hasAuthority('write')")
public String getName() {
    return "Fantastico";
}
```

Метод разрешено вызывать только пользователю с authority write.

---

# Что проверяют тесты

Тестируются три сценария:

1. Вызов без аутентификации → AuthenticationException
2. Пользователь с неправильными правами → AccessDeniedException
3. Пользователь с правильными правами → успешный результат

---

# Зависимости Maven

Для тестирования Spring Security требуется зависимость:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

# Тестируемый сервис

## NameService.java

```java
@Service
public class NameService {

    @PreAuthorize("hasAuthority('write')")
    public String getName() {
        return "Fantastico";
    }
}
```

---

# Тестовый класс

## MainTests.java

```java
@SpringBootTest
class MainTests {

    @Autowired
    private NameService nameService;

    @Test
    void testNameServiceWithNoUser() {
        assertThrows(AuthenticationException.class,
                () -> nameService.getName());
    }

    @Test
    @WithMockUser(authorities = "read")
    void testNameServiceWithUserButWrongAuthority() {
        assertThrows(AccessDeniedException.class,
                () -> nameService.getName());
    }

    @Test
    @WithMockUser(authorities = "write")
    void testNameServiceWithUserButCorrectAuthority() {
        var result = nameService.getName();

        assertEquals("Fantastico", result);
    }
}
```

---

# Разбор тестов

## 1. Тест без пользователя

SecurityContext пустой.

Spring Security не находит аутентифицированного пользователя и выбрасывает:

```text
AuthenticationException
```

---

## 2. Пользователь есть, но прав недостаточно

Пользователь аутентифицирован.

Но метод требует:

```java
hasAuthority('write')
```

а у пользователя только:

```text
read
```

Поэтому Spring Security выбрасывает:

```text
AccessDeniedException
```

---

## 3. Пользователь с правильными правами

Пользователь имеет authority write.

Spring Security разрешает вызов метода.

Метод успешно возвращает:

```text
Fantastico
```

---

# Аннотация @WithMockUser

Аннотация:

```java
@WithMockUser
```

создаёт тестовый SecurityContext.

Spring автоматически:

- создаёт Authentication
- помещает его в SecurityContext
- выполняет тест от имени этого пользователя

---

# Отличие от MockMvc тестов

## MockMvc тесты

Тестируют:

- HTTP слой
- контроллеры
- фильтры
- цепочку Spring Security

---

## Method Security тесты

Тестируют:

- сервисный слой
- @PreAuthorize
- @PostAuthorize
- аспектную безопасность методов

HTTP здесь вообще не используется.

---

# Как работает безопасность методов

Spring Security создаёт AOP-аспект.

Когда вызывается метод:

```java
nameService.getName()
```

аспект:

1. перехватывает вызов
2. читает @PreAuthorize
3. проверяет SecurityContext
4. разрешает или запрещает выполнение метода

---

# Итог

В этой главе показано:

- как тестировать Method Security
- как тестировать @PreAuthorize
- как проверять AuthenticationException
- как проверять AccessDeniedException
- как использовать @WithMockUser
- как тестировать сервисы без MockMvc
