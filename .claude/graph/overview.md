## What this is

The backend of **prep-deck**, a desktop app for preparing for interviews, languages and any other
subject: question → answer cards in nested topics, spaced-repetition practice, and topics shared
with other people. Built as a pet project to show what can be done with AI assistance.

The client is a separate repository (`prep-deck-client`, an Electron application) that sits next
to this one under one parent folder. A web version of the same client is planned. The client's
spec — topics, cards, practice grades, sharing roles — is what this API is being built to serve;
its `src/renderer/api/repositories.ts` lists every call it will make, and its mocks show what each
answers.

Built so far: accounts and sessions (`/api/auth/**`), settings, topics, cards and card pictures.
The client has no mocks any more and talks to this service only. Practice and sharing follow.

## Decisions already taken

**One backend, on a server.** Spring runs in one place and every client reaches it over HTTP.
The desktop installer does not carry Java or this service with it.

**The clients know this service through its REST API alone.** Nothing here may depend on which
client is calling, so a desktop-only shortcut (reading the user's disk, say) is never an option.

**One response shape, with message keys.** Every endpoint answers with `ApiResponse<T>`:
`success`, `message`, `data`. `message` is a key from the client's i18n files
(`auth.invalidCredentials`), which the client shows in the user's language; it is never prose.
`GlobalExceptionHandler` makes that shape out of every exception, `JsonSecurityErrors` out of the
401s and 403s the security filters raise before any controller runs. The one exception is
`GET /images/{id}/content`, which answers a picture's bytes.

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
string; it may become a UUID foreign key to `topics` (the client already falls back to the first
topic when the saved one is gone).

## Topics and cards

**Topics** are a tree per owner (`topics.parent_id`, `position` among siblings), read whole and
flat (`GET /topics`, each with `cardCount` of the cards directly in it); the client builds the tree
and sums subtrees. Create appends after the siblings; `POST /topics/{id}/move` reorders and
renumbers both sets of siblings in one transaction and refuses `topics.moveIntoItself`. Delete
leaves the subtree and its cards to `ON DELETE CASCADE`.

**Access goes through `TopicGuard.require` alone**: today a topic is its owner's only, and
anyone else's answers 404 `errors.noAccess` (ids cannot be probed). Card calls check the card's
topic. `TopicResponse.role` is always `owner`, `shared`/`unseen` false, `dueCount` 0; sharing
will widen `TopicGuard` and fill these, not change the services that call it.

**Cards**: question (≤ 500), Markdown answer (≤ 20 000), tags in `card_tags` (lower case, no `#`,
no repeats, ≤ 10, normalised by the server), pictures in `card_images` in display order. Lists are
newest first. `PATCH /cards/{id}` changes only the fields it carries; `topicId` moves the card.
`POST /cards/{id}/duplicate` takes the copy's question from the client (it is localised: "… (copy)",
"… (копія)") and shares the original's image rows. `status` is always `new` and `dueAt` null until
practice exists; editing never touches progress.

## Card pictures

Pictures go up **before** the card is saved: `POST /images` (multipart `file`; the client has
already scaled it to ≤ 2560 px) answers `{ id, url, width, height }`, and the card then sends
`imageIds` in order. **Nothing the client says about the file is trusted**; all is measured on it:
the weight on the bytes received (over 10 MB is 413 `images.tooLarge`), the format on its first
bytes (`ImageType.sniff`: PNG, JPEG, WebP, GIF; else 415 `images.notAnImage`), the width and
height on its headers (`ImageSizeReader`, metadata-extractor, EXIF rotation applied, pixels never
decoded). Below `prep-deck.images.min-width` × `min-height` (64 × 64) is 400 `images.tooSmall`
with the minimum in `data`. A large resolution is fine: only the weight is capped. Only the uploader can
put an image on a card, or a card that already shows it.

The bytes go through `ImageFileStore`, chosen by `IMAGES_STORAGE` (`prep-deck.images.storage`):
`LOCAL` writes one file per image id to `IMAGES_DIR` (`DiskImageFileStoreImpl`); `S3` puts one
object per id, keyed `images/<id>`, into an S3-compatible bucket (`S3ImageFileStoreImpl`, AWS SDK
v2, client in `ImageStorageConfig`). The project uses Cloudflare R2: endpoint
`https://<account id>.r2.cloudflarestorage.com`, region `auto`, path-style addressing, checksums
only where required. The bucket stays private: pictures are still served through
`GET /images/{id}/content`, so the client never learns where they are kept. `read` opens
nothing: `S3ObjectResource` requests the object only when Spring writes the response and is
closed by it, so a response that is never written (406, an exception) holds no pooled
connection; `S3ObjectInputStream` aborts instead of draining when the client leaves mid-picture. No transaction is open
while bytes travel: an upload puts the object first under an id the service picks
(`Image` implements `Persistable`, so the save is a plain insert), then inserts the row in a short
transaction; if anything fails, the object is deleted again. `url` is absolute
(`ImageUrls`, from the request's host) and **open without a token**, since an `<img>` sends none:
the id is a random UUID. An upload no card refers to is removed after `orphan-ttl` (24 h) by
`OrphanImageCleanupJob`, every `cleanup-interval`: one row at a time, `SELECT … FOR UPDATE` and
then a fresh check that no card uses it (JPA's PESSIMISTIC_WRITE is FOR NO KEY UPDATE on
PostgreSQL and would not stop a foreign-key check), then the object outside the transaction.
Without the lock, a card saved with an old upload during the sweep would lose it by cascade.

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
- Uploaded pictures sit in `IMAGES_DIR` (`./data/images`, git-ignored) with `IMAGES_STORAGE=LOCAL`,
  or in the bucket from `S3_*` with `S3`. Files of a deleted account are not swept yet.
- Not built yet, though the client already calls them: password reset. Its templates exist and
  `one_time_codes` already allows `RESET_PASSWORD`.
- Spring Boot 4.2, Java 21, Lombok, MapStruct 1.6, PostgreSQL 17 (run locally from `db/`: `docker compose up -d`, data in `db/data`).

## Where to start

- A new feature: a Flyway migration, then `persistence/<feature>/{entity,repository,dao,mapper}`,
  `dto/<feature>/{request,response}`, `service/<feature>`, `api/<feature>`, and a path in
  `ApiPaths`.
- A new open endpoint: add it to the `permitAll()` list in `SecurityConfig`.
- Settings: `src/main/resources/application.properties`.
