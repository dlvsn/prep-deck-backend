# prep-deck-backend

## Read the graph first

`.claude/graph/PROJECT_GRAPH.md` maps this codebase: entry points, the injection tree behind each
one, entities and tables, settings and who reads them, tests and what they cover. Each comes with
the `file:line` to open. Read it before searching the sources.

It regenerates by itself through a `Stop` hook, so it is current as of the last turn. After
editing sources by hand, run `python .claude/graph/generate.py` to bring it up to date.

The prose at the top of it comes from `.claude/graph/overview.md`, which is hand-written: what
the application is for, the decisions already taken, where to start for a common change.
Everything below that is derived and overwritten on every run.

## Conventions

- Comments and javadoc in English. No comment at all beats one that restates the code.
- One type per file: no records, classes or enums nested inside another class.
- Paths and names of resources (template locations and names, CORS mappings, the error path)
  live in `application.properties` behind a `@ConfigurationProperties` class, never as literals.
- A closed set of values is an enum (`Language`, `CodePurpose`), not a `String`.
- Collaborators arrive by constructor: `private final` fields with Lombok's
  `@RequiredArgsConstructor`.
- Three layers, each calling only the one below it:
  - `persistence/<feature>/repository` — Spring Data interfaces;
  - `persistence/<feature>/dao` — `<Name>DaoService` + `<Name>DaoServiceImpl`: plain CRUD over
    the repository, `@Transactional` here, no business rules;
  - `service/<feature>` — the web service, `<Name>WebService` + `<Name>WebServiceImpl`: business
    logic, works through DAO services only and
    returns DTOs, never entities. It opens its own transaction only
    when one operation spans several DAO calls that must commit together (refresh-token rotation);
  - `api/<feature>` — `@RestController`s, thin, calling the web service.
- Entities live in `persistence/<feature>/entity`, mappers in `persistence/<feature>/mapper`.
- DTOs live in `dto`: `dto/<feature>/{request,response}` for one feature's (`RegisterRequest`,
  `SessionResponse`), `dto/common/{request,response}` for what several share (`ApiResponse`).
  Bodies coming in end in `Request`, bodies going out in `Response`; mappers call it `toResponse`.
- Mappers are MapStruct interfaces carrying `@Mapper(config = MapStructConfig.class)`.
- The context path is `/api` (`server.servlet.context-path`); paths in code (`ApiPaths`,
  security matchers) leave it out: `ApiPaths.AUTH = "/auth"` is served at `/api/auth`.
- Every endpoint returns `ApiResponse<T>` (`success`, `message`, `data`). Failures become one in
  `GlobalExceptionHandler`; a controller never builds an error body by hand.
- `message` is a key from the client's i18n files (`auth.emailTaken`, `errors.sessionExpired`),
  never prose: the client shows it in the user's language. Business refusals throw
  `ApiException(status, key)`; Bean Validation messages are keys too.
- Schema changes are Flyway SQL in `src/main/resources/db/migration/`, named
  `V<major>.<minor>.<patch>__<what>.sql` (`V1.0.1__create_users.sql`). Tables live in the
  `prep_deck` schema. Never edit a migration that has run anywhere; add the next one.
  Hibernate runs with `ddl-auto=validate` and refuses to start when an entity and its table disagree.
- Settings and secrets come from env variables; locally from `.env` next to `pom.xml`
  (git-ignored, keys listed in `.env.example`), which `application.properties` imports.
- A local PostgreSQL: `docker compose up -d` from `db/` (`db/docker-compose.yaml`, credentials in
  `db/.env`, copied from `db/.env.example`; data in `db/data`, git-ignored). They must match
  `PREPDECK_DB_*` in the root `.env`.
- Every other local service (Mailpit so far) comes up together from `docker/`:
  `docker compose up -d` there (`docker/docker-compose.yaml`).
- Letters: an event published in the transaction, sent by an `@TransactionalEventListener(AFTER_COMMIT)`
  + `@Async` listener through `EmailService`, from `resources/templates/<name>-<en|uk>.html`.
  Locally they land in Mailpit (http://localhost:8025, from `docker/docker-compose.yaml`).
- Build with `mvn` (Java 21). The Maven wrapper is not in the repository.
