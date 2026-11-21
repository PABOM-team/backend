CREATE TABLE push_tokens
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    token        VARCHAR(255) NOT NULL,
    platform     VARCHAR(20)  NOT NULL,
    app_variant  VARCHAR(20)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    last_seen_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_push_tokens PRIMARY KEY (id),
    CONSTRAINT uk_push_tokens_token UNIQUE (token),
    CONSTRAINT fk_push_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    INDEX idx_push_tokens_user_id (user_id)
);

CREATE TABLE notification_settings
(
    user_id               BIGINT      NOT NULL,
    care_reminder_enabled BOOLEAN     NOT NULL,
    care_reminder_time    TIME        NOT NULL,
    time_zone             VARCHAR(64) NOT NULL,
    created_at            DATETIME(6) NOT NULL,
    updated_at            DATETIME(6) NOT NULL,
    CONSTRAINT pk_notification_settings PRIMARY KEY (user_id),
    CONSTRAINT fk_notification_settings_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT
);

CREATE TABLE notification_logs
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    user_id           BIGINT      NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    target_date       DATE        NOT NULL,
    created_at        DATETIME(6) NOT NULL,
    CONSTRAINT pk_notification_logs PRIMARY KEY (id),
    CONSTRAINT uk_notification_logs_user_type_date UNIQUE (user_id, notification_type, target_date),
    CONSTRAINT fk_notification_logs_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT
);

CREATE TABLE push_tickets
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    ticket_id  VARCHAR(100) NOT NULL,
    token      VARCHAR(255) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_push_tickets PRIMARY KEY (id),
    CONSTRAINT uk_push_tickets_ticket_id UNIQUE (ticket_id),
    INDEX idx_push_tickets_created_at (created_at)
);
