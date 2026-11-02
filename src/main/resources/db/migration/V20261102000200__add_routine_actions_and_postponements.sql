ALTER TABLE routines
    ADD COLUMN last_done_on DATE NULL;
ALTER TABLE routines
    ADD COLUMN postponed_base_due DATE NULL;
ALTER TABLE routines
    ADD COLUMN postponed_at DATETIME(6) NULL;

ALTER TABLE care_records
    ADD COLUMN action_date DATE NULL;
ALTER TABLE care_records
    ADD COLUMN guideline_version_id BIGINT NULL;
ALTER TABLE care_records
    ADD COLUMN skip_reason_code VARCHAR(30) NULL;

ALTER TABLE care_records
    ADD CONSTRAINT uk_care_records_routine_action_date UNIQUE (routine_id, action_date);
ALTER TABLE care_records
    ADD CONSTRAINT fk_care_records_guideline_version
        FOREIGN KEY (guideline_version_id) REFERENCES care_guideline_versions (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE INDEX idx_care_records_guideline_version ON care_records (guideline_version_id);

UPDATE routines routine
SET last_done_on = (SELECT MAX(CAST(record.occurred_at AS DATE))
                    FROM care_records record
                    WHERE record.routine_id = routine.id
                      AND record.record_type = 'DONE'
                      AND record.deleted_at IS NULL);

CREATE TABLE routine_postponement_events
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    routine_id        BIGINT      NOT NULL,
    previous_due_on   DATE        NOT NULL,
    new_due_on        DATE        NULL,
    base_due_on       DATE        NOT NULL,
    event_type        VARCHAR(20) NOT NULL,
    created_at        DATETIME(6) NOT NULL,
    CONSTRAINT pk_routine_postponement_events PRIMARY KEY (id),
    CONSTRAINT fk_routine_postponement_events_routine FOREIGN KEY (routine_id)
        REFERENCES routines (id) ON UPDATE RESTRICT ON DELETE CASCADE,
    INDEX idx_routine_postponement_events_routine (routine_id, id)
);
