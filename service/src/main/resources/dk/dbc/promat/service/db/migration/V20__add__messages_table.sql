CREATE TABLE promatmessage
(
    id  SERIAL PRIMARY KEY NOT NULL,
    reviewer_id INTEGER NOT NULL,
    editor_id INTEGER NOT NULL ,
    promatcase_id INTEGER NOT NULL,
    messageText text,
    created date,
    isread BOOLEAN DEFAULT FALSE,
    direction TEXT NOT NULL
);

-- Set starting sequence for new messages at 500000
ALTER SEQUENCE promatmessage_id_seq RESTART WITH 500000;

CREATE INDEX ON promatmessage (promatcase_id);

ALTER TABLE promatmessage ADD FOREIGN KEY (reviewer_id) REFERENCES promatuser(id);
ALTER TABLE promatmessage ADD FOREIGN KEY (editor_id) REFERENCES promatuser(id);
ALTER TABLE promatmessage ADD FOREIGN KEY (promatcase_id) REFERENCES promatcase(id) ON DELETE CASCADE;