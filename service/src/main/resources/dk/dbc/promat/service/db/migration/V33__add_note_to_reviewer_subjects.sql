CREATE TABLE subjectnote
(
    id         serial PRIMARY KEY NOT NULL,
    subject_id int                NOT NULL,
    note       text               NOT NULL,
    FOREIGN KEY (subject_id) REFERENCES subject (id)
);

CREATE TABLE reviewersubjectnotes
(
    subjectnote_id int NOT NULL,
    reviewer_id int NOT NULL,
    PRIMARY KEY (subjectnote_id,  reviewer_id),
    FOREIGN KEY (subjectnote_id) REFERENCES subjectnote (id),
    FOREIGN KEY (reviewer_id) REFERENCES promatuser (id)
);

-- Set starting sequence for new subjectNote at 500000
ALTER SEQUENCE subjectnote_id_seq RESTART WITH 500000;
