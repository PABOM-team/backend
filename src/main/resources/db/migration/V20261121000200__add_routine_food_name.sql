ALTER TABLE routines
    ADD COLUMN food_name VARCHAR(20) NULL;

UPDATE care_standards
SET default_food_name = '해동 설치류'
WHERE guideline_version_id = (
        SELECT id FROM care_guideline_versions WHERE version_label = '1.2'
    )
  AND species_id = (
        SELECT id FROM species WHERE code = 'BALL_PYTHON'
    )
  AND care_item_id = (
        SELECT id FROM care_items WHERE code = 'FEED'
    )
  AND default_food_name = '해동 생쥐';
