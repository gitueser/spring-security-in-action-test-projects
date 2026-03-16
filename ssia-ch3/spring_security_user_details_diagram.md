# Компоненты Spring Security для управления пользователями

## Схема

```text
UserDetailsService ───────► UserDetails ───────► GrantedAuthority
        ▲                        │
        │                        │
        │                        ▼
   UserDetailsManager ───────────
```

---

## Пояснение

### UserDetailsService

Интерфейс, который отвечает за загрузку данных пользователя.

Типичный метод:

```java
UserDetails loadUserByUsername(String username)
```

Spring Security вызывает этот метод во время аутентификации, чтобы получить информацию о пользователе по его имени.

---

### UserDetails

Интерфейс, который представляет пользователя внутри Spring Security.

Обычно содержит:

- username
- password (уже в виде хеша)
- authorities (роли / права)
- состояние учётной записи: активна ли она, не заблокирована ли, не истёк ли срок действия

Пример:

```java
UserDetails user =
    User.withUsername("user")
        .password("{bcrypt}$2a$10$...")
        .roles("USER")
        .build();
```

---

### GrantedAuthority

Интерфейс, который представляет **роль или полномочие**, выданное пользователю.

Примеры:

- `ROLE_USER`
- `ROLE_ADMIN`
- `READ_PRIVILEGES`
- `WRITE_PRIVILEGES`

Пример создания:

```java
new SimpleGrantedAuthority("ROLE_ADMIN")
```

У одного пользователя может быть **одно или несколько полномочий**.

---

### UserDetailsManager

Это расширение `UserDetailsService`, которое не только загружает пользователя, но и позволяет **управлять пользователями**.

Дополнительные возможности:

- createUser
- updateUser
- deleteUser
- changePassword

Пример:

```java
UserDetailsManager manager = new InMemoryUserDetailsManager();

manager.createUser(
    User.withUsername("admin")
        .password("{noop}password")
        .roles("ADMIN")
        .build()
);
```

---

## Коротко о связях между компонентами

| Компонент | Назначение |
|---|---|
| UserDetailsService | Загружает пользователя по username |
| UserDetails | Представляет пользователя внутри Spring Security |
| GrantedAuthority | Представляет роли и права пользователя |
| UserDetailsManager | Управляет учётными записями пользователей |

---

## Упрощённый поток аутентификации

```text
запрос на логин
      ↓
UserDetailsService.loadUserByUsername()
      ↓
UserDetails
      ↓
GrantedAuthority (роли / права)
      ↓
создаётся Authentication
```

---

## Как это проще запомнить

- `UserDetailsService` = сервис, который ищет пользователя
- `UserDetails` = объект пользователя
- `GrantedAuthority` = роли и права пользователя
- `UserDetailsManager` = сервис, который умеет не только искать, но и изменять пользователей
