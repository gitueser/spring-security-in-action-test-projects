# Компоненты Spring Security для управления пользователями

## Схема (логическая зависимость интерфейсов)

```text
UserDetailsService ───────► UserDetails ───────► GrantedAuthority
        ▲                        │
        │                        │
        │                        ▼
   UserDetailsManager ───────────
```

> Важно: все показанные элементы — **контракты (интерфейсы)**.  
> Они **не хранят данные сами по себе**, а только **описывают методы**, которые должны реализовать классы.

---

# Что означает каждый интерфейс

## UserDetailsService

Интерфейс, который определяет **способ загрузки пользователя**.

Spring Security использует его во время аутентификации.

Метод интерфейса:

```java
UserDetails loadUserByUsername(String username);
```

Что происходит:

1. пользователь вводит логин
2. Spring Security вызывает `loadUserByUsername()`
3. реализация ищет пользователя (например в БД)
4. возвращает объект `UserDetails`

Сам интерфейс **ничего не хранит**.

Хранение данных выполняет **реализация**.

Примеры реализаций:

- `InMemoryUserDetailsManager`
- `JdbcUserDetailsManager`
- собственная реализация через БД

---

## UserDetails

Интерфейс, который **описывает пользователя внутри Spring Security**.

Он определяет методы доступа к данным пользователя:

```java
String getUsername();
String getPassword();
Collection<? extends GrantedAuthority> getAuthorities();
boolean isAccountNonLocked();
boolean isEnabled();
```

Важно:

Интерфейс **не хранит данные**.

Данные хранятся в **классе, который реализует этот интерфейс**.

Например:

```java
org.springframework.security.core.userdetails.User
```

Упрощённо:

```java
public class User implements UserDetails {

    private String username;
    private String password;
    private Collection<GrantedAuthority> authorities;

}
```

То есть:

```
UserDetails = контракт
User = реальный объект с данными
```

---

## GrantedAuthority

Интерфейс, который описывает **роль или разрешение пользователя**.

Метод интерфейса:

```java
String getAuthority();
```

Примеры значений:

```
ROLE_USER
ROLE_ADMIN
READ_PRIVILEGES
WRITE_PRIVILEGES
```

Сам интерфейс **не хранит роль**.

Роль хранится в реализации.

Пример реализации:

```java
SimpleGrantedAuthority
```

Пример:

```java
new SimpleGrantedAuthority("ROLE_ADMIN")
```

У пользователя может быть **несколько ролей**.

---

## UserDetailsManager

Интерфейс, который **расширяет UserDetailsService**.

Он добавляет операции **управления пользователями**.

Дополнительные методы:

```java
void createUser(UserDetails user);
void updateUser(UserDetails user);
void deleteUser(String username);
void changePassword(String oldPassword, String newPassword);
```

Он также **ничего не хранит сам**.

Хранение зависит от реализации.

Примеры реализаций:

- `InMemoryUserDetailsManager`
- `JdbcUserDetailsManager`

---

# Как эти интерфейсы работают вместе

Упрощённый поток аутентификации:

```
HTTP login request
        ↓
UserDetailsService.loadUserByUsername()
        ↓
возвращается объект UserDetails
        ↓
UserDetails содержит роли (GrantedAuthority)
        ↓
Spring Security создаёт Authentication
        ↓
Authentication кладётся в SecurityContext
```

---

# Короткая шпаргалка

| Интерфейс | Что делает | Где хранятся данные |
|---|---|---|
| UserDetailsService | загружает пользователя | в реализации |
| UserDetails | описывает пользователя | в реализации класса |
| GrantedAuthority | описывает роль | в реализации |
| UserDetailsManager | управляет пользователями | в реализации |

---

# Простая аналогия

```
UserDetailsService = сервис поиска пользователя
UserDetails = описание пользователя
GrantedAuthority = роль пользователя
UserDetailsManager = сервис управления пользователями
```

Все они — **контракты**, а реальные данные и логика находятся в **классах‑реализациях**.
