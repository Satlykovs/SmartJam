-- liquibase formatted sql

-- changeset sanjar:17
ALTER TABLE users DROP COLUMN IF EXISTS fcm_token;

-- changeset sanjar:18
CREATE TABLE user_devices (
                              fcm_token VARCHAR(255) PRIMARY KEY,
                              user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              device_type VARCHAR(50) DEFAULT 'ANDROID',
                              updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- changeset sanjar:19
CREATE INDEX idx_user_devices_user_id ON user_devices(user_id);