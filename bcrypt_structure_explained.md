
# Структура bcrypt-хеша и как работает проверка пароля

## Пример bcrypt-хеша

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

Общая структура строки:

```
$algorithm$cost$salt+hash
```

То есть строка делится на **3 части по символу `$`**.

| часть | значение |
|---|---|
| `$2a$` | версия алгоритма bcrypt |
| `10` | cost factor |
| `N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy` | salt + hash |

---

# Внутренняя структура третьей части

Третья часть:

```
N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

На самом деле состоит из:

```
salt (22 chars) + hash (31 chars)
```

Пример разделения:

```
N9qo8uLOickgx2ZMRZoMye | IjZAgcfl7p92ldGxad68LJZdL17lhWy
└────── salt ────────┘   └──── hash ───────────────┘
```

| часть | длина |
|---|---|
| salt | 22 символа |
| hash | 31 символ |

Итого третья часть содержит **53 символа**.

---

# Полная схема bcrypt строки

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
│ │  │ │
│ │  │ └──────── salt + hash
│ │  │
│ │  └────────── cost factor
│ │
│ └───────────── bcrypt version
│
└─────────────── разделитель
```

---

# Что означает `$2a$`

Это **версия алгоритма bcrypt**.

Возможные варианты:

| версия | значение |
|---|---|
| `$2a$` | оригинальная версия bcrypt |
| `$2b$` | исправленная версия |
| `$2y$` | версия из OpenBSD |
| `$2x$` | старая buggy версия |

В Spring Security обычно используется:

```
$2a$
```

---

# Что такое cost factor

В примере:

```
$2a$10$
     ↑
```

Число `10` — это **cost factor**, уровень сложности вычисления хеша.

Формула:

```
iterations = 2^cost
```

Примеры:

| cost | итерации |
|---|---|
| 8 | 256 |
| 10 | 1024 |
| 12 | 4096 |
| 14 | 16384 |

Чем больше значение:

- тем медленнее считается хеш
- тем сложнее brute-force атака

Но и логин пользователя будет немного медленнее.

bcrypt называют **adaptive hashing**, потому что сложность можно увеличивать со временем.

---

# Что такое salt

Salt — это **случайная строка**, добавляемая к паролю перед хешированием.

Формула:

```
hash(password + salt)
```

Пример:

```
password = "secret"
salt = "N9qo8uLOickgx2ZMRZoMye"
```

Внутри алгоритма считается:

```
hash("secretN9qo8uLOickgx2ZMRZoMye")
```

---

# Зачем нужен salt

Без соли одинаковые пароли дают одинаковые хеши:

| пользователь | пароль | hash |
|---|---|---|
| user1 | password | abc |
| user2 | password | abc |

Это позволяет использовать **rainbow tables**.

С солью:

| пользователь | пароль | salt | hash |
|---|---|---|---|
| user1 | password | A1 | X9 |
| user2 | password | B7 | Q4 |

Даже если пароль одинаковый — хеши разные.

---

# Почему salt хранится прямо в строке

Чтобы при проверке можно было повторить вычисление.

Spring Security берёт строку из базы:

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

Из неё извлекается:

```
salt = N9qo8uLOickgx2ZMRZoMye
```

Далее вычисляется:

```
hash(rawPassword + salt)
```

И результат сравнивается с сохранённым хешем.

---

# Что происходит при логине в Spring Security

```
LOGIN REQUEST
password = "mypassword"
      ↓
Spring Security
      ↓
UserDetailsService
      ↓
encodedPasswordFromDb = "$2a$10$..."
      ↓
PasswordEncoder.matches(rawPassword, encodedPassword)
      ↓
из encodedPassword извлекается salt
      ↓
bcrypt(rawPassword + salt)
      ↓
сравнение hash
      ↓
true / false
```

---

# Короткая формула

```
encode(password) → сохранить в БД
matches(rawPassword, hashFromDb) → проверить при логине
```

bcrypt-хеш содержит всю информацию:

```
algorithm + cost + salt + hash
```

Поэтому серверу достаточно одной строки из базы данных.
