--liquibase formatted sql

-- changeset sergey_sigma:1
ALTER TABLE users
    ADD COLUMN avatar_updated_at TIMESTAMP WITH TIME ZONE DEFAULT '1970-01-01 00:00:00+00' NOT NULL;
