ALTER TABLE pets
    ADD COLUMN deleted_at DATETIME(6) NULL;

CREATE INDEX idx_pets_deleted_at ON pets (deleted_at);
