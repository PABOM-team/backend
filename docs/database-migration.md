# Database migration

## Current structure

- Database: MySQL 8.4
- Migration tool: Flyway
- Migration location: `src/main/resources/db/migration`
- File naming: `VyyyyMMddHHmmss__description.sql`
- Hibernate schema policy: `validate` for `local` and `prod`

Flyway applies versioned migrations in ascending version order and records the result in
`flyway_schema_history`. Existing versioned migration files must not be edited after they have
been applied. Add a new versioned migration for every schema change.

The initial migration creates the schema represented by the current JPA entities:

| Table | Main constraints and indexes |
| --- | --- |
| `users` | PK `id`, unique (`provider`, `provider_id`) |
| `refresh_tokens` | PK `id`, unique `token_hash`, indexes on `family_id` and `user_id`, FK to `users` |
| `user_agreements` | PK `id`, index on `user_id`, FK to `users` |
| `events` | PK `id`, index on `user_id`, FK to `users` |

No production seed data is required by the current code. Test fixtures remain test-only and must
not be added to versioned migrations.

## Existing unmanaged databases

Automatic baselining is disabled. If a database already contains tables but has no
`flyway_schema_history`, application startup stops before Flyway runs the initial migration. This
prevents duplicate table creation and avoids silently accepting an unknown schema.

Do not enable baselining until all of the following are complete:

1. Back up the target database.
2. Compare its tables, columns, constraints, indexes, and data types with the initial migration.
3. Resolve every difference with a separate reviewed migration.
4. Only when the existing schema is equivalent, baseline it at `20261011000100` using an explicit
   one-time deployment procedure.
5. Start the application with normal settings again and confirm that Hibernate validation passes.

Local database changes that exist outside this repository cannot be inferred from source code and
are intentionally not included. Production databases must not be inspected or changed as part of
local migration development.

## Operational impact

- Flyway runs before JPA initialization at application startup.
- Local schema auto-update is disabled; schema mismatches now fail startup.
- DDL can lock tables in an existing database. Review each future migration for locking, backfill,
  and rollback implications before deployment.
- The initial migration contains no destructive statements and no data modification statements.

## Verification

Use an isolated, empty MySQL 8.4 database. Do not reuse the normal local Docker volume.

1. Start the application and confirm the initial migration succeeds.
2. Confirm `flyway_schema_history` contains `20261011000100` with `success = 1`.
3. Confirm Hibernate schema validation succeeds.
4. Verify all PKs, FKs, unique constraints, and indexes listed above.
5. Restart against the same database and confirm no migration is executed twice.
