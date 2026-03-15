# Spring Security — короткая шпаргалка

## Как мыслить о Spring Security

Самая короткая формула:

```text
request
  ↓
authenticate
  ↓
authorize
  ↓
controller
```

Или чуть подробнее:

```text
HTTP request
      ↓
SecurityFilterChain
      ↓
Authentication
      ↓
Authorization
      ↓
Controller
      ↓
Response
```

---

## 5 ключевых компонентов

### 1. `SecurityFilterChain`

Отвечает за **правила безопасности HTTP-запросов**.

Именно здесь задаётся:

- какие URL открыты,
- какие требуют аутентификацию,
- используется ли form login,
- используется ли HTTP Basic,
- включён ли CSRF.

Пример:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/hello").authenticated()
            .anyRequest().permitAll()
        )
        .httpBasic(Customizer.withDefaults())
        .formLogin(Customizer.withDefaults());

    return http.build();
}
```

Как это запоминать:

> `SecurityFilterChain` = правила охраны на входе в приложение.

---

### 2. `Authentication`

Это объект, который представляет **текущего пользователя** внутри Spring Security.

Обычно содержит:

- кто пользователь (`principal`),
- его credentials (пароль, токен и т. п.),
- роли / права (`authorities`),
- флаг `authenticated`.

Как это запоминать:

> `Authentication` = паспорт пользователя внутри Spring Security.

Примеры реализаций:

- `UsernamePasswordAuthenticationToken`
- `JwtAuthenticationToken`
- `AnonymousAuthenticationToken`
- `RememberMeAuthenticationToken`
- `PreAuthenticatedAuthenticationToken`
- `TestingAuthenticationToken`

То есть `Authentication` — это интерфейс, а конкретная реализация зависит от способа аутентификации.

Например:

- логин/пароль → `UsernamePasswordAuthenticationToken`
- JWT → `JwtAuthenticationToken`
- anonymous user → `AnonymousAuthenticationToken`

---

### 3. `SecurityContextHolder`

Здесь Spring Security хранит текущий `Authentication`.

Как это запоминать:

> `SecurityContextHolder` = место, где лежит паспорт текущего пользователя.

Пример получения текущего пользователя:

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String username = authentication.getName();
```

Здесь:

- `getAuthentication()` возвращает объект текущего пользователя,
- `getName()` обычно возвращает username.

---

### 4. `AuthenticationManager`

Это координатор процесса аутентификации.

Его задача — принять объект `Authentication` и проверить, валиден ли он.

Ключевой метод:

```java
authenticate(authentication)
```

Как это запоминать:

> `AuthenticationManager` = главный проверяющий на входе.

---

### 5. `UserDetailsService`

Загружает пользователя по username.

Ключевой метод:

```java
loadUserByUsername(String username)
```

Может читать пользователя:

- из памяти,
- из базы данных,
- из LDAP,
- из внешнего сервиса.

Как это запоминать:

> `UserDetailsService` = справочная служба, которая умеет найти пользователя.

---

## Как всё связано

```text
request
   ↓
SecurityFilterChain
   ↓
AuthenticationFilter
   ↓
AuthenticationManager
   ↓
UserDetailsService
   ↓
UserDetails
   ↓
PasswordEncoder
   ↓
Authentication
   ↓
SecurityContextHolder
   ↓
Authorization
   ↓
Controller
```

---

## 401 и 403

### `401 Unauthorized`

Это означает:

> пользователь **не аутентифицирован**.

Типичные причины:

- не передан логин/пароль,
- нет `Authorization` header,
- нет session,
- истёк токен.

Как запоминать:

> `401` = «мы не знаем, кто ты».

---

### `403 Forbidden`

Это означает:

> пользователь аутентифицирован, **но прав недостаточно**.

Типичные причины:

- у пользователя роль `USER`, а endpoint требует `ADMIN`,
- CSRF заблокировал запрос,
- доступ запрещён правилами авторизации.

Как запоминать:

> `403` = «мы знаем, кто ты, но сюда тебе нельзя».

---

## Быстрая диагностика

```text
endpoint не работает
       ↓
посмотреть status
```

| статус | что это обычно значит |
|---|---|
| 401 | проблема с authentication |
| 403 | проблема с authorization или CSRF |

---

## Мини-шпаргалка `curl` для Spring Security

Только body:

```bash
curl -s http://localhost:8080/hello; echo
```

Заголовки + body:

```bash
curl -s -i http://localhost:8080/hello; echo
```

Красивый JSON:

```bash
curl -s http://localhost:8080/hello | jq
```

Проверка Basic Auth:

```bash
curl -s -u user:password http://localhost:8080/hello; echo
```

Полный HTTP-диалог:

```bash
curl -s -v http://localhost:8080/hello
```

---

## Важный момент про `jq` и `python -m json.tool`

Эти инструменты умеют форматировать **только JSON body**.

Поэтому работает так:

```bash
curl -s http://localhost:8080/hello | jq
```

Но вот так уже не сработает:

```bash
curl -s -i http://localhost:8080/hello | jq
```

Почему: `-i` добавляет в поток **HTTP headers**, а это уже не чистый JSON.

То же самое касается:

```bash
curl -s -i http://localhost:8080/hello | python -m json.tool
```

`python -m json.tool` и `jq` ожидают чистый JSON, а получают что-то вроде:

```text
HTTP/1.1 401
Content-Type: application/json
...
{"status":401,...}
```

Это уже невалидный JSON-документ.

---

## Практическое правило

Если хочешь:

- **красивый JSON body** → **без `-i`**
- **увидеть headers** → используй `-i`, но **не** передавай вывод в `jq`

Примеры:

```bash
curl -s http://localhost:8080/hello | jq
curl -s -i http://localhost:8080/hello
curl -s -u user:password http://localhost:8080/hello; echo
curl -s -i -u user:password http://localhost:8080/hello
```

---

## Самая короткая ассоциация для запоминания

- `SecurityFilterChain` = правила охраны
- `Authentication` = паспорт пользователя
- `SecurityContextHolder` = место, где лежит паспорт
- `AuthenticationManager` = главный проверяющий
- `UserDetailsService` = справочная служба по пользователям
- `401` = «не знаем, кто ты»
- `403` = «знаем, кто ты, но нельзя`
