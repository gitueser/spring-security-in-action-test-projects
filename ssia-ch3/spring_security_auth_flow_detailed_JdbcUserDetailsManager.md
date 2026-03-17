# Поток аутентификации Spring Security (подробная схема)

## Схема

```text
Client Request
      ↓
AuthenticationFilter
      ↓
AuthenticationManager
      ↓
AuthenticationProvider
      ↓
UserDetailsService (JdbcUserDetailsManager)
      ↓
Database (MySQL)
      ↓
PasswordEncoder
      ↓
Authentication SUCCESS
      ↓
SecurityContext (сохраняется в сессии)
      ↓
Controller (например HelloController)
```

---

# Пошаговое объяснение

## 1. AuthenticationFilter перехватывает запрос

Когда клиент отправляет запрос (например `/login`),  
его первым обрабатывает фильтр аутентификации.

Пример (упрощённо):

```java
UsernamePasswordAuthenticationFilter
```

Он извлекает:

- username
- password

---

## 2. Фильтр передаёт данные в AuthenticationManager

Фильтр создаёт объект:

```java
new UsernamePasswordAuthenticationToken(username, password)
```

и передаёт его в:

```java
AuthenticationManager.authenticate(...)
```

---

## 3. AuthenticationManager делегирует AuthenticationProvider

AuthenticationManager сам не проверяет пользователя.

Он передаёт задачу одному из провайдеров:

```java
AuthenticationProvider
```

---

## 4. AuthenticationProvider вызывает UserDetailsService

Провайдер выполняет:

```java
userDetailsService.loadUserByUsername(username)
```

Обычно используется:

```java
JdbcUserDetailsManager
```

---

## 5. UserDetailsService получает пользователя из БД

Происходит запрос к базе данных (например MySQL):

```sql
SELECT username, password, enabled FROM users WHERE username = ?
```

Возвращается объект:

```java
UserDetails
```

---

## 6. Проверка пароля через PasswordEncoder

Провайдер проверяет пароль:

```java
passwordEncoder.matches(rawPassword, encodedPasswordFromDb)
```

Если совпадает → аутентификация успешна.

---

## 7. Создаётся Authentication и кладётся в SecurityContext

После успеха создаётся объект:

```java
Authentication
```

Он сохраняется в:

```java
SecurityContextHolder.getContext()
```

А также может сохраняться в HTTP-сессии.

---

## 8. Запрос передаётся в контроллер

Теперь пользователь считается аутентифицированным.

Запрос идёт дальше:

```java
HelloController
```

---

# Ключевые компоненты

| Компонент | Роль |
|---|---|
| AuthenticationFilter | перехватывает запрос |
| AuthenticationManager | управляет процессом аутентификации |
| AuthenticationProvider | выполняет проверку |
| UserDetailsService | загружает пользователя |
| PasswordEncoder | проверяет пароль |
| SecurityContext | хранит текущего пользователя |

---

# Как это запомнить (очень просто)

```
запрос
↓
фильтр
↓
менеджер
↓
провайдер
↓
UserDetailsService
↓
БД
↓
проверка пароля
↓
SecurityContext
↓
контроллер
```

---

# Важное замечание

Все компоненты:

- не обязательно конкретные классы
- могут иметь разные реализации

Например:

- UserDetailsService → InMemory / JDBC / Custom
- AuthenticationProvider → DaoAuthenticationProvider / JWT / OAuth2
- PasswordEncoder → bcrypt / scrypt / argon2

Spring Security работает через **интерфейсы и делегирование**.
