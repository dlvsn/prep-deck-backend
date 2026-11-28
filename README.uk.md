# Prep Deck — бекенд

[English](README.md) · **Українська**

REST API застосунку [Prep Deck](https://github.com/dlvsn/prep-deck-frontend): картки з вкладеними
темами, відповіді в Markdown з картинками, інтервальні повторення й спільний доступ до тем.
Десктопний клієнт (Electron) і його майбутня вебверсія працюють лише з цим сервісом.

## Стек

- Java 21, Spring Boot 4.2 (Web MVC, Security з JWT, Data JPA, Validation, Mail)
- PostgreSQL 17, схемою керує Flyway; Hibernate лише перевіряє її
- MapStruct 1.6, Lombok
- AWS SDK v2 для картинок у S3-сумісному бакеті (Cloudflare R2) або файли на диску
- Docker для локальних PostgreSQL, Mailpit і самого застосунку

## Що надає

Усе під `/api`. Кожен ендпоінт відповідає `{ success, message, data }`; `message` — це ключ із файлів
перекладу клієнта (`auth.invalidCredentials`), а не текст.

| Розділ | Ендпоінти |
| --- | --- |
| Акаунти | `POST /auth/register`, `/auth/verify-email`, `/auth/resend-verification`, `/auth/login`, `/auth/refresh`, `/auth/logout`; `GET /auth/me`, `/auth/username-available` |
| Налаштування | `GET /settings`, `PUT /settings` |
| Теми | `GET /topics`, `POST /topics`, `PATCH /topics/{id}`, `POST /topics/{id}/move`, `DELETE /topics/{id}` |
| Картки | `GET /topics/{id}/cards`, `POST /topics/{id}/cards`, `GET/PATCH/DELETE /cards/{id}`, `POST /cards/{id}/duplicate`, `GET /cards/tags` |
| Картинки | `POST /images` (multipart), `GET /images/{id}/content` |
| Спільний доступ (власник) | `GET /topics/{id}/sharing`, `POST …/invites`, `DELETE …/invites/{inviteId}`, `PUT …/people/{userId}/topics`, `DELETE …/people/{userId}`, `PUT …/link`, `POST …/link/reset`, `DELETE …/link/people` |
| Спільні зі мною | `GET /shares`, `POST /shares/accept`, `POST /shares/{id}/seen`, `DELETE /shares/{id}`, `POST /shares/{id}/copy` |

Головне:

- **Сесії**: JWT access-токен на 15 хвилин і refresh-токен на 30 днів, у базі лише його SHA-256.
  Refresh-токен працює один раз; повторно використаний відкликає всю свою «родину».
- **Реєстрація** створює непідтверджений акаунт і надсилає шестизначний код; листи йдуть після
  коміту транзакції, в окремому потоці.
- **Картинки**: тому, що клієнт каже про файл, не довіряємо; формат, вага й розміри міряються на
  самих байтах. Завантаження, яких не використовує жодна картка, видаляються за добу.
- **Спільний доступ**: доступ до теми відкриває все її піддерево. Публічні посилання й запрошення
  лише створюють доступи, а доступ перевіряється на кожному запиті, тож відкликання діє одразу.
  Змінювати тему може лише власник.

## Запуск

### Усе в Docker

```
cp .env.example .env
docker compose up -d --build
```

Це збирає застосунок за `Dockerfile` і запускає три контейнери (`docker-compose.yaml`):

| Сервіс | Адреса | Що це |
| --- | --- | --- |
| `backend` | http://localhost:8080/api | Застосунок |
| `postgres` | localhost:5432 | PostgreSQL 17, дані в `db/data` |
| `mailpit` | http://localhost:8025 | Перехоплює всі листи, які надсилає бекенд |

`.env` тут необов’язковий: без нього діють значення за замовчуванням з `docker-compose.yaml`. Адреси
бази й пошти завжди вказують на контейнери. Лог — `docker compose logs -f backend`; зупинити все —
`docker compose down` (база лишається).

Швидка перевірка, якій не потрібен акаунт:

```
curl "http://localhost:8080/api/auth/username-available?username=someone"
```

### З IDE

Запусти лише базу й Mailpit, а застосунок — сам:

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

`db/docker-compose.yaml` запускає PostgreSQL з доступами з `db/.env`; `docker/docker-compose.yaml`
запускає Mailpit. Вони відкривають ті самі порти, що й повний compose, і використовують ту саму
`db/data`, тож запускай або так, або так, але не обидва разом.

Потрібні JDK 21 і Maven 3.9+ (Maven wrapper немає). Під час запуску застосунок читає `.env` поруч із
`pom.xml`; справжні змінні середовища мають пріоритет.

## Налаштування

Усі налаштування — змінні середовища (локально з `.env`; див. `.env.example`):

| Змінна | Що задає |
| --- | --- |
| `PREPDECK_DB_URL`, `PREPDECK_DB_USER`, `PREPDECK_DB_PASSWORD` | Підключення до PostgreSQL |
| `JWT_SECRET` | HMAC-ключ для access-токенів, щонайменше 32 байти. Порожньо: випадковий ключ на кожен запуск |
| `JWT_ISSUER`, `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL` | Видавець токенів і час їхнього життя (`15m`, `30d`) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | SMTP-сервер |
| `MAIL_FROM`, `MAIL_SUPPORT` | Відправник і адреса підтримки в листах |
| `IMAGES_STORAGE` | `LOCAL` (файли в `IMAGES_DIR`) або `S3` |
| `IMAGES_DIR` | Куди класти картинки при `LOCAL` |
| `S3_ENDPOINT`, `S3_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY` | Бакет при `S3` (R2: endpoint `https://<account id>.r2.cloudflarestorage.com`, регіон `auto`) |
| `SHARE_LINK_BASE_URL` | Як виглядають посилання для доступу: це, а далі токен (`prepdeck://topic/`) |

Продуктові обмеження (вага картинки, теги на картку, термін дії запрошення…) — у
`src/main/resources/application.properties`. Повний compose також читає `POSTGRES_PORT`,
`BACKEND_PORT` і `CORS_ALLOWED_ORIGINS` (за замовчуванням — dev-сервер клієнта і його вебконтейнер).

## Структура коду

```
src/main/java/denys/mazurenko/prepdeckbackend/
  api/<feature>/            REST-контролери, тонкі
  service/<feature>/        бізнес-логіка: <Name>WebService + Impl, повертає DTO
  persistence/<feature>/    entity, repository, dao (CRUD + транзакції), mapper (MapStruct)
  dto/<feature>/            записи запитів і відповідей
  config/  security/  mail/  exception/  job/  domain/
src/main/resources/
  db/migration/             міграції Flyway
  templates/                листи, <name>-<en|uk>.html
```

Кожен шар звертається лише до шару під собою. Правила — у [CLAUDE.md](CLAUDE.md); згенерована карта
коду (точки входу, дерева залежностей, сутності, налаштування) — у
[.claude/graph/PROJECT_GRAPH.md](.claude/graph/PROJECT_GRAPH.md), з архітектурними рішеннями на
початку.
