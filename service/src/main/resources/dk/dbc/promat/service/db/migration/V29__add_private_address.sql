ALTER TABLE promatuser ADD COLUMN privateAddress1 TEXT DEFAULT NULL;
ALTER TABLE promatuser ADD COLUMN privateAddress2 TEXT DEFAULT NULl;
ALTER TABLE promatuser ADD COLUMN privateZip TEXT DEFAULT NULL;
ALTER TABLE promatuser ADD COLUMN privateCity TEXT DEFAULT NULL;
ALTER TABLE promatuser ADD COLUMN privateEmail TEXT DEFAULT NULL;
ALTER TABLE promatuser ADD COLUMN privatephone TEXT DEFAULT NULL;
ALTER TABLE promatuser ADD COLUMN privateSelected BOOLEAN DEFAULT NULL;

ALTER TABLE promatuser ADD COLUMN selected BOOLEAN DEFAULT TRUE;