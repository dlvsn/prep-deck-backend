-- What a user picked, shared by every device they sign in on. One row per user, made on the first
-- save; until then the client keeps its own values and uploads them.
CREATE TABLE prep_deck.user_settings
(
    user_id       UUID PRIMARY KEY REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    theme_mode    VARCHAR(8)  NOT NULL,
    accent        VARCHAR(16) NOT NULL,
    language      VARCHAR(8)  NOT NULL,
    -- The topic opened last. A plain string while topics live only in the client; it becomes a
    -- UUID referencing the topics table once that exists.
    last_topic_id VARCHAR(64),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_user_settings_theme_mode CHECK (theme_mode IN ('DARK', 'LIGHT')),
    CONSTRAINT ck_user_settings_accent CHECK (accent IN ('MINT', 'PINK', 'PURPLE', 'BLUE', 'ORANGE', 'GREEN', 'YELLOW', 'RED')),
    CONSTRAINT ck_user_settings_language CHECK (language IN ('EN', 'UK'))
);
