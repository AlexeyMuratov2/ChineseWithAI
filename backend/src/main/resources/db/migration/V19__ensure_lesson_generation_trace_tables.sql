-- Repair drift: schema may be at v17+ without lesson_generation_runs / stages (e.g. V18 skipped owner cleanup).
-- Align with JPA: no owner_id on lesson_generation_runs (removed in V18 when table existed).

CREATE TABLE IF NOT EXISTS lesson_generation_runs (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL,
    module_key VARCHAR(120) NOT NULL,
    pipeline_key VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    lesson_id UUID NULL,
    final_generator_session_id UUID NULL,
    failure_reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_lesson_generation_runs_draft_id
        FOREIGN KEY (draft_id) REFERENCES lesson_drafts (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_generation_runs_module_key
        FOREIGN KEY (module_key) REFERENCES lesson_modules (module_key) ON DELETE RESTRICT,
    CONSTRAINT fk_lesson_generation_runs_lesson_id
        FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE SET NULL,
    CONSTRAINT fk_lesson_generation_runs_final_generator_session_id
        FOREIGN KEY (final_generator_session_id) REFERENCES agent_sessions (id) ON DELETE SET NULL,
    CONSTRAINT chk_lesson_generation_runs_pipeline_key_not_blank
        CHECK (btrim(pipeline_key) <> ''),
    CONSTRAINT chk_lesson_generation_runs_status
        CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS lesson_generation_run_stages (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    stage_index INT NOT NULL,
    stage_key VARCHAR(120) NOT NULL,
    agent_session_id UUID NULL,
    status VARCHAR(30) NOT NULL,
    output_json TEXT NULL,
    failure_reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_lesson_generation_run_stages_run_id
        FOREIGN KEY (run_id) REFERENCES lesson_generation_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_generation_run_stages_agent_session_id
        FOREIGN KEY (agent_session_id) REFERENCES agent_sessions (id) ON DELETE SET NULL,
    CONSTRAINT chk_lesson_generation_run_stages_stage_index_non_negative
        CHECK (stage_index >= 0),
    CONSTRAINT chk_lesson_generation_run_stages_stage_key_not_blank
        CHECK (btrim(stage_key) <> ''),
    CONSTRAINT chk_lesson_generation_run_stages_status
        CHECK (status IN ('COMPLETED', 'FAILED')),
    CONSTRAINT uq_lesson_generation_run_stages_run_stage_index
        UNIQUE (run_id, stage_index),
    CONSTRAINT uq_lesson_generation_run_stages_run_stage_key
        UNIQUE (run_id, stage_key)
);

CREATE INDEX IF NOT EXISTS idx_lesson_generation_run_stages_run_stage_index
    ON lesson_generation_run_stages (run_id, stage_index);
