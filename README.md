# Prep Deck — backend

**English** · [Українська](README.uk.md)

The REST API of [Prep Deck](https://github.com/dlvsn/prep-deck-frontend), a flashcard app with
nested topics, Markdown cards with pictures, spaced-repetition practice and topic sharing. The
desktop client (Electron) and its future web version talk to this service only.

## Stack

- Java 21, Spring Boot 4.2 (Web MVC, Security with JWT, Data JPA, Validation, Mail)
- PostgreSQL 17, schema owned by Flyway; Hibernate only validates
- MapStruct 1.6, Lombok
- AWS SDK v2 for pictures in an S3-compatible bucket (Cloudflare R2), or files on disk
- Docker for local PostgreSQL, Mailpit and the application itself

## What it serves

Everything lives under `/api`. Every endpoint answers `{ success, message, data }`; `message` is a
key from the client's translation files (`auth.invalidCredentials`), never prose.

| Area | Endpoints |
| --- | --- |
| Accounts | `POST /auth/register`, `/auth/verify-email`, `/auth/resend-verification`, `/auth/login`, `/auth/refresh`, `/auth/logout`; `GET /auth/me`, `/auth/username-available` |
| Settings | `GET /settings`, `PUT /settings` |
| Topics | `GET /topics`, `POST /topics`, `PATCH /topics/{id}`, `POST /topics/{id}/move`, `DELETE /topics/{id}` |
| Cards | `GET /topics/{id}/cards`, `POST /topics/{id}/cards`, `GET/PATCH/DELETE /cards/{id}`, `POST /cards/{id}/duplicate`, `GET /cards/tags` |
| Pictures | `POST /images` (multipart), `GET /images/{id}/content` |
| Sharing (owner) | `GET /topics/{id}/sharing`, `POST …/invites`, `DELETE …/invites/{inviteId}`, `PUT …/people/{userId}/topics`, `DELETE …/people/{userId}`, `PUT …/link`, `POST …/link/reset`, `DELETE …/link/people` |
| Shared with me | `GET /shares`, `POST /shares/accept`, `POST /shares/{id}/seen`, `DELETE /shares/{id}`, `POST /shares/{id}/copy` |

Highlights:

- **Sessions**: a 15-minute JWT access token and a 30-day refresh token kept only as a SHA-256. A
  refresh token works once; a reused one revokes its whole family.
- **Sign-up** creates the account unconfirmed and emails a six-digit code; letters go out after the
  transaction commits, on a thread of their own.
- **Pictures**: nothing the client says about a file is trusted; format, size and dimensions are
  measured on the bytes. Uploads no card uses are removed after a day.
- **Sharing**: a grant on a topic opens its whole subtree. Public links and email invitations only
  create grants, and grants are checked on every request, so taking access away works at once.
  Only the owner changes a topic.

## Running it

### Everything in Docker

```
cp .env.example .env
docker compose up -d --build
```

This builds the application from the `Dockerfile` and starts three containers
(`docker-compose.yaml`):

| Service | Address | What it is |
| --- | --- | --- |
| `backend` | http://localhost:8080/api | The application |
| `postgres` | localhost:5432 | PostgreSQL 17, data in `db/data` |
| `mailpit` | http://localhost:8025 | Catches every letter the backend sends |

`.env` is optional here: without it the defaults in `docker-compose.yaml` apply. Database and mail
hosts always point at the containers. Follow the log with `docker compose logs -f backend`; stop
everything with `docker compose down` (the database stays).

A quick check that does not need an account:

```
curl "http://localhost:8080/api/auth/username-available?username=someone"
```

### From an IDE

Start only the database and Mailpit, then run the application yourself:

```
cd db
cp .env.example .env
docker compose up -d

cd ../docker
docker compose up -d

cd ..
cp .env.example .env
mvn spring-boot:run
```

`db/docker-compose.yaml` starts PostgreSQL with the credentials from `db/.env`;
`docker/docker-compose.yaml` starts Mailpit. They publish the same ports as the full compose file
and share `db/data`, so run one way or the other, not both.

You need JDK 21 and Maven 3.9+ (there is no Maven wrapper). The application imports `.env` next to
`pom.xml` on start; real environment variables override it.

## Configuration

All settings come from environment variables (locally from `.env`; see `.env.example`):

| Variable | What it sets |
| --- | --- |
| `PREPDECK_DB_URL`, `PREPDECK_DB_USER`, `PREPDECK_DB_PASSWORD` | The PostgreSQL connection |
| `JWT_SECRET` | HMAC key for access tokens, at least 32 bytes. Empty: a random key per start |
| `JWT_ISSUER`, `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL` | Token issuer and lifetimes (`15m`, `30d`) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | The SMTP server |
| `MAIL_FROM`, `MAIL_SUPPORT` | The sender and the support address shown in letters |
| `IMAGES_STORAGE` | `LOCAL` (files in `IMAGES_DIR`) or `S3` |
| `IMAGES_DIR` | Where pictures go with `LOCAL` |
| `S3_ENDPOINT`, `S3_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY` | The bucket with `S3` (R2: endpoint `https://<account id>.r2.cloudflarestorage.com`, region `auto`) |
| `SHARE_LINK_BASE_URL` | What share links look like: this followed by a token (`prepdeck://topic/`) |

Product limits (picture size, tags per card, invitation lifetime…) are in
`src/main/resources/application.properties`. The full compose file also reads `POSTGRES_PORT`,
`BACKEND_PORT` and `CORS_ALLOWED_ORIGINS` (by default the client's dev server and its web
container).

## Code layout

```
src/main/java/denys/mazurenko/prepdeckbackend/
  api/<feature>/            REST controllers, thin
  service/<feature>/        business rules: <Name>WebService + Impl, returns DTOs
  persistence/<feature>/    entity, repository, dao (CRUD + transactions), mapper (MapStruct)
  dto/<feature>/            request and response records
  config/  security/  mail/  exception/  job/  domain/
src/main/resources/
  db/migration/             Flyway migrations
  templates/                letters, <name>-<en|uk>.html
```

Each layer calls only the one below it. Conventions are in [CLAUDE.md](CLAUDE.md); a generated map
of the code (entry points, injection trees, entities, settings) is in
[.claude/graph/PROJECT_GRAPH.md](.claude/graph/PROJECT_GRAPH.md), with the design decisions at its
top.
