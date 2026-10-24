-- Preserva os dados existentes; o banco rejeita a migração se houver produtos órfãos.
ALTER TABLE recommended_products
    ADD CONSTRAINT fk_recommended_products_patient
    FOREIGN KEY (patient_id) REFERENCES patients (user_id) ON DELETE CASCADE;
