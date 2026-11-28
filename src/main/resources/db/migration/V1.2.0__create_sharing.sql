-- Sharing: an owner opens parts of a topic tree to other people. Only the owner changes a topic;
-- the people it is shared with view it (and will practise it with progress of their own).

-- Who may see what. A grant on a topic opens its whole subtree, subtopics added later included;
-- a subtopic can be granted on its own. Links and invitations only ever create grants: access is
-- checked against this table on every request, so a removed grant locks its holder out even if
-- they still have the link.
CREATE TABLE prep_deck.topic_grants
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    topic_id   UUID        NOT NULL REFERENCES prep_deck.topics (id) ON DELETE CASCADE,
    -- The topic's owner (a topic never changes owner), so shares group and block by owner cheaply.
    owner_id   UUID        NOT NULL REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    grantee_id UUID        NOT NULL REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    -- Only viewers for now; an editor role may follow, and the application already reads roles
    -- through one list of what each role allows.
    role       VARCHAR(16) NOT NULL,
    -- How the grantee came: a personal invitation (or the owner choosing their topics), or the
    -- tree's public link.
    source     VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Null until the grantee opens the topic: until then it shows as new in Shared with me.
    seen_at    TIMESTAMPTZ,
    CONSTRAINT uq_topic_grants UNIQUE (topic_id, grantee_id),
    CONSTRAINT ck_topic_grants_role CHECK (role IN ('VIEWER')),
    CONSTRAINT ck_topic_grants_source CHECK (source IN ('INVITE', 'LINK')),
    CONSTRAINT ck_topic_grants_not_owner CHECK (grantee_id <> owner_id)
);

CREATE INDEX ix_topic_grants_grantee ON prep_deck.topic_grants (grantee_id);
CREATE INDEX ix_topic_grants_owner ON prep_deck.topic_grants (owner_id);

-- The public link of a tree: anyone signed in who opens it gets the topics it lists.
CREATE TABLE prep_deck.share_links
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    -- The root of the tree the link belongs to: one link per tree.
    topic_id   UUID        NOT NULL REFERENCES prep_deck.topics (id) ON DELETE CASCADE,
    owner_id   UUID        NOT NULL REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    -- Kept as it is, not hashed: the owner copies the link again whenever they like. Resetting
    -- replaces it, and the old one stops working at once.
    token      VARCHAR(64) NOT NULL,
    enabled    BOOLEAN     NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_share_links_topic UNIQUE (topic_id),
    CONSTRAINT uq_share_links_token UNIQUE (token)
);

CREATE INDEX ix_share_links_owner ON prep_deck.share_links (owner_id);

-- The topics a link opens, all inside its tree.
CREATE TABLE prep_deck.share_link_topics
(
    link_id  UUID NOT NULL REFERENCES prep_deck.share_links (id) ON DELETE CASCADE,
    topic_id UUID NOT NULL REFERENCES prep_deck.topics (id) ON DELETE CASCADE,
    PRIMARY KEY (link_id, topic_id)
);

CREATE INDEX ix_share_link_topics_topic ON prep_deck.share_link_topics (topic_id);

-- Personal invitations by email. The letter carries a link of its own that only an account with
-- that email can accept, once, before it expires.
CREATE TABLE prep_deck.share_invites
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    -- The root of the tree the invitation is for.
    topic_id    UUID         NOT NULL REFERENCES prep_deck.topics (id) ON DELETE CASCADE,
    owner_id    UUID         NOT NULL REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    -- Lower-cased, like users.email.
    email       VARCHAR(320) NOT NULL,
    -- SHA-256 of the token, hex. The token itself only travels in the letter.
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    accepted_at TIMESTAMPTZ,
    revoked_at  TIMESTAMPTZ,
    CONSTRAINT uq_share_invites_token UNIQUE (token_hash),
    CONSTRAINT ck_share_invites_email_lower CHECK (email = lower(email))
);

CREATE INDEX ix_share_invites_owner_email ON prep_deck.share_invites (owner_id, email, created_at DESC);

-- The topics an invitation opens, all inside its tree.
CREATE TABLE prep_deck.share_invite_topics
(
    invite_id UUID NOT NULL REFERENCES prep_deck.share_invites (id) ON DELETE CASCADE,
    topic_id  UUID NOT NULL REFERENCES prep_deck.topics (id) ON DELETE CASCADE,
    PRIMARY KEY (invite_id, topic_id)
);

CREATE INDEX ix_share_invite_topics_topic ON prep_deck.share_invite_topics (topic_id);

-- People an owner took access from. They cannot come back through a public link; only a new
-- personal invitation, once accepted, lets them in again.
CREATE TABLE prep_deck.share_blocks
(
    owner_id   UUID        NOT NULL REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (owner_id, user_id)
);
