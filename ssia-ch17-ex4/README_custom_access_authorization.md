# Spring Security Reactive Applications: Custom Authorization with access()

Этот README относится ко второй логической части главы 17.3.1 и проекту:

```text
ssia-ch17-ex4
```

Проект показывает, как использовать метод:

```java
access()
```

для настройки собственного правила авторизации в реактивном приложении.

---

# Главная идея

В проекте `ssia-ch17-ex3` использовались простые правила:

```java
authenticated()
permitAll()
```

Но иногда этого недостаточно.

Например, нужно разрешить доступ только если одновременно выполняются несколько условий:

```text
user has ROLE_ADMIN
AND
current time is before noon
AND
path is /hello
```

Для таких сценариев используется:

```java
access()
```

---

# Что делает access()

Метод:

```java
access()
```

принимает функцию, которая получает:

```java
Mono<Authentication>
AuthorizationContext
```

и возвращает:

```java
Mono<AuthorizationDecision>
```

---

# AuthorizationDecision

```java
new AuthorizationDecision(true)
```

означает:

```text
request is allowed
```

```java
new AuthorizationDecision(false)
```

означает:

```text
request is denied
```

---

# Важное отличие от первой части

В проекте `ssia-ch17-ex3` у пользователя было authority:

```java
.authorities("read")
```

Но во второй логической части правило проверяет:

```text
ROLE_ADMIN
```

Поэтому для проекта `ssia-ch17-ex4` удобнее создать пользователя так:

```java
.roles("ADMIN")
```

Spring Security автоматически превратит это в authority:

```text
ROLE_ADMIN
```

---

# Логика авторизации

Доступ разрешается только если:

```text
path == /hello
AND
user has ROLE_ADMIN
AND
current time is before noon
```

Все остальные запросы запрещены.

---

# Проверка path

```java
String path = getRequestPath(context);
```

Метод:

```java
getRequestPath(...)
```

извлекает path из `AuthorizationContext`.

---

# Проверка времени

```java
boolean restrictedTime =
        LocalTime.now().isAfter(LocalTime.NOON);
```

Если сейчас после полудня, доступ запрещается.

---

# Проверка роли

```java
private Function<Authentication, Boolean> isAdmin() {
    return authentication ->
            authentication.getAuthorities()
                    .stream()
                    .anyMatch(
                            authority -> authority
                                    .getAuthority()
                                    .equals("ROLE_ADMIN")
                    );
}
```

---

# Проверка работы

## До полудня

```bash
curl -u john:12345 http://localhost:8080/hello
```

Ожидаемый ответ:

```text
Hello john
```

## После полудня

```bash
curl -i -u john:12345 http://localhost:8080/hello
```

Ожидаемый результат:

```text
HTTP/1.1 403 Forbidden
```

## Любой другой endpoint

```bash
curl -i -u john:12345 http://localhost:8080/ciao
```

Ожидаемый результат:

```text
HTTP/1.1 403 Forbidden
```

Потому что для всех path, кроме `/hello`, возвращается:

```java
new AuthorizationDecision(false)
```

---

# Что происходит за кадром

После authentication запрос обрабатывает:

```text
AuthorizationWebFilter
```

Он делегирует authorization в:

```text
ReactiveAuthorizationManager
```

При использовании `access()` ты сам задаешь функцию,
которая возвращает authorization decision.

---

# access() vs hasRole()

## hasRole()

```java
.hasRole("ADMIN")
```

Подходит для простых проверок.

## access()

```java
.access(this::getAuthorizationDecisionMono)
```

Подходит для сложных правил:

- роль + время;
- роль + path;
- роль + request header;
- role + query parameter;
- custom business rule.

---

# Итог

Проект `ssia-ch17-ex4` показывает, как в реактивном Spring Security приложении использовать `access()` для кастомной авторизации.

Главные компоненты:

- `SecurityWebFilterChain`
- `ServerHttpSecurity`
- `AuthorizationContext`
- `AuthorizationDecision`
- `Mono<Authentication>`
- `ReactiveAuthorizationManager`
