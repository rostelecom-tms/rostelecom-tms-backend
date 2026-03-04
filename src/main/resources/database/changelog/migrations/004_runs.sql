--liquibase formatted sql

--changeset dev:004-run-statuses
CREATE TABLE run_statuses
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE
);

--changeset dev:004-runs
CREATE TABLE runs
(
    id          SERIAL PRIMARY KEY,
    case_id     INTEGER   NOT NULL REFERENCES cases (id) ON DELETE RESTRICT,
    plan_id     INTEGER   NOT NULL REFERENCES plans (id) ON DELETE RESTRICT,
    status_id   INTEGER   NOT NULL REFERENCES run_statuses (id) ON DELETE RESTRICT,
    executed_by INTEGER   REFERENCES users (id) ON DELETE SET NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

--changeset dev:004-runs-indexes
CREATE INDEX idx_runs_case_id ON runs (case_id);
CREATE INDEX idx_runs_plan_id ON runs (plan_id);
CREATE INDEX idx_runs_status_id ON runs (status_id);
CREATE INDEX idx_runs_executed_by ON runs (executed_by);
CREATE INDEX idx_runs_executed_at ON runs (executed_at);