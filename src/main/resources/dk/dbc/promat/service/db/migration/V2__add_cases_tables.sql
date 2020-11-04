CREATE OR REPLACE FUNCTION CheckNoOpenCaseWithFaust(
    faust text)
    RETURNS bool AS $$
BEGIN
    RETURN NOT EXISTS (
            SELECT *
            FROM cases
            WHERE primaryFaust = faust
              AND status <> 'CLOSED'
              AND status <> 'DONE'
        );
END
$$
LANGUAGE plpgsql;

-- Table holding all cases
CREATE TABLE cases
(
    id              serial  PRIMARY KEY     NOT NULL,
    title           text                    NOT NULL,
    details         text                    NOT NULL,
    primaryFaust    text                    NOT NULL,
    relatedFausts   jsonb                   NOT NULL,
    reviewer_id     integer,
    created         date                    NOT NULL,
    deadline        date,
    assigned        date,
    status          text                    NOT NULL,
    materialType    text                    NOT NULL,

    FOREIGN KEY     (reviewer_id)           REFERENCES Reviewer (id),
    CHECK ( CheckNoOpenCaseWithFaust(primaryFaust) )
);

-- Set starting point for new case id's
ALTER SEQUENCE cases_id_seq RESTART WITH 500000;

-- Table holding tasks for cases
CREATE TABLE task
(
    id              serial  PRIMARY KEY     NOT NULL,
    typeOfTask      text                    NOT NULL,
    created         date                    NOT NULL,
    paycode         text                    NOT NULL,
    approved        date,
    payed           date,
    data            text
);

CREATE TABLE casesubjects
(
    subject_id      int                     NOT NULL,
    case_id         int                     NOT NULL,

    primary key     (subject_id, case_id),
    FOREIGN KEY     (subject_id)            REFERENCES subject (id),
    FOREIGN KEY     (case_id)               REFERENCES cases (id)
);

CREATE TABLE casetasks
(
    task_id         int                     NOT NULL,
    case_id         int                     NOT NULL,

    primary key     (task_id, case_id),
    FOREIGN KEY     (task_id)               REFERENCES task (id),
    FOREIGN KEY     (case_id)               REFERENCES cases (id)
);
