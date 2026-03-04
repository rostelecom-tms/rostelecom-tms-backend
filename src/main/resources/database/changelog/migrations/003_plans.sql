--liquibase formatted sql

--changeset dev:003-plans
CREATE TABLE plans
(
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    introduction        TEXT,
    approach            TEXT,
    start_date          DATE,
    end_date            DATE,
    responsible_user_id INTEGER REFERENCES users (id) ON DELETE SET NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

--changeset dev:003-plans-cases
CREATE TABLE plans_cases
(
    id      SERIAL PRIMARY KEY,
    plan_id INTEGER NOT NULL REFERENCES plans (id) ON DELETE CASCADE,
    case_id INTEGER NOT NULL REFERENCES cases (id) ON DELETE CASCADE,
    UNIQUE (plan_id, case_id)
);

--changeset dev:003-plans-indexes
CREATE INDEX idx_plans_responsible_user_id ON plans (responsible_user_id);
CREATE INDEX idx_plans_dates ON plans (start_date, end_date);
CREATE INDEX idx_plans_cases_plan_id ON plans_cases (plan_id);
CREATE INDEX idx_plans_cases_case_id ON plans_cases (case_id);