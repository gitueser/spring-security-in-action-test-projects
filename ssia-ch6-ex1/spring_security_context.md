# Spring Security Context — понятное объяснение

---

# Главное

```text
SecurityContext = информация о текущем пользователе
SecurityContextHolder = механизм, через который Spring получает и хранит этот контекст
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

Типичный случай в Spring — использование `@Async`.

Например:

```text
Запрос A -> поток T1 -> User1 уже аутентифицирован
```

И дальше внутри кода, который выполняется в `T1`, запускается метод `@Async`, а Spring выполняет его в другом потоке.

Для наглядности будем обозначать его так:

```text
T1 -> основной поток HTTP-запроса
T1_child -> новый поток, созданный для @Async
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
                  | вызывает @Async
                  v
               T1_child -> SecurityContext(EMPTY)
```

То есть пользователь аутентифицирован в `T1`, но в `T1_child` контекст уже пустой.

---

# Пример endpoint с @Async

```java
@GetMapping("/bye")
@Async
public void goodbye() {
    SecurityContext context = SecurityContextHolder.getContext();
    String username = context.getAuthentication().getName();
    System.out.println("!!I am in Async!!!");
}
```

## Что здесь важно понять

Аннотация `@Async` означает, что метод будет выполнен **в другом потоке**.

То есть:

- HTTP-запрос пришёл в основной поток;
- метод `goodbye()` запускается уже не в этом потоке, а в новом;
- `SecurityContextHolder` по умолчанию не переносит контекст в новый поток;
- поэтому в async-методе можно получить пустой контекст, `null` или anonymous authentication.

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

### Пример конфигурации

```java
@Configuration
@EnableAsync
public class ProjectConfig {

    @Bean
    public InitializingBean initializingBean() {
        return () -> SecurityContextHolder.setStrategyName(
                SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}
```

## Что важно

### 1. `@EnableAsync`
Именно эта аннотация включает поддержку `@Async`.

Технически её можно поставить и в главном классе приложения, но на практике удобнее держать такие настройки в конфигурационном классе.

### 2. Бин с установкой стратегии должен сработать как можно раньше
Смысл в том, чтобы стратегия `SecurityContextHolder` была установлена **до того, как начнут активно использоваться async-вызовы**.

На практике такой бин и размещают в конфигурации одним из первых.

### 3. Что это даёт
Теперь при создании нового потока для `@Async` Spring сможет унаследовать контекст из родительского потока.

То есть:

```text
T2 -> Context(User2)
      |
      v
T2_child -> Context(User2)
```

---

## 3. MODE_GLOBAL

Один общий контекст на всё приложение.

```text
все потоки -> один общий SecurityContext
```

### Как это выглядит

```text
T1 -> общий Context
T2 -> общий Context
T3 -> общий Context
T1_child -> тот же общий Context
```

### Когда это может быть полезно

Эта стратегия может иметь смысл для автономных, не-web приложений, где действительно нужен один общий security context для всего процесса.

### Почему это почти не используют в web-приложениях

Потому что тогда все потоки начинают видеть и менять **один и тот же объект**.

Это означает:

- поток T1 может изменить данные;
- поток T2 увидит эти изменения;
- поток T3 тоже увидит эти изменения.

То есть появляется риск гонок данных и путаницы между пользователями.

### Пример настройки MODE_GLOBAL

```java
@Configuration
public class ProjectConfig {

    @Bean
    public InitializingBean initializingBean() {
        return () -> SecurityContextHolder.setStrategyName(
                SecurityContextHolder.MODE_GLOBAL);
    }
}
```

### Важное замечание про MODE_GLOBAL

`SecurityContext` не является потокобезопасным объектом.

Поэтому при использовании `MODE_GLOBAL` нужно самостоятельно заботиться о безопасном параллельном доступе и синхронизации.

Именно поэтому для обычных backend web-приложений такая стратегия практически никогда не подходит.

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

| Стратегия | Смысл | Для чего подходит |
|---|---|---|
| MODE_THREADLOCAL | каждый поток хранит свой контекст | обычные web-запросы |
| MODE_INHERITABLETHREADLOCAL | дочерний поток наследует контекст | async внутри web-приложения |
| MODE_GLOBAL | один общий контекст на всё приложение | автономные/single-context сценарии |

---

# Итог

1. `SecurityContext` хранит текущего пользователя.
2. В обычных HTTP-запросах всё работает нормально.
3. Проблема возникает только при создании нового потока внутри уже идущего запроса.
4. `@Async` — типичный пример, когда создаётся другой поток.
5. По умолчанию новый поток не получает контекст автоматически.
6. Для async это можно решить через `MODE_INHERITABLETHREADLOCAL`.
7. `MODE_GLOBAL` даёт один общий контекст на все потоки, но для web-приложений это обычно плохой выбор.
