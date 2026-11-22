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
- Collaborators arrive by constructor: `private final` fields with Lombok's
  `@RequiredArgsConstructor`.
- Three layers, each calling only the one below it:
  - `persistence/<feature>/repository` — Spring Data interfaces;
  - `persistence/<feature>/dao` — `<Name>DaoService` + `<Name>DaoServiceImpl`: plain CRUD over
    the repository, `@Transactional` here, no business rules;
  - `service/<feature>` — the web service: business logic, works through DAO services only and
    returns DTOs, never entities;
  - `api/<feature>` — `@RestController`s, thin, calling the web service.
- Entities live in `persistence/<feature>/entity`, mappers in `persistence/<feature>/mapper`.
- Mappers are MapStruct interfaces carrying `@Mapper(config = MapStructConfig.class)`.
- Every endpoint returns `ApiResponse<T>` (`success`, `message`, `data`). Failures become one in
  `GlobalExceptionHandler`; a controller never builds an error body by hand.
- Build with `mvn` (Java 21). The Maven wrapper is not in the repository.
