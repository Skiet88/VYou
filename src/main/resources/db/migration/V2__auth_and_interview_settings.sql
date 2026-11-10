-- Adds authentication/authorization to users, per-interview settings, and widens the
-- columns that will hold AES-GCM ciphertext (base64 of IV + ciphertext + tag is larger
-- than the plaintext it replaces).
--
-- One column per ALTER TABLE statement: Postgres accepts a comma-separated list of
-- ADD COLUMN clauses in a single ALTER TABLE, but H2 (used for tests) does not, so this
-- file is written in the common subset both understand.

ALTER TABLE users ADD COLUMN password_hash VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN role           VARCHAR(20)  NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD COLUMN status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ALTER COLUMN password_hash DROP DEFAULT;
ALTER TABLE users ALTER COLUMN role DROP DEFAULT;
ALTER TABLE users ALTER COLUMN status DROP DEFAULT;

ALTER TABLE interview_sessions ADD COLUMN feedback_mode   VARCHAR(20) NOT NULL DEFAULT 'INSTANT';
ALTER TABLE interview_sessions ADD COLUMN total_questions INTEGER    NOT NULL DEFAULT 5;
ALTER TABLE interview_sessions ALTER COLUMN feedback_mode DROP DEFAULT;
ALTER TABLE interview_sessions ALTER COLUMN total_questions DROP DEFAULT;

-- Encrypted at the application layer (AES-GCM); plaintext length limits no longer apply.
ALTER TABLE interview_turns ALTER COLUMN answer TYPE TEXT;
ALTER TABLE interview_turns ALTER COLUMN strengths TYPE TEXT;
ALTER TABLE interview_turns ALTER COLUMN improvements TYPE TEXT;
ALTER TABLE interview_summaries ALTER COLUMN feedback TYPE TEXT;
ALTER TABLE interview_summaries ALTER COLUMN top_strengths TYPE TEXT;
ALTER TABLE interview_summaries ALTER COLUMN focus_areas TYPE TEXT;
