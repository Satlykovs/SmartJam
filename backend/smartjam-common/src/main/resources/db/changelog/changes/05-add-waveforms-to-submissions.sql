-- liquibase formatted sql
-- changeset sanjar:20
ALTER TABLE submissions ADD COLUMN teacher_waveform REAL[];
ALTER TABLE submissions ADD COLUMN student_waveform REAL[];