CREATE TABLE files
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id        BIGINT       NOT NULL,
    purpose         VARCHAR(30)  NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    width           INT          NOT NULL,
    height          INT          NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    temp_expires_at DATETIME(6)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_files PRIMARY KEY (id),
    CONSTRAINT uk_files_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_files_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    INDEX idx_files_owner_status (owner_id, status),
    INDEX idx_files_temp_expires_at (temp_expires_at)
);
