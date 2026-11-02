ALTER TABLE routines
    ADD COLUMN guideline_version_id BIGINT NULL;

ALTER TABLE routines
    ADD COLUMN cycle_source VARCHAR(20) NULL;

ALTER TABLE routines
    ADD CONSTRAINT fk_routines_guideline_version
        FOREIGN KEY (guideline_version_id) REFERENCES care_guideline_versions (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE INDEX idx_routines_guideline_version ON routines (guideline_version_id);

