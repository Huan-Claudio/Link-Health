CREATE TABLE water_intakes (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    amount_milliliters INTEGER NOT NULL CHECK (amount_milliliters > 0 AND amount_milliliters <= 10000),
    consumed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_water_intakes_patient_consumed_at ON water_intakes (patient_id, consumed_at);

CREATE TABLE meal_records (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    meal_plan_item_id UUID,
    meal_name VARCHAR(120) NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_meal_records_patient_consumed_at ON meal_records (patient_id, consumed_at);

CREATE TABLE weight_records (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    weight_kg NUMERIC(6, 2) NOT NULL CHECK (weight_kg >= 1 AND weight_kg <= 500),
    measured_at TIMESTAMPTZ NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_weight_records_patient_measured_at ON weight_records (patient_id, measured_at);
