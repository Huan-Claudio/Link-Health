-- Novos registros agrupam três imagens e uma pesagem. Os dados antigos são preservados.
CREATE TABLE evolution_assessments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(user_id),
    weight_record_id UUID NOT NULL UNIQUE REFERENCES weight_records(id),
    front_photo_id UUID NOT NULL UNIQUE REFERENCES evolution_photo_records(id),
    side_photo_id UUID NOT NULL UNIQUE REFERENCES evolution_photo_records(id),
    back_photo_id UUID NOT NULL UNIQUE REFERENCES evolution_photo_records(id),
    recorded_at TIMESTAMPTZ NOT NULL,
    request_digest CHAR(64) NOT NULL,
    CHECK (front_photo_id <> side_photo_id AND front_photo_id <> back_photo_id AND side_photo_id <> back_photo_id)
);
CREATE INDEX idx_evolution_assessments_patient_date ON evolution_assessments(patient_id, recorded_at DESC);
