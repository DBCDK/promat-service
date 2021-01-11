ALTER TABLE task RENAME TO promattask;
ALTER TABLE promattask RENAME COLUMN typeOfTask TO taskType;
ALTER TABLE promattask ADD COLUMN taskfieldtype text NOT NULL DEFAULT 'NONE';