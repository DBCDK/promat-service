ALTER TABLE reviewer RENAME TO promatuser;
ALTER TABLE promatuser ADD COLUMN role TEXT NOT NULL;
ALTER TABLE promatuser ADD CONSTRAINT promatuser_role_constraint CHECK (role IN ('EDITOR', 'REVIEWER'));
CREATE INDEX ON promatuser(role);
ALTER TABLE reviewersubjects DROP CONSTRAINT reviewersubjects_subject_id_fkey;
ALTER TABLE reviewersubjects ADD CONSTRAINT  reviewersubjects_subject_id_fkey FOREIGN KEY(subject_id) REFERENCES subject(id) ON DELETE CASCADE;
ALTER TABLE reviewersubjects DROP CONSTRAINT reviewersubjects_reviewer_id_fkey;
ALTER TABLE reviewersubjects ADD CONSTRAINT reviewersubjects_reviewer_id_fkey FOREIGN KEY(reviewer_id) REFERENCES promatuser(id) ON DELETE CASCADE;