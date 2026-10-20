CREATE TABLE care_items
(
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    code            VARCHAR(50)   NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    input_type      VARCHAR(20)   NOT NULL,
    unit            VARCHAR(20)   NULL,
    min_value       DECIMAL(19,4) NULL,
    max_value       DECIMAL(19,4) NULL,
    outcome_allowed BOOLEAN       NOT NULL DEFAULT FALSE,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_care_items PRIMARY KEY (id),
    CONSTRAINT uk_care_items_code UNIQUE (code)
);

INSERT INTO care_items (code, name, input_type, unit, min_value, max_value, outcome_allowed, active)
VALUES ('FEEDING', '먹이 주기', 'DETAIL', NULL, NULL, NULL, TRUE, TRUE),
       ('WEIGHT', '체중', 'NUMBER', 'g', NULL, NULL, FALSE, TRUE),
       ('SHED', '탈피', 'DETAIL', NULL, NULL, NULL, FALSE, TRUE),
       ('MEMO', '메모', 'MEMO', NULL, NULL, NULL, FALSE, TRUE);

CREATE TABLE routines
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    pet_id       BIGINT      NOT NULL,
    care_item_id BIGINT      NOT NULL,
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    interval_days INT        NULL,
    next_due_at  DATETIME(6) NULL,
    updated_at   DATETIME(6) NOT NULL,
    CONSTRAINT pk_routines PRIMARY KEY (id),
    CONSTRAINT uk_routines_pet_item UNIQUE (pet_id, care_item_id),
    CONSTRAINT fk_routines_pet FOREIGN KEY (pet_id) REFERENCES pets (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_routines_care_item FOREIGN KEY (care_item_id) REFERENCES care_items (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_routines_pet_active (pet_id, active)
);

CREATE TABLE care_records
(
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    user_id                BIGINT         NOT NULL,
    pet_id                 BIGINT         NOT NULL,
    care_item_id           BIGINT         NOT NULL,
    routine_id             BIGINT         NULL,
    client_request_id      VARCHAR(36)    NOT NULL,
    request_hash           VARCHAR(64)    NOT NULL,
    record_type            VARCHAR(20)    NOT NULL,
    source                 VARCHAR(20)    NOT NULL,
    occurred_at            DATETIME(6)    NOT NULL,
    time_precision         VARCHAR(20)    NOT NULL,
    outcome                VARCHAR(30)    NULL,
    value_num              DECIMAL(19,4)  NULL,
    detail_json            TEXT           NULL,
    memo                   VARCHAR(500)   NULL,
    photo_file_id          BIGINT         NULL,
    standard_version_label VARCHAR(100)   NULL,
    revision_no            INT            NOT NULL,
    deleted_at             DATETIME(6)    NULL,
    created_at             DATETIME(6)    NOT NULL,
    updated_at             DATETIME(6)    NOT NULL,
    CONSTRAINT pk_care_records PRIMARY KEY (id),
    CONSTRAINT uk_care_records_user_client_request UNIQUE (user_id, client_request_id),
    CONSTRAINT fk_care_records_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_records_pet FOREIGN KEY (pet_id) REFERENCES pets (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_records_item FOREIGN KEY (care_item_id) REFERENCES care_items (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_records_routine FOREIGN KEY (routine_id) REFERENCES routines (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_records_photo FOREIGN KEY (photo_file_id) REFERENCES files (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_care_records_user_occurred (user_id, occurred_at, id),
    INDEX idx_care_records_pet_item (pet_id, care_item_id),
    INDEX idx_care_records_routine (routine_id),
    INDEX idx_care_records_photo (photo_file_id),
    INDEX idx_care_records_deleted (deleted_at)
);

CREATE TABLE care_record_revisions
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    care_record_id      BIGINT       NOT NULL,
    change_type         VARCHAR(30)  NOT NULL,
    changed_fields_json TEXT         NOT NULL,
    before_json         TEXT         NOT NULL,
    after_json          TEXT         NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    CONSTRAINT pk_care_record_revisions PRIMARY KEY (id),
    CONSTRAINT fk_care_record_revisions_record FOREIGN KEY (care_record_id)
        REFERENCES care_records (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    INDEX idx_care_record_revisions_record (care_record_id, id)
);
