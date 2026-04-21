
# Spring Security CSRF Demo — Расширенное объяснение

## 📌 Что это за проект

Демонстрация CSRF-защиты в Spring Security с кастомным `CsrfTokenRepository`,
который хранит токены в MySQL.

---

## 🧠 Ключевая идея CSRF

CSRF (Cross-Site Request Forgery) — защита от поддельных запросов.
Сервер выдаёт клиенту секрет (токен), который должен быть возвращён
в каждом state-changing запросе (POST/PUT/DELETE/PATCH).

---

## ❗ ВАЖНО: GET-запросы

- **GET не требует CSRF-токена**
- Spring Security по умолчанию НЕ проверяет CSRF для GET
- Поэтому:
  - `GET /hello` работает без токена
  - методы `generateToken()` и `saveToken()` обычно НЕ вызываются
- `loadToken()` тоже обычно не нужен для обычного `GET`, если приложение явно не запрашивает токен

Исключение: если ты явно запрашиваешь `CsrfToken`, например через endpoint `/csrf`.

---

## 🔄 Lifecycle токена в этом приложении

### 1. GET /hello

```http
GET /hello
```

Что происходит:

- CSRF не нужен
- token flow НЕ запускается
- БД не трогается

---

### 2. GET /csrf

```http
GET /csrf
X-IDENTIFIER: 12345
```

Flow:

1. `loadToken()` → ищем токен в БД
2. если токена нет → `generateToken()`
3. затем → `saveToken()`
4. токен возвращается клиенту

---

### 3. POST /hello

```http
POST /hello
X-IDENTIFIER: 12345
X-CSRF-TOKEN: <token>
```

Flow:

1. `loadToken()` → берём ожидаемый токен
2. сравнение с токеном из запроса
3. если совпадает → OK
4. иначе → 403

---

## 🐳 Поднятие MySQL в Docker

### Запуск контейнера

Если ты работаешь в **Git Bash** или похожем bash-терминале на Windows, используй:

```bash
docker run --name spring-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=spring \
  -p 3306:3306 \
  -d mysql:8.3
```

Если ты работаешь в **cmd.exe**, можно использовать `^` для переноса строк:

```cmd
docker run --name spring-mysql ^
  -e MYSQL_ROOT_PASSWORD=root ^
  -e MYSQL_DATABASE=spring ^
  -p 3306:3306 ^
  -d mysql:8.3
```

Если контейнер с таким именем уже существует и мешает запуску:

```bash
docker rm -f spring-mysql
```

---

## 🔍 Проверка, что контейнер поднялся

```bash
docker ps
```

Или посмотреть все контейнеры, включая остановленные:

```bash
docker ps -a
```

Посмотреть логи контейнера:

```bash
docker logs spring-mysql
```

---

## 🗄️ Как зайти в MySQL внутри контейнера

### На Linux/macOS
```bash
docker exec -it spring-mysql mysql -uroot -proot
```

### На Windows в Git Bash
Часто нужно использовать `winpty`, иначе интерактивная консоль может не открыться корректно:

```bash
winpty docker exec -it spring-mysql mysql -uroot -proot
```

---

## 🧪 Проверка, что база создалась

После входа в mysql-клиент:

```sql
SHOW DATABASES;
```

Ты должен увидеть базу `spring`, потому что контейнер был создан с переменной:

```text
MYSQL_DATABASE=spring
```

Переключиться на неё:

```sql
USE spring;
```

Проверить текущую базу:

```sql
SELECT DATABASE();
```

---

## 🧱 Проверка таблиц

Показать таблицы в текущей базе:

```sql
SHOW TABLES;
```

После запуска Spring Boot приложения и выполнения `schema.sql` здесь должна появиться таблица `token`.

---

## 🧾 Проверка структуры таблицы

```sql
DESCRIBE token;
```

или

```sql
SHOW CREATE TABLE token;
```

---

## 📄 Проверка содержимого таблицы

```sql
SELECT * FROM token;
```

Если хочешь посмотреть записи в более читабельном вертикальном формате:

```sql
SELECT * FROM token\G
```

Посмотреть только количество записей:

```sql
SELECT COUNT(*) FROM token;
```

Удалить все записи из таблицы:

```sql
DELETE FROM token;
```

---

## ⚙️ Как `schema.sql` создаёт таблицу

Приложение подключается к:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/spring
```

Это означает:

- сервер MySQL находится на `localhost`
- порт `3306`
- база по умолчанию — `spring`

Дальше Spring Boot выполняет файл `schema.sql` в контексте этой базы.
Поэтому в `schema.sql` обычно достаточно писать:

```sql
CREATE TABLE IF NOT EXISTS token (
    id INT NOT NULL AUTO_INCREMENT,
    identifier VARCHAR(45) NULL,
    token TEXT NULL,
    PRIMARY KEY (id)
);
```

а не `spring.token`, потому что соединение уже открыто именно к базе `spring`.

---

## 🚀 Запуск приложения

```bash
mvn spring-boot:run
```

После запуска приложения можно снова зайти в MySQL и проверить:

```sql
USE spring;
SHOW TABLES;
SELECT * FROM token;
```

---

## 🧪 Команды для проверки приложения

### Получить токен

```bash
curl -i -H "X-IDENTIFIER:12345" http://localhost:8080/csrf
```

---

### Проверить, что токен сохранился в БД

```sql
SELECT * FROM token;
```

---

### POST без токена

```bash
curl -i -X POST -H "X-IDENTIFIER:12345" http://localhost:8080/hello
```

Ожидаемый результат:

```text
403 Forbidden
```

---

### POST с токеном

```bash
curl -i -X POST \
  -H "X-IDENTIFIER:12345" \
  -H "X-CSRF-TOKEN: <TOKEN>" \
  http://localhost:8080/hello
```

Ожидаемый результат:

```text
200 OK
```

и тело ответа:

```text
Post Hello!
```

---

### Проверка, что другой identifier требует свой токен

Сначала получаем токен для нового клиента:

```bash
curl -i -H "X-IDENTIFIER:9999" http://localhost:8080/csrf
```

После этого в БД должна появиться новая запись:

```sql
SELECT * FROM token;
```

---

## 🧠 Как это работает в реальных приложениях

### Где хранится токен

Обычно:

- в HTTP session (server-side)
- или в cookie

---

### Когда пользователь получает токен

Чаще всего:

1. при первой загрузке страницы
2. после логина
3. при bootstrap-запросе в SPA
4. через специальный endpoint, например `/csrf`

---

### Типичный сценарий в реальном приложении

1. пользователь логинится
2. сервер создаёт session
3. сервер генерирует CSRF-токен
4. клиент получает токен:
   - из HTML
   - из cookie
   - из JSON-ответа bootstrap endpoint
5. клиент отправляет токен при каждом POST/PUT/PATCH/DELETE

---

## ⚠️ Отличие твоей реализации

У тебя:

- нет session
- нет полноценной authentication-связки с пользователем
- используется `X-IDENTIFIER` как ключ для поиска токена

Это упрощённая учебная модель:

```text
identifier -> token
```

---

## ❗ Почему это не production-ready

- `X-IDENTIFIER` можно подделать
- нет надёжной привязки к пользователю
- нет session binding
- это демонстрация механики `CsrfTokenRepository`, а не готовая боевая схема

---

## 📊 Последовательность вызовов

### GET /hello

```text
CSRF flow обычно не запускается
```

### GET /csrf для нового identifier

```text
loadToken -> generateToken -> saveToken
```

### GET /csrf для существующего identifier

```text
loadToken -> token found -> return existing token
```

### POST /hello с валидным токеном

```text
loadToken -> compare -> OK
```

### POST /hello без токена

```text
loadToken -> compare -> FAIL -> 403
```

### POST /hello с токеном от другого identifier

```text
loadToken -> compare -> FAIL -> 403
```

---

## 📚 Итог

Проект показывает:

- как работает CSRF внутри Spring Security
- lifecycle токена
- кастомное хранилище токенов
- разницу между GET и POST
- как использовать MySQL в Docker для хранения токенов

---
