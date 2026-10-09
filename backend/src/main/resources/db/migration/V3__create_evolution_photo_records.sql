CREATE TABLE evolution_photo_records (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(30) NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png')),
    captured_at TIMESTAMPTZ NOT NULL,
    notes VARCHAR(500),
    upload_status VARCHAR(30) NOT NULL CHECK (upload_status IN ('PENDING_UPLOAD')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_evolution_photo_records_patient_captured_at
    ON evolution_photo_records (patient_id, captured_at DESC);
