ALTER TABLE life_stages
    ADD COLUMN has_own_guideline_rules BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE life_stages
    ADD COLUMN guideline_fallback_stage_id BIGINT NULL;

UPDATE pets
SET life_stage_id = (SELECT juvenile.id FROM life_stages juvenile WHERE juvenile.code = 'JUVENILE')
WHERE life_stage_id IN (
    SELECT legacy_stage.id FROM life_stages legacy_stage
    WHERE legacy_stage.code IN ('HATCHLING', 'SUBADULT')
);

DELETE FROM life_stages WHERE code IN ('HATCHLING', 'SUBADULT');

INSERT INTO life_stages (code, name, display_order, has_own_guideline_rules, guideline_fallback_stage_id)
SELECT 'UNKNOWN', '확인 전', 4, FALSE, NULL
WHERE NOT EXISTS (SELECT 1 FROM life_stages WHERE code = 'UNKNOWN');

UPDATE life_stages SET display_order = 1, has_own_guideline_rules = TRUE WHERE code = 'JUVENILE';
UPDATE life_stages SET display_order = 2, has_own_guideline_rules = TRUE WHERE code = 'ADULT';
UPDATE life_stages SET display_order = 3, has_own_guideline_rules = FALSE WHERE code = 'UNKNOWN';
UPDATE life_stages
SET guideline_fallback_stage_id = (
    SELECT juvenile_id FROM (
        SELECT juvenile.id AS juvenile_id FROM life_stages juvenile WHERE juvenile.code = 'JUVENILE'
    ) juvenile_stage
)
WHERE code = 'UNKNOWN';

ALTER TABLE life_stages
    ADD CONSTRAINT fk_life_stages_guideline_fallback
        FOREIGN KEY (guideline_fallback_stage_id) REFERENCES life_stages (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE INDEX idx_life_stages_guideline_fallback
    ON life_stages (guideline_fallback_stage_id);

ALTER TABLE care_items
    ADD COLUMN guideline_record_type VARCHAR(20) NULL;

ALTER TABLE care_items
    ADD COLUMN requires_input_model_extension BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE care_item_code_mappings
(
    id                         BIGINT      NOT NULL AUTO_INCREMENT,
    legacy_care_item_id        BIGINT      NOT NULL,
    guideline_item_code        VARCHAR(50) NOT NULL,
    guideline_care_item_id     BIGINT      NULL,
    CONSTRAINT pk_care_item_code_mappings PRIMARY KEY (id),
    CONSTRAINT uk_care_item_code_mappings_legacy UNIQUE (legacy_care_item_id),
    CONSTRAINT uk_care_item_code_mappings_guideline_code UNIQUE (guideline_item_code),
    CONSTRAINT uk_care_item_code_mappings_guideline_item UNIQUE (guideline_care_item_id),
    CONSTRAINT fk_care_item_code_mappings_legacy FOREIGN KEY (legacy_care_item_id)
        REFERENCES care_items (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_item_code_mappings_guideline FOREIGN KEY (guideline_care_item_id)
        REFERENCES care_items (id) ON UPDATE RESTRICT ON DELETE RESTRICT
);

INSERT INTO care_item_code_mappings (legacy_care_item_id, guideline_item_code, guideline_care_item_id)
SELECT legacy.id, 'FEED', NULL FROM care_items legacy WHERE legacy.code = 'FEEDING';

INSERT INTO care_item_code_mappings (legacy_care_item_id, guideline_item_code, guideline_care_item_id)
SELECT legacy.id, 'SHED_CHECK', NULL FROM care_items legacy WHERE legacy.code = 'SHED';

CREATE TABLE care_guideline_versions
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    version_label  VARCHAR(100) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    effective_from DATE         NULL,
    published_at   DATETIME(6)  NULL,
    created_by     BIGINT       NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_care_guideline_versions PRIMARY KEY (id),
    CONSTRAINT uk_care_guideline_versions_label UNIQUE (version_label),
    CONSTRAINT fk_care_guideline_versions_user
        FOREIGN KEY (created_by) REFERENCES users (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_care_guideline_versions_status_effective (status, effective_from)
);

CREATE TABLE care_standards
(
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    guideline_version_id BIGINT        NOT NULL,
    species_id           BIGINT        NOT NULL,
    life_stage_id        BIGINT        NOT NULL,
    care_item_id         BIGINT        NOT NULL,
    default_cycle_days   INT           NOT NULL,
    min_days             INT           NOT NULL,
    max_days             INT           NOT NULL,
    record_type          VARCHAR(20)   NOT NULL,
    feed_result_set      VARCHAR(30)   NULL,
    default_food_name    VARCHAR(100)  NULL,
    default_enabled      BOOLEAN       NOT NULL,
    evidence_level       VARCHAR(30)   NOT NULL,
    evidence_source_label VARCHAR(100) NOT NULL,
    source_refs          TEXT          NOT NULL,
    CONSTRAINT pk_care_standards PRIMARY KEY (id),
    CONSTRAINT uk_care_standards_version_species_stage_item
        UNIQUE (guideline_version_id, species_id, life_stage_id, care_item_id),
    CONSTRAINT fk_care_standards_version FOREIGN KEY (guideline_version_id)
        REFERENCES care_guideline_versions (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_standards_species FOREIGN KEY (species_id)
        REFERENCES species (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_standards_stage FOREIGN KEY (life_stage_id)
        REFERENCES life_stages (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_standards_item FOREIGN KEY (care_item_id)
        REFERENCES care_items (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_care_standards_lookup (species_id, life_stage_id, guideline_version_id),
    INDEX idx_care_standards_item (care_item_id)
);

CREATE TABLE care_stage_thresholds
(
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    guideline_version_id   BIGINT        NOT NULL,
    species_id             BIGINT        NOT NULL,
    sex                    VARCHAR(20)   NOT NULL,
    ref_weight_g           DECIMAL(19,4) NOT NULL,
    min_age_months         INT           NOT NULL,
    hatch_adult_age_months INT           NOT NULL,
    CONSTRAINT pk_care_stage_thresholds PRIMARY KEY (id),
    CONSTRAINT uk_care_stage_thresholds_version_species_sex
        UNIQUE (guideline_version_id, species_id, sex),
    CONSTRAINT fk_care_stage_thresholds_version FOREIGN KEY (guideline_version_id)
        REFERENCES care_guideline_versions (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_stage_thresholds_species FOREIGN KEY (species_id)
        REFERENCES species (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_care_stage_thresholds_species (species_id, guideline_version_id)
);

CREATE TABLE care_item_descriptions
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    guideline_version_id BIGINT       NOT NULL,
    description_key      VARCHAR(150) NOT NULL,
    species_id           BIGINT       NULL,
    care_item_id         BIGINT       NOT NULL,
    description_text     TEXT         NOT NULL,
    CONSTRAINT pk_care_item_descriptions PRIMARY KEY (id),
    CONSTRAINT uk_care_item_descriptions_version_key
        UNIQUE (guideline_version_id, description_key),
    CONSTRAINT fk_care_item_descriptions_version FOREIGN KEY (guideline_version_id)
        REFERENCES care_guideline_versions (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_item_descriptions_species FOREIGN KEY (species_id)
        REFERENCES species (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_care_item_descriptions_item FOREIGN KEY (care_item_id)
        REFERENCES care_items (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_care_item_descriptions_lookup (species_id, care_item_id, guideline_version_id)
);
