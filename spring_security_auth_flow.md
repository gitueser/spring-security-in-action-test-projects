# Схема Spring Security (аутентификация)

## Как работает аутентификация внутри Spring Security

```
HTTP request
      ↓
SecurityFilterChain
      ↓
Authentication Filter
(например BasicAuthenticationFilter /
 UsernamePasswordAuthenticationFilter /
 BearerTokenAuthenticationFilter)
      ↓
AuthenticationManager
      ↓
AuthenticationProvider
      ↓
UserDetailsService
      ↓
UserDetails
      ↓
PasswordEncoder
      ↓
Authentication (authenticated = true)
      ↓
SecurityContextHolder
      ↓
Authorization
      ↓
Controller
```

---

## Поток аутентификации Spring Security (кратко)

```
request
   ↓
SecurityFilterChain
   ↓
AuthenticationFilter
   ↓
AuthenticationManager
   ↓
AuthenticationProvider
   ↓
UserDetailsService
   ↓
UserDetails
   ↓
PasswordEncoder
   ↓
SecurityContextHolder
   ↓
Authorization
   ↓
Controller
```

---

## Что делает каждый компонент

- **SecurityFilterChain** — правила безопасности HTTP
- **AuthenticationFilter** — извлекает credentials из запроса
- **AuthenticationManager** — запускает процесс аутентификации
- **AuthenticationProvider** — проверяет пользователя
- **UserDetailsService** — загружает пользователя
- **PasswordEncoder** — проверяет пароль
- **SecurityContextHolder** — хранит текущего пользователя
- **Authorization** — проверяет роли и права

---

## Мини-примеры кода для каждого компонента

### 1. SecurityFilterChain — правила безопасности HTTP

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/public").permitAll()
            .requestMatchers("/hello").authenticated()
            .anyRequest().denyAll()
        )
        .httpBasic();

    return http.build();
}
```

Что здесь происходит:

- `/public` открыт всем,
- `/hello` требует login,
- всё остальное запрещено.

---

### 2. AuthenticationFilter — извлекает credentials из запроса

Прямо руками этот фильтр обычно не вызывают, но он работает примерно с такими данными:

```http
GET /hello HTTP/1.1
Host: localhost:8080
Authorization: Basic dXNlcjpwYXNzd29yZA==
```

Или с JWT:

```http
GET /api/data HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOi...
```

Что здесь происходит:

- фильтр читает `Authorization` header,
- извлекает логин/пароль или токен,
- создаёт объект `Authentication`.

---

### 3. AuthenticationManager — запускает аутентификацию

```java
Authentication authenticationRequest =
        new UsernamePasswordAuthenticationToken("user", "password");

Authentication authenticationResult =
        authenticationManager.authenticate(authenticationRequest);
```

Что здесь происходит:

- создаётся запрос на аутентификацию,
- `AuthenticationManager` проверяет credentials,
- если всё хорошо, возвращает аутентифицированный объект.

---

### 4. AuthenticationProvider — проверяет пользователя

Пример своей реализации:

```java
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        if ("user".equals(username) && "password".equals(password)) {
            return new UsernamePasswordAuthenticationToken(
                    username,
                    password,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }

        throw new BadCredentialsException("Bad credentials");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

Что здесь происходит:

- provider получает логин и пароль,
- сам решает, валидны они или нет,
- при успехе возвращает `Authentication`,
- при ошибке бросает исключение.

---

### 5. UserDetailsService — загружает пользователя

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

Или через метод:

```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    if (!"user".equals(username)) {
        throw new UsernameNotFoundException("User not found");
    }

    return User.withUsername("user")
            .password("{noop}password")
            .roles("USER")
            .build();
}
```

Что здесь происходит:

- Spring просит загрузить пользователя по username,
- сервис возвращает `UserDetails`.

---

### 6. PasswordEncoder — проверяет пароль

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Проверка выглядит концептуально так:

```java
boolean matches = passwordEncoder.matches("rawPassword", encodedPasswordFromDb);
```

Что здесь происходит:

- пароль в базе хранится в зашифрованном/хешированном виде,
- Spring сравнивает введённый пароль с хешем.

---

### 7. Authentication — паспорт пользователя внутри Spring Security

Пример создания объекта:

```java
Authentication authentication =
        new UsernamePasswordAuthenticationToken(
                "user",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
```

Другие реализации:

- `UsernamePasswordAuthenticationToken`
- `JwtAuthenticationToken`
- `AnonymousAuthenticationToken`
- `RememberMeAuthenticationToken`
- `PreAuthenticatedAuthenticationToken`

Что здесь происходит:

- объект хранит, кто пользователь,
- какие у него роли,
- прошёл ли он аутентификацию.

---

### 8. SecurityContextHolder — хранит текущего пользователя

Получить текущего пользователя можно так:

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String username = authentication.getName();
```

Что здесь происходит:

- Spring достаёт текущий `Authentication`,
- `getName()` обычно возвращает username.

Сохранение вручную выглядит так:

```java
SecurityContextHolder.getContext().setAuthentication(authentication);
```

---

### 9. Authorization — проверяет роли и права

Через конфигурацию:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/admin").hasRole("ADMIN")
            .requestMatchers("/profile").authenticated()
            .anyRequest().permitAll()
        )
        .httpBasic();

    return http.build();
}
```

Через аннотацию:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin")
public String admin() {
    return "admin";
}
```

Что здесь происходит:

- Spring смотрит на роли пользователя,
- если ролей недостаточно, возвращает `403 Forbidden`.

---

### 10. Controller — выполняется только если security пропустил запрос

```java
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }
}
```

Что здесь происходит:

- до контроллера запрос доходит только после authentication и authorization.

---

## Как запомнить

Authentication = кто ты  
Authorization  = можно ли тебе сюда  

401 → мы не знаем, кто ты  
403 → мы знаем, кто ты, но сюда тебе нельзя
