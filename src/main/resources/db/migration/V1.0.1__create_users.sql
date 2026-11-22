CREATE TABLE prep_deck.users
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    -- Stored lower-cased by the application, so the unique constraints are case-insensitive.
    email         VARCHAR(320) NOT NULL,
    username      VARCHAR(32)  NOT NULL,
    display_name  VARCHAR(64)  NOT NULL,
    -- A DelegatingPasswordEncoder hash, "{bcrypt}$2a$...": never the password itself.
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email)),
    CONSTRAINT ck_users_username_lower CHECK (username = lower(username))
);
