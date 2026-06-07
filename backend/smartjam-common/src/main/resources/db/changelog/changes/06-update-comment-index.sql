-- liquibase formatted sql

-- changeset sergey_sigma:1
DROP INDEX IF EXISTS idx_comments_assignment_id;
CREATE INDEX IF NOT EXISTS idx_comments_assignment_pagination ON comments(assignment_id, created_at, id);