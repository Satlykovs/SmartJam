-- changeset sergey_sigma: 1
DROP INDEX idx_comments_assignment_id;
CREATE INDEX idx_comments_assignment_pagination ON comments(assignment_id, created_at, id);