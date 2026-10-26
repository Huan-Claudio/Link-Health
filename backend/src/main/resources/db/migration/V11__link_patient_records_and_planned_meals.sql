-- Preserva os registros existentes. Dados órfãos devem ser corrigidos antes desta migração;
-- não são excluídos ou associados automaticamente a outra pessoa.
ALTER TABLE water_intakes ADD CONSTRAINT fk_water_intakes_patient
    FOREIGN KEY (patient_id) REFERENCES patients (user_id);
ALTER TABLE meal_records ADD CONSTRAINT fk_meal_records_patient
    FOREIGN KEY (patient_id) REFERENCES patients (user_id);
ALTER TABLE weight_records ADD CONSTRAINT fk_weight_records_patient
    FOREIGN KEY (patient_id) REFERENCES patients (user_id);
ALTER TABLE evolution_photo_records ADD CONSTRAINT fk_evolution_photo_records_patient
    FOREIGN KEY (patient_id) REFERENCES patients (user_id);

-- O nome legado meal_plan_item_id identifica a refeição planejada (planned_meals),
-- e não um alimento individual de meal_plan_items.
ALTER TABLE meal_records ADD CONSTRAINT fk_meal_records_planned_meal
    FOREIGN KEY (meal_plan_item_id) REFERENCES planned_meals (id);
CREATE UNIQUE INDEX uq_meal_completion_patient_day ON meal_records
    (patient_id, meal_plan_item_id, ((consumed_at AT TIME ZONE 'America/Sao_Paulo')::date))
    WHERE meal_plan_item_id IS NOT NULL;
