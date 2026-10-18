-- A lista de compras pertence a uma conta de paciente já cadastrada.
-- Esta relação é adicionada em uma migração nova para não alterar a V1 já aplicada em outros bancos.
ALTER TABLE shopping_items
    ADD CONSTRAINT fk_shopping_items_patient
    FOREIGN KEY (patient_id) REFERENCES patients (user_id) ON DELETE CASCADE;
