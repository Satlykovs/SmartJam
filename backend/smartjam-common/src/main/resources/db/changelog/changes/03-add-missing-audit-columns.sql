-- liquibase formatted sql

-- changeset sanjar:14
ALTER TABLE users ADD COLUMN updated_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE connections ADD COLUMN updated_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE assignments ADD COLUMN updated_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE comments ADD COLUMN updated_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE refresh_tokens ADD COLUMN updated_at TIMESTAMPTZ DEFAULT NOW();

-- changeset sanjar:15
UPDATE users SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE connections SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE assignments SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE comments SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE refresh_tokens SET updated_at = created_at WHERE updated_at IS NULL;


-- changeset sanjar:16
ALTER TABLE users ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE connections ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE assignments ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE comments ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE refresh_tokens ALTER COLUMN updated_at SET NOT NULL;