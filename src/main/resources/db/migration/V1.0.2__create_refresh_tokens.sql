CREATE TABLE prep_deck.refresh_tokens
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    -- SHA-256 of the token, hex. The token itself is only ever in the client's hands.
    token_hash VARCHAR(64) NOT NULL,
    -- Every token issued by rotation from one login shares a family. A revoked token that comes
    -- back means it was stolen, and the whole family is revoked with it.
    family_id  UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_tokens_user ON prep_deck.refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family ON prep_deck.refresh_tokens (family_id);
