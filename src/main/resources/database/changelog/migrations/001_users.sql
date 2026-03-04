--liquibase formatted sql

--changeset dev:001-extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

--changeset dev:001-user-roles
CREATE TABLE IF NOT EXISTS user_roles
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE
);

--changeset dev:001-users
CREATE TABLE IF NOT EXISTS users
(
    id            SERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    username      VARCHAR(100) NOT NULL UNIQUE,
    role_id       INTEGER      NOT NULL REFERENCES user_roles (id) ON DELETE RESTRICT,
    password_hash TEXT         NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMP
);

--changeset dev:001-users-indexes
CREATE INDEX idx_users_role_id ON users (role_id);
CREATE INDEX idx_users_deleted_at ON users (deleted_at) WHERE deleted_at IS NULL;