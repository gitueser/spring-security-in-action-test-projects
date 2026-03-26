# DelegatingSecurityContext — информация по работе с потоками в Spring Security

---

# 📌 Зачем это нужно

Когда вы используете:

- `@Async`
- `Executor`
- `ThreadPool`
- `CompletableFuture`

➡️ код выполняется в **другом потоке**

И тогда:

```text
SecurityContext теряется ❗
```

Spring Security решает это через специальные классы:

```text
DelegatingSecurityContext*
```

---

# 📊 Таблица классов

| Класс | Что это | Когда использовать |
|---|---|---|
| DelegatingSecurityContextExecutor | обёртка над Executor, которая прокидывает SecurityContext | когда используешь Executor |
| DelegatingSecurityContextExecutorService | то же самое для ExecutorService | когда есть пул потоков |
| DelegatingSecurityContextScheduledExecutorService | для Scheduled задач | для @Scheduled / delay задач |
| DelegatingSecurityContextRunnable | оборачивает Runnable и копирует SecurityContext | когда создаёшь поток вручную |
| DelegatingSecurityContextCallable | оборачивает Callable и копирует SecurityContext | когда нужен результат из потока |

---

# 📌 Главная идея

```text
Обычный Runnable -> теряет SecurityContext
DelegatingSecurityContextRunnable -> переносит SecurityContext
```

---

# ❌ Проблемный код (без SecurityContext)

```java
new Thread(() -> {
    Authentication auth =
        SecurityContextHolder.getContext().getAuthentication();

    System.out.println(auth); // null ❗
}).start();
```

---

# ✅ Правильный код (с DelegatingSecurityContextRunnable)

```java
Runnable task = () -> {
    Authentication auth =
        SecurityContextHolder.getContext().getAuthentication();

    System.out.println(auth.getName());
};

Runnable wrapped = new DelegatingSecurityContextRunnable(task);

new Thread(wrapped).start();
```

---

# 📌 DelegatingSecurityContextRunnable

## Когда использовать

- создаёшь поток вручную
- используешь Thread / Runnable

## Что делает

```text
копирует SecurityContext из текущего потока в новый
```

---

# 📌 DelegatingSecurityContextCallable

## Когда использовать

- используешь Callable
- нужен результат из другого потока

## Пример

```java
Callable<String> task = () -> {
    Authentication auth =
        SecurityContextHolder.getContext().getAuthentication();

    return auth.getName();
};

Callable<String> wrapped =
    new DelegatingSecurityContextCallable<>(task);

new Thread(() -> {
    try {
        System.out.println(wrapped.call());
    } catch (Exception e) {
        e.printStackTrace();
    }
}).start();
```

---

# 📌 DelegatingSecurityContextExecutor

## Когда использовать

- используешь Executor
- например ThreadPoolTaskExecutor

## Пример

```java
Executor executor = Executors.newFixedThreadPool(5);

Executor securedExecutor =
    new DelegatingSecurityContextExecutor(executor);

securedExecutor.execute(() -> {
    Authentication auth =
        SecurityContextHolder.getContext().getAuthentication();

    System.out.println(auth.getName());
});
```

---

# 📌 DelegatingSecurityContextExecutorService

## Когда использовать

- используешь ExecutorService
- работаешь с пулом потоков

## Пример

```java
ExecutorService executorService =
    Executors.newFixedThreadPool(5);

ExecutorService securedExecutorService =
    new DelegatingSecurityContextExecutorService(executorService);

securedExecutorService.submit(() -> {
    Authentication auth =
        SecurityContextHolder.getContext().getAuthentication();

    System.out.println(auth.getName());
});
```

---

# 📌 DelegatingSecurityContextScheduledExecutorService

## Когда использовать

- планировщик задач
- delay / scheduled задачи

## Пример

```java
ScheduledExecutorService scheduler =
    Executors.newScheduledThreadPool(2);

ScheduledExecutorService securedScheduler =
    new DelegatingSecurityContextScheduledExecutorService(scheduler);

securedScheduler.schedule(() -> {
    Authentication auth =
        SecurityContextHolder.getContext().getAuthentication();

    System.out.println(auth.getName());
}, 1, TimeUnit.SECONDS);
```

---

# 📌 Best Practices

## ✅ Используй DelegatingSecurityContext*

```text
если работаешь с потоками — почти всегда нужно
```

---

## ❌ Не надейся на MODE_INHERITABLETHREADLOCAL

```text
работает не всегда (особенно с thread pools)
```

---

## ✅ Для production

```text
DelegatingSecurityContextExecutorService — лучший выбор
```

---

## ❗ Почему важно

Thread pools переиспользуют потоки:

```text
один поток -> разные пользователи ❗
```

Если не использовать DelegatingSecurityContext:

```text
можно случайно получить чужого пользователя
```

---

# 📌 Коротко

```text
ThreadLocal не работает с потоками автоматически
DelegatingSecurityContext* — делает это правильно
```

---

# 📌 Итог

- `@Async` и Executor ломают SecurityContext
- DelegatingSecurityContext* решает проблему
- это production-ready подход
- MODE_INHERITABLETHREADLOCAL — упрощённый вариант, но не всегда безопасный

