-- Self-service email verification. Existing accounts are considered already verified (they
-- predate this feature); only new signups start out unverified.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN verification_token_hash VARCHAR(100);
ALTER TABLE users ADD COLUMN verification_token_expires_at TIMESTAMP(6) WITH TIME ZONE;

-- A plain unique index is enough: both Postgres and H2 treat any number of NULLs in a unique
-- index as distinct, so accounts with no active token (the common case) never collide.
CREATE UNIQUE INDEX ux_users_verification_token_hash ON users (verification_token_hash);
