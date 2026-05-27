# Spring Security Reactive Applications (WebFlux + Reactor)

Этот пример относится к главе 17.1 и вводит основы реактивных приложений в Spring Security.

Проект:

```text
ssia-ch17-ex1
```

---

# Что такое реактивное приложение

В классическом (императивном) приложении используется подход:

```text
1 request -> 1 thread
```

Каждый HTTP-запрос обслуживается отдельным потоком.

Пока поток ждет:

- базу данных;
- внешний API;
- файловую систему;
- сеть;

он простаивает.

---

# Императивный подход

При императивном подходе приложение:

- получает все данные целиком;
- затем начинает их обработку.

---

# Реактивный подход

При реактивном подходе приложение:

- получает данные частями;
- обрабатывает их по мере поступления;
- не блокирует поток ожиданием.

---

# Reactive Streams

Reactive Streams — спецификация для асинхронной обработки потоков данных.

Основные контракты:

```text
Publisher
Subscriber
```

---

# Project Reactor

Spring WebFlux использует библиотеку:

```text
Project Reactor
```

Project Reactor реализует спецификацию Reactive Streams.

---

# Mono и Flux

## Mono

```text
Mono<T>
```

Publisher для:

```text
0 или 1 значения
```

## Flux

```text
Flux<T>
```

Publisher для:

```text
0..N значений
```

---

# Запуск приложения

```bash
mvn spring-boot:run
```

---

# Проверка endpoint

```bash
curl http://localhost:8080/hello
```

Ответ:

```text
Hello!
```

---

# Netty вместо Tomcat

При использовании:

```xml
spring-boot-starter-webflux
```

Spring Boot автоматически поднимает:

```text
Netty
```

вместо:

```text
Tomcat
```

---

# Реактивность и Spring Security

В реактивном приложении:

```text
1 request != 1 thread
```

Поэтому классический ThreadLocal-based SecurityContext больше не подходит.

Из-за этого Spring Security имеет отдельную реактивную реализацию безопасности.

---

# Основная идея главы

Глава 17.1 вводит фундаментальные понятия:

- Reactive Streams
- Publisher
- Subscriber
- Project Reactor
- Mono
- Flux
- WebFlux
- Netty
- Reactive Security Context
