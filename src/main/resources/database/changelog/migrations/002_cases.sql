--liquibase formatted sql

--changeset dev:002-case-groups
CREATE TABLE case_groups
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE
);

--changeset dev:002-cases
CREATE TABLE cases
(
    id             SERIAL PRIMARY KEY,
    title          VARCHAR(500) NOT NULL,
    group_id       INTEGER      NOT NULL REFERENCES case_groups (id) ON DELETE RESTRICT,
    description    TEXT,
    preconditions  TEXT,
    postconditions TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

--changeset dev:002-case-steps
CREATE TABLE case_steps
(
    id              SERIAL PRIMARY KEY,
    case_id         INTEGER NOT NULL REFERENCES cases (id) ON DELETE CASCADE,
    "order"         INTEGER NOT NULL,
    title           TEXT,
    action          TEXT    NOT NULL,
    expected_result TEXT,
    UNIQUE (case_id, "order")
);

--changeset dev:002-defects
CREATE TABLE defects
(
    id          SERIAL PRIMARY KEY,
    case_id     INTEGER   NOT NULL REFERENCES cases (id) ON DELETE CASCADE,
    title       TEXT      NOT NULL,
    description TEXT,
    is_solved   BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

--changeset dev:002-cases-indexes
CREATE INDEX idx_cases_group_id ON cases (group_id);
CREATE INDEX idx_case_steps_case_id ON case_steps (case_id, "order");
CREATE INDEX idx_defects_case_id ON defects (case_id);
CREATE INDEX idx_defects_is_solved ON defects (is_solved);