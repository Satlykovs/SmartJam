-- liquibase formatted sql

-- changeset sanjar:0
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- changeset sanjar:1
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(255) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(255),
                       last_name VARCHAR(255),
                       avatar_url VARCHAR(500),
                       fcm_token VARCHAR(255),
                       created_at TIMESTAMP DEFAULT NOW()
);

-- changeset sanjar:2
CREATE TABLE user_roles (
                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role VARCHAR(50) NOT NULL,
                            PRIMARY KEY (user_id, role)
);

-- changeset sanjar:3
CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY,
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token VARCHAR(255) UNIQUE NOT NULL,
                                expires_at TIMESTAMP NOT NULL
);

-- changeset sanjar:4
CREATE TABLE connections (
                             id UUID PRIMARY KEY,
                             teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             student_id UUID REFERENCES users(id) ON DELETE CASCADE,
                             invite_code VARCHAR(50) UNIQUE,
                             status VARCHAR(50) NOT NULL,
                             created_at TIMESTAMP DEFAULT NOW()
);

-- changeset sanjar:5
CREATE TABLE assignments (
                             id UUID PRIMARY KEY,
                             connection_id UUID NOT NULL REFERENCES connections(id) ON DELETE CASCADE,
                             status VARCHAR(50) DEFAULT 'AWAITING_UPLOAD',
                             title VARCHAR(255) NOT NULL,
                             description TEXT,
                             s3_reference_key VARCHAR(500),
                             reference_spectre_cache BYTEA,
                             error_message TEXT,
                             created_at TIMESTAMP DEFAULT NOW()

);

-- changeset sanjar:6
CREATE TABLE submissions (
                             id UUID PRIMARY KEY,
                             assignment_id UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
                             student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             s3_submission_key VARCHAR(500),
                             status VARCHAR(50) DEFAULT 'AWAITING_UPLOAD',
                             error_message TEXT,
                             pitch_score DOUBLE PRECISION,
                             rhythm_score DOUBLE PRECISION,
                             total_score DOUBLE PRECISION,
                             analysis_feedback JSONB,
                             created_at TIMESTAMP DEFAULT NOW(),
                             updated_at TIMESTAMP
);

-- changeset sanjar:7
CREATE TABLE comments (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          assignment_id UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
                          author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          text TEXT NOT NULL,
                          created_at TIMESTAMP DEFAULT NOW()
);