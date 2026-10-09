ALTER TABLE evolution_photo_records
    ADD COLUMN storage_key VARCHAR(255);

ALTER TABLE evolution_photo_records
    DROP CONSTRAINT evolution_photo_records_upload_status_check;

ALTER TABLE evolution_photo_records
    ADD CONSTRAINT evolution_photo_records_upload_status_check
    CHECK (upload_status IN ('PENDING_UPLOAD', 'STORED'));
