CREATE TABLE shopping_items (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    item_name VARCHAR(120) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(20) NOT NULL,
    purchased BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_shopping_items_patient_id ON shopping_items (patient_id);

CREATE TABLE recommended_products (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    product_name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    price NUMERIC(12, 2) CHECK (price >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    purchase_url VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_recommended_products_patient_id ON recommended_products (patient_id);
