-- liquibase formatted sql

-- changeset sanjar:8
CREATE UNIQUE INDEX idx_connections_teacher_student_unique
    ON connections(teacher_id, student_id)
    WHERE student_id IS NOT NULL;


-- changeset sanjar:9
CREATE INDEX idx_connections_teacher_id ON connections(teacher_id);
CREATE INDEX idx_connections_student_id ON connections(student_id);


-- changeset sanjar:10
CREATE INDEX idx_assignments_connection_id ON assignments(connection_id);


-- changeset sanjar:11
CREATE INDEX idx_submissions_assignment_id ON submissions(assignment_id);
CREATE INDEX idx_submissions_student_id ON submissions(student_id);


-- changeset sanjar:12
CREATE INDEX idx_comments_assignment_id ON comments(assignment_id);
CREATE INDEX idx_comments_author_id ON comments(author_id);


-- changeset sanjar:13
CREATE INDEX idx_submissions_feedback_gin ON submissions USING GIN (analysis_feedback);
