ALTER TABLE meal_plans ADD COLUMN published BOOLEAN NOT NULL DEFAULT FALSE;
-- O modelo anterior não distinguia rascunho de plano já substituído.
-- Conserva todos os planos anteriores como histórico, sem apagar dados.
-- A API permite copiá-los para um novo rascunho editável, preservando os originais.
UPDATE meal_plans SET published = TRUE;
ALTER TABLE meal_plans ADD CONSTRAINT ck_active_plan_published CHECK (NOT active OR published);
