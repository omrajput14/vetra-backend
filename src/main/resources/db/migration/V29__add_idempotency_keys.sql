  -- V29: Add idempotency_keys table for offline-first request deduplication
CREATE TABLE IF NOT EXISTS idempotency_keys (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id          UUID NOT NULL,
  idempotency_key  VARCHAR(64) NOT NULL,
  request_path     VARCHAR(255) NOT NULL,
  response_status  INT NOT NULL,
  response_body    TEXT NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at       TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '48 hours'),
  CONSTRAINT uq_idempotency_user_key_path UNIQUE (user_id, idempotency_key, request_path)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_keys_expires ON idempotency_keys(expires_at);
CREATE INDEX IF NOT EXISTS idx_idempotency_keys_user ON idempotency_keys(user_id);
