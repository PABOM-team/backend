ALTER TABLE care_records
    DROP INDEX uk_care_records_routine_action_date;

ALTER TABLE care_records
    ADD COLUMN active_action_date DATE
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN action_date ELSE NULL END);

ALTER TABLE care_records
    ADD CONSTRAINT uk_care_records_routine_active_action_date
        UNIQUE (routine_id, active_action_date);
