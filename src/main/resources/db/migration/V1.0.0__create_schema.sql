-- Everything of the application lives in its own schema; Flyway's history table stays in public.
CREATE SCHEMA IF NOT EXISTS prep_deck;
