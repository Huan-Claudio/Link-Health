CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    birth_date DATE,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(13) NOT NULL CHECK (role IN ('PACIENTE', 'NUTRICIONISTA')),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (id, role)
);

CREATE TABLE patients (
    user_id UUID PRIMARY KEY,
    role VARCHAR(13) NOT NULL DEFAULT 'PACIENTE' CHECK (role = 'PACIENTE'),
    cpf VARCHAR(30) NOT NULL UNIQUE,
    FOREIGN KEY (user_id, role) REFERENCES user_accounts (id, role) ON DELETE CASCADE
);

CREATE TABLE nutritionists (
    user_id UUID PRIMARY KEY,
    role VARCHAR(13) NOT NULL DEFAULT 'NUTRICIONISTA' CHECK (role = 'NUTRICIONISTA'),
    professional_registration VARCHAR(30) NOT NULL UNIQUE,
    FOREIGN KEY (user_id, role) REFERENCES user_accounts (id, role) ON DELETE CASCADE
);
