# Spring Security OAuth2 Login: Authentication and Authorization

Этот README относится к главе 16.1.5 и объясняет, как использовать данные аутентификации после успешного OAuth2 Login.

Главная идея главы:

```text
oauth2Login() интегрируется в стандартный механизм аутентификации Spring Security.
```

После успешного OAuth2 Login объект Authentication помещается в SecurityContext точно так же, как при:

- formLogin()
- httpBasic()
- oauth2ResourceServer()

Это означает, что вся инфраструктура Spring Security продолжает работать одинаково независимо от способа аутентификации.

---

# Что происходит после OAuth2 Login

Когда пользователь успешно проходит OAuth2 Login:

1. Spring Security получает authorization code;
2. обменивает code на access token;
3. получает информацию о пользователе;
4. создает объект Authentication;
5. помещает Authentication в SecurityContext.

После этого приложение может:

- узнать, кто вошел в систему;
- проверить authorities;
- применять authorization rules;
- отображать персонализированный UI;
- ограничивать доступ к endpoints;
- использовать @PreAuthorize и @PostAuthorize.

---

# Процесс аутентификации Spring Security

Ниже показан общий flow аутентификации Spring Security, который одинаково работает и для OAuth2 Login.

```text
┌────────────────────────────┐
│ 1. HTTP Request            │
└────────────┬───────────────┘
             │
             ▼
┌────────────────────────────┐
│ Authentication Filter      │
│ (OAuth2 Login Filter)      │
└────────────┬───────────────┘
             │
             ▼
┌────────────────────────────┐
│ AuthenticationManager      │
└────────────┬───────────────┘
             │
             ▼
┌────────────────────────────┐
│ AuthenticationProvider     │
└────────────┬───────────────┘
             │
             ▼
┌────────────────────────────┐
│ UserInfo / Token Validation│
└────────────┬───────────────┘
             │
             ▼
┌────────────────────────────┐
│ Authentication object      │
│ created successfully       │
└────────────┬───────────────┘
             │
             ▼
┌────────────────────────────┐
│ SecurityContextHolder      │
└────────────────────────────┘
```

---

# Важная мысль главы

OAuth2 Login НЕ создает отдельную модель безопасности.

После успешного login Spring Security работает абсолютно так же, как и раньше.

Главный объект остается тем же:

```java
Authentication
```

И хранится он там же:

```java
SecurityContextHolder
```

---

# Как получить Authentication

Глава показывает несколько способов получения данных аутентификации.

---

# Способ 1: Injection в controller method

Это самый удобный и рекомендуемый способ.

Пример из главы:

```java
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(
            OAuth2AuthenticationToken authentication) {

        // Работа с authentication

        return "index.html";
    }
}
```

Spring Security автоматически внедряет объект authentication в параметр метода.

---

# Что такое OAuth2AuthenticationToken

При OAuth2 Login Spring Security создает:

```java
OAuth2AuthenticationToken
```

Это реализация интерфейса:

```java
Authentication
```

Через него можно получить:

- имя пользователя;
- authorities;
- OAuth2 principal;
- client registration id;
- user attributes.

---

# Получение username

```java
@GetMapping("/")
public String home(OAuth2AuthenticationToken authentication) {

    String username = authentication.getName();

    System.out.println(username);

    return "index.html";
}
```

---

# Получение authorities

```java
@GetMapping("/")
public String home(OAuth2AuthenticationToken authentication) {

    authentication.getAuthorities()
            .forEach(System.out::println);

    return "index.html";
}
```

---

# Получение OAuth2 provider

```java
@GetMapping("/")
public String home(OAuth2AuthenticationToken authentication) {

    String provider =
            authentication.getAuthorizedClientRegistrationId();

    System.out.println(provider);

    return "index.html";
}
```

Например:

```text
google
github
my_authorization_server
```

---

# Получение OAuth2 principal

```java
@GetMapping("/")
public String home(OAuth2AuthenticationToken authentication) {

    OAuth2User user = authentication.getPrincipal();

    return "index.html";
}
```

---

# Получение user attributes

```java
@GetMapping("/")
public String home(OAuth2AuthenticationToken authentication) {

    OAuth2User user = authentication.getPrincipal();

    Map<String, Object> attributes =
            user.getAttributes();

    System.out.println(attributes);

    return "index.html";
}
```

---

# Примеры attributes

## Google

```json
{
  "sub": "123456",
  "email": "user@gmail.com",
  "name": "John Doe",
  "picture": "https://..."
}
```

## GitHub

```json
{
  "login": "octocat",
  "id": 12345
}
```

---

# Почему лучше использовать Authentication

В книге подчеркивается важная рекомендация.

Лучше использовать:

```java
Authentication
```

а не конкретную реализацию.

Например:

```java
@GetMapping("/")
public String home(Authentication authentication) {

    String username = authentication.getName();

    return "index.html";
}
```

Это уменьшает связанность кода.

---

# Когда нужен OAuth2AuthenticationToken

Конкретную реализацию стоит использовать только тогда, когда нужны OAuth2-specific данные:

- attributes;
- provider id;
- OAuth2 principal;
- OAuth2 claims.

---

# Способ 2: SecurityContextHolder

Authentication можно получить в любом месте приложения.

```java
Authentication authentication =
        SecurityContextHolder
                .getContext()
                .getAuthentication();
```

---

# Пример

```java
Authentication authentication =
        SecurityContextHolder
                .getContext()
                .getAuthentication();

String username = authentication.getName();

System.out.println(username);
```

---

# Где это используется

Такой подход часто используется:

- в service layer;
- в custom security components;
- в audit logging;
- в utility classes;
- в asynchronous processing.

---

# Способ 3: @AuthenticationPrincipal

Spring Security предоставляет annotation:

```java
@AuthenticationPrincipal
```

Пример:

```java
@GetMapping("/")
public String home(
        @AuthenticationPrincipal OAuth2User user) {

    System.out.println(user.getAttributes());

    return "index.html";
}
```

---

# Способ 4: @PreAuthorize

OAuth2 Login полностью интегрирован с method security.

Пример:

```java
@PreAuthorize("hasAuthority('SCOPE_profile')")
public String hello() {
    return "Hello";
}
```

или:

```java
@PreAuthorize("isAuthenticated()")
```

---

# Что хранится в SecurityContext

После OAuth2 Login в SecurityContext обычно хранится:

```java
OAuth2AuthenticationToken
```

Внутри него:

```text
OAuth2User
Authorities
Attributes
Client registration id
Authentication state
```

---

# Как Spring Security создает Authentication

Упрощенный flow:

```text
OAuth2 Login
    ↓
Authorization Code
    ↓
Access Token
    ↓
UserInfo endpoint
    ↓
OAuth2User
    ↓
OAuth2AuthenticationToken
    ↓
SecurityContextHolder
```

---

# Важное отличие OAuth2 Login от Resource Server

## OAuth2 Login

Используется для browser login.

Результат:

```text
HTTP Session + Authentication
```

---

## OAuth2 Resource Server

Используется для API authentication.

Результат:

```text
Bearer token validation
```

Обычно без session.

---

# Authentication одинаковый

Несмотря на разный flow:

- formLogin()
- httpBasic()
- oauth2Login()
- oauth2ResourceServer()

все они заканчиваются созданием:

```java
Authentication
```

и сохранением его в:

```java
SecurityContextHolder
```

Это фундаментальная идея Spring Security.

---

# Пример полного controller

```java
package com.laurentiuspilca.ssia.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(
            OAuth2AuthenticationToken authentication) {

        System.out.println(authentication.getName());

        authentication.getAuthorities()
                .forEach(System.out::println);

        System.out.println(
                authentication.getAuthorizedClientRegistrationId()
        );

        System.out.println(
                authentication.getPrincipal().getAttributes()
        );

        return "index.html";
    }
}
```

---

# Что показывает эта глава

Глава показывает, что:

```text
oauth2Login() полностью интегрирован в стандартную security model Spring Security.
```

После login приложение работает с Authentication точно так же, как и при любом другом способе аутентификации.

---

# Итог

После успешного OAuth2 Login:

```java
Authentication
```

помещается в:

```java
SecurityContextHolder
```

Далее приложение может использовать authentication:

- через method injection;
- через SecurityContextHolder;
- через @AuthenticationPrincipal;
- через @PreAuthorize;
- через authorities и roles.

Главная мысль главы:

```text
OAuth2 Login — это просто еще один способ получить Authentication в Spring Security.
```
