CREATE TABLE promatmessage
(
    id  SERIAL PRIMARY KEY NOT NULL,
    authorId INTEGER NOT NULL,
    authorFirstname TEXT NOT NULL,
    authorLastname TEXT NOT NULL,
    caseId INTEGER NOT NULL,
    messageText text,
    created date DEFAULT NOW(),
    isRead BOOLEAN DEFAULT FALSE,
    direction TEXT NOT NULL
);

-- Set starting sequence for new messages at 500000
ALTER SEQUENCE promatmessage_id_seq RESTART WITH 500000;

CREATE INDEX ON promatmessage (caseId);

ALTER TABLE promatmessage ADD FOREIGN KEY (authorId) REFERENCES promatuser(id);
ALTER TABLE promatmessage ADD FOREIGN KEY (caseId) REFERENCES promatcase(id) ON DELETE CASCADE;