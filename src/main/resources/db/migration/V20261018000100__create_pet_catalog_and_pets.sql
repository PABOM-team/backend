CREATE TABLE species
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50)  NOT NULL,
    common_name   VARCHAR(100) NOT NULL,
    category      VARCHAR(20)  NOT NULL,
    display_order INT          NOT NULL,
    CONSTRAINT pk_species PRIMARY KEY (id),
    CONSTRAINT uk_species_code UNIQUE (code),
    CONSTRAINT uk_species_category_display_order UNIQUE (category, display_order)
);

CREATE TABLE morphs
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    species_id    BIGINT       NOT NULL,
    code          VARCHAR(50)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    display_order INT          NOT NULL,
    CONSTRAINT pk_morphs PRIMARY KEY (id),
    CONSTRAINT uk_morphs_species_code UNIQUE (species_id, code),
    CONSTRAINT uk_morphs_species_display_order UNIQUE (species_id, display_order),
    CONSTRAINT fk_morphs_species
        FOREIGN KEY (species_id) REFERENCES species (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    INDEX idx_morphs_species_id (species_id)
);

CREATE TABLE life_stages
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(30)  NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    display_order INT          NOT NULL,
    CONSTRAINT pk_life_stages PRIMARY KEY (id),
    CONSTRAINT uk_life_stages_code UNIQUE (code),
    CONSTRAINT uk_life_stages_display_order UNIQUE (display_order)
);

CREATE TABLE pets
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    client_request_id  VARCHAR(36)  NOT NULL,
    species_id         BIGINT       NOT NULL,
    name               VARCHAR(20)  NOT NULL,
    custom_morph       VARCHAR(40)  NULL,
    hatched_at         DATE         NULL,
    life_stage_id      BIGINT       NULL,
    stage_certainty    VARCHAR(20)  NOT NULL,
    sex                VARCHAR(20)  NOT NULL,
    sex_certainty      VARCHAR(20)  NOT NULL,
    photo_file_id      BIGINT       NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    CONSTRAINT pk_pets PRIMARY KEY (id),
    CONSTRAINT uk_pets_user_client_request UNIQUE (user_id, client_request_id),
    CONSTRAINT fk_pets_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    CONSTRAINT fk_pets_species
        FOREIGN KEY (species_id) REFERENCES species (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    CONSTRAINT fk_pets_life_stage
        FOREIGN KEY (life_stage_id) REFERENCES life_stages (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    CONSTRAINT fk_pets_photo_file
        FOREIGN KEY (photo_file_id) REFERENCES files (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    INDEX idx_pets_user_created_at (user_id, created_at),
    INDEX idx_pets_species_id (species_id),
    INDEX idx_pets_life_stage_id (life_stage_id),
    INDEX idx_pets_photo_file_id (photo_file_id)
);

CREATE TABLE pet_morphs
(
    pet_id   BIGINT NOT NULL,
    morph_id BIGINT NOT NULL,
    CONSTRAINT pk_pet_morphs PRIMARY KEY (pet_id, morph_id),
    CONSTRAINT fk_pet_morphs_pet
        FOREIGN KEY (pet_id) REFERENCES pets (id)
            ON UPDATE RESTRICT
            ON DELETE CASCADE,
    CONSTRAINT fk_pet_morphs_morph
        FOREIGN KEY (morph_id) REFERENCES morphs (id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,
    INDEX idx_pet_morphs_morph_id (morph_id)
);

INSERT INTO species (code, common_name, category, display_order)
SELECT 'CRESTED', '크레스티드 게코', 'LIZARD', 1
WHERE NOT EXISTS (SELECT 1 FROM species WHERE code = 'CRESTED');

INSERT INTO species (code, common_name, category, display_order)
SELECT 'LEOPARD', '레오파드 게코', 'LIZARD', 2
WHERE NOT EXISTS (SELECT 1 FROM species WHERE code = 'LEOPARD');

INSERT INTO species (code, common_name, category, display_order)
SELECT 'BALL_PYTHON', '볼파이톤', 'SNAKE', 1
WHERE NOT EXISTS (SELECT 1 FROM species WHERE code = 'BALL_PYTHON');

INSERT INTO morphs (species_id, code, name, display_order)
SELECT s.id, seed.code, seed.name, seed.display_order
FROM species s
JOIN (
    SELECT 'NORMAL' code, '노멀' name, 1 display_order
    UNION ALL SELECT 'HARLEQUIN', '할리퀸', 2
    UNION ALL SELECT 'FLAME', '플레임', 3
    UNION ALL SELECT 'DALMATIAN', '달마시안', 4
    UNION ALL SELECT 'PINSTRIPE', '핀스트라이프', 5
    UNION ALL SELECT 'LILLY_WHITE', '릴리 화이트', 6
) seed ON s.code = 'CRESTED'
WHERE NOT EXISTS (
    SELECT 1 FROM morphs m WHERE m.species_id = s.id AND m.code = seed.code
);

INSERT INTO morphs (species_id, code, name, display_order)
SELECT s.id, seed.code, seed.name, seed.display_order
FROM species s
JOIN (
    SELECT 'NORMAL' code, '노멀' name, 1 display_order
    UNION ALL SELECT 'HIGH_YELLOW', '하이옐로', 2
    UNION ALL SELECT 'TANGERINE', '탠저린', 3
    UNION ALL SELECT 'MACK_SNOW', '맥스노우', 4
    UNION ALL SELECT 'BLIZZARD', '블리자드', 5
    UNION ALL SELECT 'RAPTOR', '랩터', 6
) seed ON s.code = 'LEOPARD'
WHERE NOT EXISTS (
    SELECT 1 FROM morphs m WHERE m.species_id = s.id AND m.code = seed.code
);

INSERT INTO morphs (species_id, code, name, display_order)
SELECT s.id, seed.code, seed.name, seed.display_order
FROM species s
JOIN (
    SELECT 'NORMAL' code, '노멀' name, 1 display_order
    UNION ALL SELECT 'BANANA', '바나나', 2
    UNION ALL SELECT 'MOJAVE', '모하베', 3
    UNION ALL SELECT 'PIED', '파이드', 4
    UNION ALL SELECT 'CLOWN', '클라운', 5
) seed ON s.code = 'BALL_PYTHON'
WHERE NOT EXISTS (
    SELECT 1 FROM morphs m WHERE m.species_id = s.id AND m.code = seed.code
);

INSERT INTO life_stages (code, name, display_order)
SELECT 'HATCHLING', '해츨링', 1
WHERE NOT EXISTS (SELECT 1 FROM life_stages WHERE code = 'HATCHLING');

INSERT INTO life_stages (code, name, display_order)
SELECT 'JUVENILE', '준성체', 2
WHERE NOT EXISTS (SELECT 1 FROM life_stages WHERE code = 'JUVENILE');

INSERT INTO life_stages (code, name, display_order)
SELECT 'ADULT', '성체', 3
WHERE NOT EXISTS (SELECT 1 FROM life_stages WHERE code = 'ADULT');
