# Шпаргалка по PasswordEncoder в Spring Security

## Зачем нужны префиксы вида `{noop}`, `{bcrypt}`, `{pbkdf2}`, `{scrypt}`, `{argon2}`

В Spring Security пароль часто хранится в формате:

```text
{id}encodedPassword
```

Где:

- `id` — идентификатор алгоритма,
- `encodedPassword` — строка пароля или его хеш.

Примеры:

```text
{noop}password
{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
{pbkdf2}5d923b44...
{scrypt}$e0801$...
{argon2}$argon2id$v=19$...
```

Spring Security читает префикс `{id}` и понимает, какой `PasswordEncoder` нужно использовать для проверки пароля.

---

## Как это работает внутри

Spring Security обычно использует `DelegatingPasswordEncoder`.

Идея такая:

```text
{bcrypt}$2a$10$...
   ↓
DelegatingPasswordEncoder
   ↓
BCryptPasswordEncoder
```

То есть:

1. Spring видит префикс, например `{bcrypt}`
2. выбирает соответствующий encoder
3. сравнивает введённый пароль с сохранённым хешем

---

## 1. `{noop}`

### Что означает

`noop` = **no operation**

То есть пароль **не хешируется вообще**.

Пример:

```text
{noop}password
```

Это значит:

- реальный пароль: `password`
- encoder: `NoOpPasswordEncoder`

### Пример кода

```java
@Bean
UserDetailsService userDetailsService() {
    UserDetails user = User.withUsername("user")
            .password("{noop}password")
            .roles("USER")
            .build();

    return new InMemoryUserDetailsManager(user);
}
```

### Когда использовать

Только:

- в учебных примерах,
- в очень простых демо,
- для быстрого теста.

### Когда не использовать

Никогда в production.

Почему: пароль хранится в открытом виде.

---

## 2. `{bcrypt}`

### Что означает

`bcrypt` — один из самых популярных и безопасных алгоритмов хеширования паролей.

Пример:

```text
{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

### Пример encoder bean

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### Пример хеширования пароля

```java
String encoded = passwordEncoder().encode("password");
System.out.println(encoded);
```

### Пример проверки

```java
boolean matches = passwordEncoder().matches("password", encoded);
```

### Когда использовать

Это стандартный и очень хороший выбор для большинства приложений.

---

## 3. `{pbkdf2}`

### Что означает

`PBKDF2` = **Password-Based Key Derivation Function 2**

Это алгоритм, который делает подбор пароля дорогим по времени.

Пример:

```text
{pbkdf2}5d923b44...
```

### Пример encoder bean

```java
@Bean
PasswordEncoder passwordEncoder() {
    return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
}
```

### Пример использования

```java
PasswordEncoder encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

String encoded = encoder.encode("password");
boolean matches = encoder.matches("password", encoded);
```

### Когда использовать

Подходит для production, особенно если в проекте есть требования использовать PBKDF2.

---

## 4. `{scrypt}`

### Что означает

`scrypt` — алгоритм хеширования, который делает атаку дорогой не только по CPU, но и по памяти.

Пример:

```text
{scrypt}$e0801$...
```

### Пример encoder bean

```java
@Bean
PasswordEncoder passwordEncoder() {
    return SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
}
```

### Пример использования

```java
PasswordEncoder encoder = SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();

String encoded = encoder.encode("password");
boolean matches = encoder.matches("password", encoded);
```

### Когда использовать

Тоже хороший production-вариант.

---

## 5. `{argon2}`

### Что означает

`Argon2` — современный алгоритм хеширования паролей, победитель Password Hashing Competition.

Пример:

```text
{argon2}$argon2id$v=19$...
```

### Пример encoder bean

```java
@Bean
PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
}
```

### Пример использования

```java
PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

String encoded = encoder.encode("password");
boolean matches = encoder.matches("password", encoded);
```

### Когда использовать

Очень хороший современный production-вариант.

---

## Почему без префикса бывает ошибка

Если написать так:

```java
.password("password")
```

Spring Security может выдать ошибку:

```text
There is no PasswordEncoder mapped for id "null"
```

Причина: Spring не понимает, каким алгоритмом проверять пароль.

---

## Как правильно для учебных примеров

Если нужен максимально простой пример:

```java
.password("{noop}password")
```

---

## Как правильно для реального проекта

Лучше использовать `PasswordEncoder` bean, например bcrypt:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

И сохранять в хранилище уже захешированный пароль:

```java
String encodedPassword = passwordEncoder().encode("password");
```

---

## Пример с in-memory user и bcrypt

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

@Bean
UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
    UserDetails user = User.withUsername("user")
            .password(passwordEncoder.encode("password"))
            .roles("USER")
            .build();

    return new InMemoryUserDetailsManager(user);
}
```

---

## Пример с несколькими форматами паролей

Spring может поддерживать несколько форматов сразу.

Например, один пароль хранится так:

```text
{bcrypt}$2a$10$...
```

другой так:

```text
{pbkdf2}abc123...
```

`DelegatingPasswordEncoder` выберет правильный encoder по префиксу.

---

## Как запомнить

- `{noop}` — не хешировать вообще
- `{bcrypt}` — стандартный надёжный выбор
- `{pbkdf2}` — классический безопасный алгоритм
- `{scrypt}` — дорогой по CPU и памяти
- `{argon2}` — современный сильный алгоритм

---

## Практическая рекомендация

Для учебных примеров:

```text
{noop}password
```

Для реальных приложений:

- `BCrypt`
- `Argon2`
- иногда `PBKDF2` или `SCrypt`

---

## Самая короткая шпаргалка

```text
{noop}    -> пароль хранится как есть
{bcrypt}  -> хороший стандартный выбор
{pbkdf2}  -> безопасный классический вариант
{scrypt}  -> усиленная защита за счёт памяти
{argon2}  -> современный сильный вариант
```
