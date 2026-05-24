# Spring Security OAuth2 Client: Client Credentials Grant

Этот пример относится к главе 16.2 и показывает, как реализовать приложение, которое само выступает OAuth2 Client и получает access token у Authorization Server с помощью grant type:

```text
client_credentials
```

В этом сценарии пользователь не участвует. Приложение получает access token на основании собственных client credentials:

```text
client_id
client_secret
```

---

# Проекты

В примере используются два приложения:

```text
ssia-ch16-ex2-authorization-server
ssia-ch16-ex2-client
```

---

# Authorization Server

Проект:

```text
ssia-ch16-ex2-authorization-server
```

Порт:

```text
7070
```

Этот сервер:

- регистрирует OAuth2 client;
- поддерживает `client_credentials` grant;
- выдает access token;
- предоставляет token endpoint:

```text
http://localhost:7070/oauth2/token
```

---

# OAuth2 Client

Проект:

```text
ssia-ch16-ex2-client
```

Порт:

```text
8080
```

Этот проект:

- сам является OAuth2 Client;
- вызывает Authorization Server;
- получает access token через `client_credentials`;
- возвращает access token в теле ответа endpoint'а:

```text
GET /token
```

---

# Главная идея главы

В предыдущих примерах OAuth2 использовался для login пользователя:

```text
oauth2Login()
```

В этой главе используется другой сценарий:

```text
service-to-service communication
```

То есть одно приложение вызывает другое приложение.

В таких сценариях часто используется grant:

```text
client_credentials
```

---

# Когда используется client_credentials

Grant `client_credentials` подходит, когда:

- нет пользователя;
- приложение вызывает другое приложение;
- backend service вызывает protected API;
- microservice получает token для обращения к resource server;
- token нужен самому приложению, а не пользователю.

---

# Что НЕ используется в client_credentials

Так как пользователя нет, не нужны:

- login page;
- redirect URI;
- authorization endpoint в браузере;
- authorization code;
- PKCE;
- user session.

Приложение напрямую вызывает:

```text
/oauth2/token
```

и передает:

```text
client_id
client_secret
grant_type=client_credentials
```

---

# Общий flow

```text
User / curl
    |
    | GET /token
    v
OAuth2 Client Application
    |
    | POST /oauth2/token
    | grant_type=client_credentials
    | Authorization: Basic client:secret
    v
Authorization Server
    |
    | access token
    v
OAuth2 Client Application
    |
    | token in HTTP response body
    v
User / curl
```

---

# Authorization Server configuration

## RegisteredClient

В Authorization Server регистрируется client:

```java
@Bean
public RegisteredClientRepository registeredClientRepository() {
    RegisteredClient registeredClient = RegisteredClient
            .withId(UUID.randomUUID().toString())
            .clientId("client")
            .clientSecret("secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scope(OidcScopes.OPENID)
            .build();

    return new InMemoryRegisteredClientRepository(registeredClient);
}
```

---

# Что важно в RegisteredClient

## clientId

```java
.clientId("client")
```

Идентификатор клиента.

## clientSecret

```java
.clientSecret("secret")
```

Секрет клиента.

В учебном примере используется plain text secret, потому что также настроен:

```java
NoOpPasswordEncoder
```

В production так делать нельзя.

## clientAuthenticationMethod

```java
.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
```

Это означает, что client будет аутентифицироваться на token endpoint через HTTP Basic.

То есть фактически используется:

```text
Authorization: Basic Base64(client:secret)
```

Для:

```text
client:secret
```

Base64 значение:

```text
Y2xpZW50OnNlY3JldA==
```

## authorizationGrantType

```java
.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
```

Это ключевая настройка главы.

Она разрешает client получать token через:

```text
grant_type=client_credentials
```

## redirectUri не нужен

В этом проекте не нужен:

```java
.redirectUri(...)
```

Потому что `client_credentials` не использует browser redirect.

## AUTHORIZATION_CODE не нужен

В этом проекте не нужен:

```java
.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
```

Потому что пользователь не логинится через браузер.

---

# Authorization Server application.properties

```properties
server.port=7070
```

Authorization Server работает на порту:

```text
7070
```

---

# OAuth2 Client configuration

Проект:

```text
ssia-ch16-ex2-client
```

---

# SecurityFilterChain

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.oauth2Client(Customizer.withDefaults());

    http.authorizeHttpRequests(c -> c.anyRequest().permitAll());

    return http.build();
}
```

---

# Что делает oauth2Client()

```java
http.oauth2Client(Customizer.withDefaults());
```

Эта настройка включает поддержку OAuth2 Client.

Важно не путать:

```java
oauth2Login()
```

и:

```java
oauth2Client()
```

---

# oauth2Login() vs oauth2Client()

## oauth2Login()

Используется для login пользователя через внешний provider.

```java
http.oauth2Login(...)
```

## oauth2Client()

Используется, когда само приложение должно получать access token и вызывать другие сервисы.

```java
http.oauth2Client(...)
```

---

# Почему permitAll()

```java
http.authorizeHttpRequests(c -> c.anyRequest().permitAll());
```

Endpoint `/token` открыт для учебного примера, чтобы можно было вызвать его через curl без login.

В production такой endpoint обычно не должен просто возвращать token наружу. Здесь это сделано только для демонстрации.

---

# ClientRegistrationRepository

```java
@Bean
public ClientRegistrationRepository clientRegistrationRepository() {
    ClientRegistration c1 = ClientRegistration
            .withRegistrationId("1")
            .clientId("client")
            .clientSecret("secret")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .tokenUri("http://localhost:7070/oauth2/token")
            .scope(OidcScopes.OPENID)
            .build();

    return new InMemoryClientRegistrationRepository(c1);
}
```

---

# Что такое ClientRegistration

`ClientRegistration` описывает, как OAuth2 Client должен обращаться к Authorization Server.

В этом примере указываются:

- registration id;
- client id;
- client secret;
- grant type;
- authentication method;
- token URI;
- scope.

---

# registrationId

```java
.withRegistrationId("1")
```

Это внутренний идентификатор client registration.

Он используется позже в controller:

```java
.withClientRegistrationId("1")
```

Эти значения должны совпадать.

---

# tokenUri

```java
.tokenUri("http://localhost:7070/oauth2/token")
```

Это endpoint Authorization Server, куда OAuth2 Client отправляет запрос за access token.

---

# OAuth2AuthorizedClientManager

Главный компонент для получения access token:

```java
OAuth2AuthorizedClientManager
```

Он отвечает за то, чтобы:

- найти нужный `ClientRegistration`;
- понять, какой grant type использовать;
- вызвать Authorization Server;
- получить access token;
- вернуть authorized client.

---

# AuthorizedClientManager configuration

```java
@Bean
public OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientRepository authorizedClientRepository
) {
    OAuth2AuthorizedClientProvider provider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                    .clientCredentials()
                    .build();

    DefaultOAuth2AuthorizedClientManager manager =
            new DefaultOAuth2AuthorizedClientManager(
                    clientRegistrationRepository,
                    authorizedClientRepository
            );

    manager.setAuthorizedClientProvider(provider);

    return manager;
}
```

---

# OAuth2AuthorizedClientProvider

```java
OAuth2AuthorizedClientProviderBuilder.builder()
        .clientCredentials()
        .build();
```

Эта часть говорит менеджеру:

```text
используй grant type client_credentials
```

---

# DemoController

```java
@RestController
public class DemoController {

    private final OAuth2AuthorizedClientManager clientManager;

    public DemoController(OAuth2AuthorizedClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @GetMapping("/token")
    public String token() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId("1")
                .principal("client")
                .build();

        var client = clientManager.authorize(request);

        if (client == null) {
            throw new IllegalStateException("Client authorization failed");
        }

        return client.getAccessToken().getTokenValue();
    }
}
```

---

# Что делает endpoint /token

Endpoint:

```text
GET /token
```

делает следующее:

1. создает `OAuth2AuthorizeRequest`;
2. передает request в `OAuth2AuthorizedClientManager`;
3. manager получает access token у Authorization Server;
4. controller возвращает token в HTTP response body.

---

# Зачем нужен principal("client")

```java
.principal("client")
```

В этом сценарии это не пользователь.

Это имя principal, под которым Spring Security связывает authorized client.

Для `client_credentials` пользователя нет, но Spring Security все равно требует principal name для внутреннего хранения authorized client.

---

# Запуск приложений

Сначала нужно запустить Authorization Server.

## 1. Запустить Authorization Server

В папке:

```text
ssia-ch16-ex2-authorization-server
```

Команда:

```bash
mvn spring-boot:run
```

Сервер будет доступен на:

```text
http://localhost:7070
```

## 2. Запустить OAuth2 Client

В папке:

```text
ssia-ch16-ex2-client
```

Команда:

```bash
mvn spring-boot:run
```

Client application будет доступен на:

```text
http://localhost:8080
```

---

# Проверка работы

Выполнить:

```bash
curl http://localhost:8080/token
```

Ожидаемый результат:

```text
eyJraWQiOiI...
```

То есть в response body вернется access token.

---

# Проверка напрямую через Authorization Server

Для сравнения можно запросить token напрямую:

```bash
curl -i -X POST "http://localhost:7070/oauth2/token" \
-H "Authorization: Basic Y2xpZW50OnNlY3JldA==" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=openid"
```

Но смысл главы в том, чтобы этот запрос делало само приложение через:

```java
OAuth2AuthorizedClientManager
```

---

# Использование Maven без отдельной установки

Чтобы команды `mvn` работали в Git Bash без отдельной установки Maven в систему, можно создать alias для встроенного Maven из IntelliJ IDEA.

Выполните команды:

```bash
echo "alias mvn='/c/Users/<USERNAME>/AppData/Roaming/JetBrains/<INTELLIJ_VERSION>/plugins/maven/lib/maven3/bin/mvn.cmd'" >> ~/.bashrc
source ~/.bashrc
```

Где:

- `<USERNAME>` — имя пользователя Windows
- `<INTELLIJ_VERSION>` — версия IntelliJ IDEA, например `IntelliJIdea2026.1`

После этого команда `mvn` станет доступна в Git Bash.

---

# Что важно запомнить

- `client_credentials` используется без пользователя.
- `redirectUri` не нужен.
- `authorization_code` не нужен.
- `oauth2Login()` здесь не используется.
- Используется `oauth2Client()`.
- Token получает не пользователь, а само приложение.
- За получение token отвечает `OAuth2AuthorizedClientManager`.
- `ClientRegistrationRepository` описывает данные OAuth2 client.
- `/token` endpoint в примере нужен только для демонстрации.

---

# Итог

Глава 16.2 показывает, как приложение может быть OAuth2 Client в service-to-service сценарии.

В этом примере:

- Authorization Server выдает token через `client_credentials`;
- OAuth2 Client получает token через `OAuth2AuthorizedClientManager`;
- controller возвращает token в response body;
- пользовательский login flow отсутствует.
