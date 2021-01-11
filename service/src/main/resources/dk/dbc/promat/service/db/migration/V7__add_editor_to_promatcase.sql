ALTER TABLE promatcase ADD COLUMN editor_id integer;
ALTER TABLE promatcase ADD FOREIGN KEY (editor_id) REFERENCES promatuser (id);
