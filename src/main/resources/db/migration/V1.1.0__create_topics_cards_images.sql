-- Topics: a tree of folders per user. parent_id null = a root of the owner's tree.
CREATE TABLE prep_deck.topics
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    owner_id   UUID         NOT NULL REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    -- Deleting a topic takes its whole subtree and every card in it.
    parent_id  UUID REFERENCES prep_deck.topics (id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    -- Place among its siblings, 0 first.
    position   INTEGER      NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_topics_position CHECK (position >= 0)
);

CREATE INDEX ix_topics_owner ON prep_deck.topics (owner_id);
CREATE INDEX ix_topics_parent ON prep_deck.topics (parent_id);

-- Cards: a question and a Markdown answer, in one topic.
CREATE TABLE prep_deck.cards
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    topic_id   UUID         NOT NULL REFERENCES prep_deck.topics (id) ON DELETE CASCADE,
    question   VARCHAR(500) NOT NULL,
    answer     TEXT         NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_cards_answer_length CHECK (char_length(answer) <= 20000)
);

CREATE INDEX ix_cards_topic ON prep_deck.cards (topic_id, created_at DESC);

-- A card's tags in their order: lower-case, without "#", no duplicates (the application ensures it).
CREATE TABLE prep_deck.card_tags
(
    card_id  UUID        NOT NULL REFERENCES prep_deck.cards (id) ON DELETE CASCADE,
    position INTEGER     NOT NULL,
    tag      VARCHAR(40) NOT NULL,
    PRIMARY KEY (card_id, position)
);

CREATE INDEX ix_card_tags_tag ON prep_deck.card_tags (tag);

-- Uploaded pictures. The file itself lives in prep-deck.images.directory under the image id.
-- Uploads come before the card is saved; one no card refers to is removed after a while.
CREATE TABLE prep_deck.images
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    owner_id     UUID        NOT NULL REFERENCES prep_deck.users (id) ON DELETE CASCADE,
    content_type VARCHAR(16) NOT NULL,
    size_bytes   BIGINT      NOT NULL,
    width        INTEGER     NOT NULL,
    height       INTEGER     NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_images_content_type CHECK (content_type IN ('PNG', 'JPEG', 'WEBP', 'GIF')),
    CONSTRAINT ck_images_size CHECK (size_bytes > 0),
    CONSTRAINT ck_images_dimensions CHECK (width > 0 AND height > 0)
);

CREATE INDEX ix_images_created ON prep_deck.images (created_at);

-- A card's pictures in display order; the first is the main one. A duplicated card shares the
-- rows of images with its original, so one image may belong to several cards.
CREATE TABLE prep_deck.card_images
(
    card_id  UUID    NOT NULL REFERENCES prep_deck.cards (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    image_id UUID    NOT NULL REFERENCES prep_deck.images (id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, position)
);

CREATE INDEX ix_card_images_image ON prep_deck.card_images (image_id);
