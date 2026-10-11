CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    provider      VARCHAR(20)  NOT NULL,
    provider_id   VARCHAR(191) NOT NULL,
    nickname      VARCHAR(100) NOT NULL,
    status        VARCHAR(30)  NOT NULL,
    last_login_at DATETIME(6)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id)
);

CREATE TABLE refresh_tokens
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    token_hash             VARCHAR(64) NOT NULL,
    user_id                BIGINT      NOT NULL,
    family_id              VARCHAR(36) NOT NULL,
    issued_at              DATETIME(6) NOT NULL,
    expires_at             DATETIME(6) NOT NULL,
    revoked_at             DATETIME(6) NULL,
    replaced_by_token_hash VARCHAR(64) NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    INDEX idx_refresh_tokens_family_id (family_id),
    INDEX idx_refresh_tokens_user_id (user_id)
);

CREATE TABLE user_agreements
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    agreement_type VARCHAR(30) NOT NULL,
    version        VARCHAR(30) NOT NULL,
    agreed         BOOLEAN     NOT NULL,
    agreed_at      DATETIME(6) NOT NULL,
    CONSTRAINT pk_user_agreements PRIMARY KEY (id),
    CONSTRAINT fk_user_agreements_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    INDEX idx_user_agreements_user_id (user_id)
);

CREATE TABLE events
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    event_code  VARCHAR(20)  NOT NULL,
    event_name  VARCHAR(100) NOT NULL,
    user_id     BIGINT       NOT NULL,
    is_new_user BOOLEAN      NOT NULL,
    occurred_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT fk_events_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    INDEX idx_events_user_id (user_id)
);
