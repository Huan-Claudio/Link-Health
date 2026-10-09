CREATE TABLE evolution_photo_files (
    photo_id UUID PRIMARY KEY REFERENCES evolution_photo_records(id) ON DELETE CASCADE,
    encrypted_content BYTEA NOT NULL CHECK (octet_length(encrypted_content) BETWEEN 29 AND 5242910),
    encryption_version SMALLINT NOT NULL CHECK (encryption_version = 1),
    created_at TIMESTAMPTZ NOT NULL
);
