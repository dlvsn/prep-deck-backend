## What this is

The backend of **prep-deck**, a desktop app for preparing for interviews, languages and any other
subject: question → answer cards in nested topics, spaced-repetition practice, and topics shared
with other people. Built as a pet project to show what can be done with AI assistance.

The client is a separate repository (`prep-deck-client`, an Electron application) that sits next
to this one under one parent folder. A web version of the same client is planned. The client's
spec — topics, cards, practice grades, sharing roles — is what this API is being built to serve;
its `src/renderer/api/repositories.ts` lists every call it will make, and its mocks show what each
answers.

Built so far: accounts and sessions (`/api/auth/**`). Topics, cards, practice and sharing follow.

## Decisions already taken

**One backend, on a server.** Spring runs in one place and every client reaches it over HTTP.
The desktop installer does not carry Java or this service with it.

**The clients know this service through its REST API alone.** Nothing here may depend on which
client is calling, so a desktop-only shortcut (reading the user's disk, say) is never an option.

**One response shape, with message keys.** Every endpoint answers with `ApiResponse<T>`:
`success`, `message`, `data`. `message` is a key from the client's i18n files
(`auth.invalidCredentials`), which the client shows in the user's language; it is never prose.
`GlobalExceptionHandler` makes that shape out of every exception, `JsonSecurityErrors` out of the
401s and 403s the security filters raise before any controller runs.

**Layers.** Repository → DAO service (CRUD only, transactions) → web service (business rules,
DTOs) → controller. Each layer calls only the one beneath it; a controller never touches a
repository, and a web service never returns an entity.

## Sign-up and email confirmation

Sign-up creates the account **unconfirmed** (`users.email_verified_at` null) and emails a
six-digit code; no session yet (`202`, `RegistrationResponse`). `POST /auth/verify-email` with
the code confirms the address and opens the first session. Until then:

- log in with the right password answers `403 auth.emailNotVerified` with the email in `data`,
  and sends a fresh code, so the client can go straight to the code screen;
- registering the same email again takes the unconfirmed account over (new username, new
  password, new code): only whoever reads the mailbox can finish, so nothing is stolen.

Codes live in `one_time_codes` (`purpose` VERIFY_EMAIL, later RESET_PASSWORD), one active per user
and purpose, hashed as SHA-256 of `<user id>:<code>`. 15 minutes, 5 guesses (`attempts_left`; a
wrong guess must stick, hence `noRollbackFor`), at most one letter a minute (`auth.resendTooSoon`
with `retryAfterSeconds`). Refusals carry what the client shows: attempts left, seconds to wait.

**Letters go out after commit.** The service publishes `VerificationCodeIssued`;
`VerificationMailListener` handles it `AFTER_COMMIT` and `@Async`: a rolled-back sign-up sends
nothing, and a slow SMTP server never holds up a response. A failed send is logged; the person
asks for the code again. `EmailTemplates` fills `<location><name>-<language><suffix>` (all in
`prep-deck.mail.templates.*`, today `classpath:templates/verify-email-uk.html`)
(`{{code}}`, `{{username}}`, `{{email}}`, `{{expires_minutes}}`, `{{support_email}}`, values
HTML-escaped; subject = `<title>`), the language from the request's `Accept-Language`.
Locally, letters land in Mailpit (`docker/docker-compose.yaml`, inbox at http://localhost:8025).

## Sessions

A session is two tokens:

- an **access token**: a JWT signed with HS256 (`JwtConfig`), 15 minutes, subject = user id.
  Spring Security's resource server checks it on every request outside the open auth endpoints;
  controllers read the user id from `@AuthenticationPrincipal Jwt`;
- a **refresh token**: 256 random bits handed to the client once. The database keeps only its
  SHA-256 (`refresh_tokens.token_hash`), 30 days.

**A refresh token works once.** `AuthWebServiceImpl.refresh` locks the row, revokes it, and issues the
next token in the same `family_id`. A revoked token that comes back can only be a stolen copy:
the whole family is revoked and both holders have to log in again. That revocation must survive
the 401 that follows, hence `noRollbackFor = ApiException.class`. Log out revokes the family too.

**Passwords** are stored as `DelegatingPasswordEncoder` hashes (BCrypt, `{bcrypt}` prefix) and
upgraded on log in if the settings ever get stronger. A login that matches no account is still
checked against a dummy hash, so response times do not tell which accounts exist. Every 401 from
the security filters says `errors.sessionExpired`: the client refreshes once and otherwise shows
the login screen.

Email and username are stored lower-cased (the table checks it), so their unique constraints are
case-insensitive.

## User settings

`/api/settings` keeps a user's picks so every device starts the same: theme mode, accent,
language, the topic opened last. One `user_settings` row per user, made on the first `PUT`; a
`GET` before that answers all nulls (`updatedAt` null), and the client then uploads its own
values. After sign-in the server's copy wins; every later change is sent as the whole set.
The enums (`ThemeMode`, `AccentColor`, `Language` in `domain/`) travel as the client's lower-case
codes (`@JsonValue`/`@JsonCreator`) and are stored as their names. `last_topic_id` is a plain
string while topics live only in the client; it becomes a UUID foreign key with the topics table.

## Things to know before changing code

- Settings come from env variables, locally from `.env` beside `pom.xml` (imported by
  `spring.config.import`; keys in `.env.example`). The JWT key is `JWT_SECRET` (≥ 32 bytes);
  left empty, a random key is made per start: access tokens die with the process, refresh tokens
  live on in the database.
- `server.servlet.context-path=/api`: every path in code — `ApiPaths`, the `SecurityConfig`
  matchers, the CORS mapping — is relative to it. `ApiPaths.AUTH = "/auth"` answers at `/api/auth`.
- The JVM runs in UTC (`PrepDeckBackendApplication.main`). Without it the PostgreSQL driver sends
  the machine's zone on connect, and on Windows that is `Europe/Kiev`, which PostgreSQL refuses.
- Flyway owns the schema: `V1.0.0__create_schema.sql` creates `prep_deck`, every later file adds
  to it; Flyway's own history table stays in `public`. Hibernate only validates.
- CORS allows `prep-deck.cors.allowed-origins` (the client's dev server for now). The packaged
  Electron app loads from `file://` and will need its origin added when it talks to a server.
- Not built yet, though the client already calls them: password reset. Its templates exist and
  `one_time_codes` already allows `RESET_PASSWORD`.
- Spring Boot 4.2, Java 21, Lombok, MapStruct 1.6, PostgreSQL 17 (run locally from `db/`: `docker compose up -d`, data in `db/data`).

## Where to start

- A new feature: a Flyway migration, then `persistence/<feature>/{entity,repository,dao,mapper}`,
  `dto/<feature>/{request,response}`, `service/<feature>`, `api/<feature>`, and a path in
  `ApiPaths`.
- A new open endpoint: add it to the `permitAll()` list in `SecurityConfig`.
- Settings: `src/main/resources/application.properties`.
