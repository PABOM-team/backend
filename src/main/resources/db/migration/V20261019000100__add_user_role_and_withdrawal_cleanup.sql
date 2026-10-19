ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD COLUMN withdrawn_at DATETIME(6) NULL;
ALTER TABLE users MODIFY COLUMN provider_id VARCHAR(191) NULL;
ALTER TABLE users MODIFY COLUMN nickname VARCHAR(100) NULL;
ALTER TABLE users MODIFY COLUMN last_login_at DATETIME(6) NULL;

ALTER TABLE pets ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
CREATE INDEX idx_pets_user_status ON pets (user_id, status);

ALTER TABLE events
    MODIFY COLUMN user_id BIGINT NULL;

CREATE TABLE withdrawal_cleanup_jobs
(
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    user_id         BIGINT        NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    attempts        INT           NOT NULL,
    next_attempt_at DATETIME(6)   NOT NULL,
    last_error      VARCHAR(1000) NULL,
    created_at      DATETIME(6)   NOT NULL,
    completed_at    DATETIME(6)   NULL,
    CONSTRAINT pk_withdrawal_cleanup_jobs PRIMARY KEY (id),
    CONSTRAINT uk_withdrawal_cleanup_jobs_user UNIQUE (user_id),
    CONSTRAINT fk_withdrawal_cleanup_jobs_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    INDEX idx_withdrawal_cleanup_jobs_due (status, next_attempt_at)
);
