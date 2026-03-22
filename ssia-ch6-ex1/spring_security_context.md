# Spring Security Context — понятное объяснение

---

# Главное

```text
SecurityContext = информация о текущем пользователе
SecurityContextHolder = место/механизм, через который Spring получает этот контекст
```

Обычно внутри `SecurityContext` хранится объект:

```java
Authentication authentication;
```

---

# Что происходит при обычных HTTP-запросах

Когда приходит HTTP-запрос, сервер (например Tomcat) выделяет для его обработки поток.

Например:

```text
Запрос A -> поток T1
Запрос B -> поток T2
Запрос C -> поток T3
```

После успешной аутентификации Spring Security сохраняет контекст безопасности **внутри потока, который обрабатывает запрос**.

Получается:

```text
T1 -> SecurityContext(User1)
T2 -> SecurityContext(User2)
T3 -> SecurityContext(User3)
```

## Важно

Здесь **никакой проблемы нет**.

То есть ситуация:

```text
Запрос A -> T1 -> User1
Запрос B -> T2 -> User2
Запрос C -> T3 -> User3
```

работает совершенно нормально.

---

# Где реально возникает проблема

Проблема появляется **не между обычными запросами**, а когда **внутри уже существующего потока запроса создаётся новый поток**.

Например:

```text
Запрос A -> поток T1 -> User1 уже аутентифицирован
```

И дальше внутри кода, который выполняется в `T1`, создаётся ещё один поток.

Для наглядности будем обозначать его так:

```text
T1_child
```

То есть:

```text
T1 -> основной поток HTTP-запроса
T1_child -> новый поток, созданный из T1
```

---

# Почему это важно

По умолчанию Spring Security хранит `SecurityContext` через `ThreadLocal`.

А `ThreadLocal` означает:

```text
данные привязаны к конкретному потоку
```

Поэтому:

```text
T1       -> видит SecurityContext(User1)
T1_child -> НЕ видит SecurityContext(User1) автоматически
```

Именно здесь и возникает проблема.

---

# Нормальная схема без проблемы

```text
Запрос A -> T1 -> SecurityContext(User1)
Запрос B -> T2 -> SecurityContext(User2)
Запрос C -> T3 -> SecurityContext(User3)
```

Здесь всё хорошо.

---

# Схема, где появляется проблема

```text
Запрос A -> T1 -> SecurityContext(User1)
                  |
                  | создаёт новый поток
                  v
               T1_child -> SecurityContext(EMPTY)
```

То есть пользователь аутентифицирован в `T1`, но в `T1_child` контекст уже пустой.

---

# Почему новый поток пустой

Потому что по умолчанию используется стратегия:

```text
MODE_THREADLOCAL
```

Она означает:

```text
каждый поток хранит свой собственный SecurityContext
```

Контекст **не копируется автоматически** в новый поток.

---

# Очень важная формулировка

Правильно понимать это так:

- обычные HTTP-запросы работают нормально;
- проблема только тогда, когда ты переходишь в **другой поток**;
- `ThreadLocal` не переносит данные между потоками автоматически.

Коротко:

```text
Обычные запросы -> OK
Новый поток внутри запроса -> контекст теряется
```

---

# Пример с @Async

Типичный сценарий:

```text
HTTP request -> поток T2 -> User2
```

Внутри этого запроса вызывается метод `@Async`, и Spring запускает его в другом потоке.

Для наглядности:

```text
T2 -> основной поток запроса
T2_child -> поток, в котором выполняется @Async
```

Схема:

```text
Запрос B -> T2 -> SecurityContext(User2)
                  |
                  | @Async
                  v
               T2_child -> SecurityContext(EMPTY)
```

То есть внутри `@Async` текущий пользователь уже может быть недоступен.

---

# Пример кода

```java
@GetMapping("/hello")
public String hello() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    System.out.println("main thread user = " + auth.getName());

    new Thread(() -> {
        Authentication childAuth =
                SecurityContextHolder.getContext().getAuthentication();
        System.out.println("child thread auth = " + childAuth);
    }).start();

    return "ok";
}
```

## Что здесь произойдёт

В основном потоке запроса пользователь будет доступен:

```text
main thread user = user1
```

А в новом потоке контекст может быть пустым:

```text
child thread auth = null
```

или anonymous, в зависимости от конфигурации.

---

# Что такое SecurityContextHolder

`SecurityContextHolder` — это класс, через который Spring Security получает доступ к текущему `SecurityContext`.

Пример:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

Это значит:

- взять текущий контекст;
- взять из него `Authentication`;
- получить имя пользователя.

---

# Стратегии хранения SecurityContext

Spring Security поддерживает 3 стратегии.

## 1. MODE_THREADLOCAL

Это стратегия по умолчанию.

```text
каждый поток -> свой собственный SecurityContext
```

Подходит для обычных servlet web-приложений.

### Как это выглядит

```text
T1 -> Context(User1)
T2 -> Context(User2)
T3 -> Context(User3)
```

Но:

```text
T1_child -> EMPTY
```

---

## 2. MODE_INHERITABLETHREADLOCAL

Эта стратегия позволяет дочернему потоку получить копию контекста родительского потока.

Схема:

```text
T1 -> Context(User1)
      |
      v
T1_child -> Context(User1)
```

Пример настройки:

```java
SecurityContextHolder.setStrategyName(
    SecurityContextHolder.MODE_INHERITABLETHREADLOCAL
);
```

---

## 3. MODE_GLOBAL

Один общий контекст на всё приложение.

```text
все потоки -> один общий SecurityContext
```

Для web-приложений почти никогда не используется, потому что это небезопасно.

---

# Как решить проблему нового потока

## Вариант 1. Использовать MODE_INHERITABLETHREADLOCAL

```java
SecurityContextHolder.setStrategyName(
    SecurityContextHolder.MODE_INHERITABLETHREADLOCAL
);
```

---

## Вариант 2. Передать контекст вручную

```java
SecurityContext context = SecurityContextHolder.getContext();

new Thread(() -> {
    SecurityContextHolder.setContext(context);
    // здесь логика уже увидит текущего пользователя
}).start();
```

---

## Вариант 3. Использовать готовые классы Spring Security

Spring даёт специальные обёртки:

- `DelegatingSecurityContextRunnable`
- `DelegatingSecurityContextCallable`
- `DelegatingSecurityContextExecutor`

Пример:

```java
Runnable task = () -> {
    Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
};

Runnable wrapped = new DelegatingSecurityContextRunnable(task);
new Thread(wrapped).start();
```

Это удобнее и безопаснее, чем передавать контекст вручную.

---

# Как запомнить

```text
SecurityContext живёт внутри потока
```

Поэтому:

```text
новый поток = новый контекст
```

если ты специально его не передал.

---

# Что относится к Spring MVC, а что нет

Это объяснение относится к:

```text
servlet / Spring MVC приложениям
```

Для reactive-приложений (`WebFlux`) модель другая, потому что там не опираются на `ThreadLocal` таким же образом.

---

# Короткая шпаргалка

| Стратегия | Смысл |
|---|---|
| MODE_THREADLOCAL | каждый поток хранит свой контекст |
| MODE_INHERITABLETHREADLOCAL | дочерний поток наследует контекст |
| MODE_GLOBAL | один общий контекст на всё приложение |

---

# Итог

1. `SecurityContext` хранит текущего пользователя.
2. В обычных HTTP-запросах всё работает нормально.
3. Проблема возникает только при создании нового потока внутри уже идущего запроса.
4. По умолчанию новый поток не получает контекст автоматически.
5. Для async/threads контекст нужно передавать отдельно или использовать специальные средства Spring Security.
