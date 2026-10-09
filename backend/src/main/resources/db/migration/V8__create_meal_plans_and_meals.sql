CREATE TABLE meal_plans (
    id UUID PRIMARY KEY,
    follow_up_id UUID NOT NULL REFERENCES follow_ups (id),
    active BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_meal_plans_follow_up ON meal_plans (follow_up_id);
CREATE UNIQUE INDEX uq_meal_plans_active_follow_up ON meal_plans (follow_up_id) WHERE active;

CREATE TABLE planned_meals (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES meal_plans (id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL CHECK (length(trim(name)) > 0),
    meal_time TIME NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    UNIQUE (plan_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE meal_plan_items (
    id UUID PRIMARY KEY,
    meal_id UUID NOT NULL REFERENCES planned_meals (id) ON DELETE CASCADE,
    description VARCHAR(150) NOT NULL CHECK (length(trim(description)) > 0),
    position INTEGER NOT NULL CHECK (position >= 0),
    UNIQUE (meal_id, position) DEFERRABLE INITIALLY DEFERRED
);
