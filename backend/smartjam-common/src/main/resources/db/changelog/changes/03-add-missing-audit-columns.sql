-- liquibase formatted sql

-- changeset sanjar:14
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE connections ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE assignments ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE comments ADD COLUMN updated_at TIMESTAMP;

-- changeset sanjar:15
UPDATE users SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE connections SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE assignments SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE comments SET updated_at = created_at WHERE updated_at IS NULL;