-- Self-service account deletion with a grace period: status PENDING_DELETION plus the time the
-- account will be permanently deleted. The purge job looks accounts up by these two columns.
ALTER TABLE users ADD COLUMN deletion_scheduled_at TIMESTAMP(6) WITH TIME ZONE;
CREATE INDEX ix_users_pending_deletion ON users (status, deletion_scheduled_at);
