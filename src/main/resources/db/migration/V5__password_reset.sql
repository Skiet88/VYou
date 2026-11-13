-- Self-service password reset. Existing accounts are considered to have changed their password
-- at account creation, since that's the only password they've ever had.
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP(6) WITH TIME ZONE;
UPDATE users SET password_changed_at = created_at WHERE password_changed_at IS NULL;
ALTER TABLE users ALTER COLUMN password_changed_at SET NOT NULL;

ALTER TABLE users ADD COLUMN password_reset_token_hash VARCHAR(100);
ALTER TABLE users ADD COLUMN password_reset_token_expires_at TIMESTAMP(6) WITH TIME ZONE;

-- A plain unique index: both Postgres and H2 treat any number of NULLs in a unique index as
-- distinct, so accounts with no active token (the common case) never collide.
CREATE UNIQUE INDEX ux_users_password_reset_token_hash ON users (password_reset_token_hash);
