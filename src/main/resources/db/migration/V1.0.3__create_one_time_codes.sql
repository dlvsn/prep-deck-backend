-- An account is usable once its email is confirmed with a code from a letter.
ALTER TABLE prep_deck.users ADD COLUMN email_verified_at TIMESTAMPTZ;

-- Accounts made before confirmation existed count as confirmed.
UPDATE prep_deck.users SET email_verified_at = created_at;

-- Short codes sent by email: confirming the address now, resetting the password later.
CREATE TABLE prep_deck.one_time_codes
(
    id            UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    purpose       VARCHAR(32) NOT NULL,
    -- SHA-256 of "<user id>:<code>", hex. The code itself only travels in the letter.
    code_hash     VARCHAR(64) NOT NULL,
    -- Wrong guesses still allowed; at 0 the code is spent and a new one must be requested.
    attempts_left INTEGER     NOT NULL,
    expires_at    TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Set when the code was accepted or burnt by the last wrong guess.
    used_at       TIMESTAMPTZ,
    CONSTRAINT ck_one_time_codes_purpose CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    CONSTRAINT ck_one_time_codes_attempts CHECK (attempts_left >= 0)
);

CREATE INDEX ix_one_time_codes_user_purpose ON prep_deck.one_time_codes (user_id, purpose, created_at DESC);
