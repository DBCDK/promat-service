ALTER TABLE promatcase ADD COLUMN weekcode TEXT DEFAULT NULL;
ALTER TABLE promatcase ADD COLUMN author TEXT DEFAULT NULL;
ALTER TABLE promatcase ADD COLUMN creator_id INTEGER DEFAULT NULL;
ALTER TABLE promatcase ADD FOREIGN KEY (creator_id) REFERENCES promatuser (id);
ALTER TABLE promatcase ADD COLUMN publisher TEXT DEFAULT NULL;