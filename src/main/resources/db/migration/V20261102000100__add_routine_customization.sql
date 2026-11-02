ALTER TABLE routines
    ADD COLUMN start_date DATE NULL;

ALTER TABLE routines
    ADD COLUMN start_date_custom BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE routines
    ADD COLUMN modification_reason VARCHAR(30) NULL;

ALTER TABLE routines
    ADD COLUMN modification_reason_note VARCHAR(100) NULL;

ALTER TABLE routines
    ADD COLUMN user_modified_at DATETIME(6) NULL;
