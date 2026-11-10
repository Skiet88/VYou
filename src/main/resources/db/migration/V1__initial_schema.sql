-- VYou2 initial schema. Never edit an applied migration; add V2__... instead.

CREATE TABLE users (
    id           UUID PRIMARY KEY,
    email        VARCHAR(254) NOT NULL,
    display_name VARCHAR(80)  NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE interview_sessions (
    id               UUID PRIMARY KEY,
    version          BIGINT       NOT NULL,
    user_id          UUID         NOT NULL REFERENCES users (id),
    job_role         VARCHAR(120) NOT NULL,
    level            VARCHAR(20)  NOT NULL,
    mode             VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    pending_question VARCHAR(2000),
    started_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    ended_at         TIMESTAMP(6) WITH TIME ZONE
);
CREATE INDEX ix_sessions_user_started ON interview_sessions (user_id, started_at);

CREATE TABLE interview_turns (
    id               UUID PRIMARY KEY,
    session_id       UUID           NOT NULL REFERENCES interview_sessions (id),
    sequence_number  INTEGER        NOT NULL,
    question         VARCHAR(2000)  NOT NULL,
    answer           VARCHAR(10000) NOT NULL,
    clarity_score    INTEGER        NOT NULL,
    technical_score  INTEGER        NOT NULL,
    confidence_score INTEGER        NOT NULL,
    overall_score    INTEGER        NOT NULL,
    strengths        VARCHAR(4000),
    improvements     VARCHAR(4000),
    answered_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_turns_session_sequence UNIQUE (session_id, sequence_number)
);

CREATE TABLE interview_summaries (
    id                 UUID PRIMARY KEY,
    session_id         UUID          NOT NULL REFERENCES interview_sessions (id),
    questions_answered INTEGER       NOT NULL,
    average_clarity    DOUBLE PRECISION NOT NULL,
    average_technical  DOUBLE PRECISION NOT NULL,
    average_confidence DOUBLE PRECISION NOT NULL,
    average_overall    DOUBLE PRECISION NOT NULL,
    feedback           VARCHAR(4000) NOT NULL,
    top_strengths      VARCHAR(4000),
    focus_areas        VARCHAR(4000),
    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_summaries_session UNIQUE (session_id)
);
