CREATE TABLE follow_ups (
    id UUID PRIMARY KEY,
    nutritionist_id UUID NOT NULL REFERENCES nutritionists (user_id),
    patient_id UUID NOT NULL REFERENCES patients (user_id),
    status VARCHAR(10) NOT NULL CHECK (status IN ('PENDENTE', 'ATIVO', 'RECUSADO', 'INATIVO')),
    objective VARCHAR(100),
    water_goal_ml INTEGER CHECK (water_goal_ml > 0),
    invited_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_follow_ups_patient_invited_at ON follow_ups (patient_id, invited_at);
CREATE INDEX idx_follow_ups_nutritionist_invited_at ON follow_ups (nutritionist_id, invited_at);

CREATE UNIQUE INDEX uq_follow_ups_open_pair ON follow_ups (nutritionist_id, patient_id)
    WHERE status IN ('PENDENTE', 'ATIVO');
