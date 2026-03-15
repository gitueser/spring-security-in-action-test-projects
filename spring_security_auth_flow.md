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

## Как запомнить

Authentication = кто ты  
Authorization  = можно ли тебе сюда  

401 → мы не знаем, кто ты  
403 → мы знаем, кто ты, но сюда тебе нельзя
