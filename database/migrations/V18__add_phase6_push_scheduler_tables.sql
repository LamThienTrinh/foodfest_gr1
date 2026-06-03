-- Phase 6: device tokens for push delivery when users are not actively opening the app.
CREATE TABLE IF NOT EXISTS push_device_tokens (
    push_device_token_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    platform VARCHAR(20) NOT NULL,
    token TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_push_device_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_push_device_tokens_user_platform_token
    ON push_device_tokens(user_id, platform, token);

CREATE INDEX IF NOT EXISTS idx_push_device_tokens_user_active
    ON push_device_tokens(user_id, is_active);

-- Phase 6: persistent job audit trail so scheduler runs and failures are visible.
CREATE TABLE IF NOT EXISTS notification_job_runs (
    notification_job_run_id SERIAL PRIMARY KEY,
    job_name VARCHAR(80) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    inserted_count INT NOT NULL DEFAULT 0,
    push_attempted_count INT NOT NULL DEFAULT 0,
    push_sent_count INT NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_notification_job_runs_job_started
    ON notification_job_runs(job_name, started_at DESC);

-- Phase 6: push delivery result log. Push failures must not block notification creation.
CREATE TABLE IF NOT EXISTS notification_delivery_logs (
    notification_delivery_log_id SERIAL PRIMARY KEY,
    notification_id INT NOT NULL,
    user_id INT NOT NULL,
    push_device_token_id INT,
    provider VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_delivery_logs_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(notification_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_delivery_logs_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_delivery_logs_device_token
        FOREIGN KEY (push_device_token_id) REFERENCES push_device_tokens(push_device_token_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_delivery_logs_notification
    ON notification_delivery_logs(notification_id, created_at DESC);
