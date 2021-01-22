ALTER TABLE promatcase ADD COLUMN trimmedWeekcode TEXT DEFAULT NULL;
CREATE INDEX ON promatcase (trimmedWeekcode);