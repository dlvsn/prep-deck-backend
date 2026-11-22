## What this is

The backend of **prep-deck**, a desktop app for preparing for interviews, languages and any other
subject: question → answer cards in nested topics, spaced-repetition practice, and topics shared
with other people. Built as a pet project to show what can be done with AI assistance.

The client is a separate repository (`prep-deck-client`, an Electron application) that sits next
to this one under one parent folder. A web version of the same client is planned. The client's
spec — topics, cards, practice grades, sharing roles — is what this API is being built to serve.

The project is at its very start: the application class, the response envelope, the MapStruct
config and the exception handler. No persistence yet; no database has been chosen.

## Decisions already taken

**One backend, on a server.** Spring runs in one place and every client reaches it over HTTP.
The desktop installer does not carry Java or this service with it: bundling it would make the
installer hundreds of megabytes larger and give every machine a database of its own, and the
point of several clients is that a user sees the same data from any of them.

**The clients know this service through its REST API alone.** Nothing here may depend on which
client is calling, so a desktop-only shortcut (reading the user's disk, say) is never an option.

**One response shape.** Every endpoint answers with `ApiResponse<T>`: `success`, `message`,
`data`. The client unwraps `data` on success and shows `message` on failure, so a failure must
carry a message fit for a user. `GlobalExceptionHandler` is where exceptions become that shape;
the HTTP status is still set truthfully.

**Layers.** Repository → DAO service (CRUD only, transactions) → web service (business rules,
DTOs) → controller. Each layer calls only the one beneath it; a controller never touches a
repository, and a web service never returns an entity.

## Things to know before changing code

- `spring-boot-starter-security` is on the classpath and no `SecurityFilterChain` is defined yet,
  so every endpoint is locked behind HTTP Basic with a password generated at start-up. A new
  endpoint answering `401` is that, not a bug in it. Login comes later.
- The browser client will run on another origin than the API, so CORS has to be configured once
  the web version exists.
- Spring Boot 4.2, Java 21, Lombok, MapStruct 1.6 (Lombok runs first through
  `lombok-mapstruct-binding`).

## Where to start

- A new feature: `persistence/<feature>/{entity,repository,dao,mapper}`, then
  `service/<feature>`, then `api/<feature>`.
- Settings: `src/main/resources/application.properties`.
