-- Table holding all cases
CREATE TABLE cases
(
    id              serial  PRIMARY KEY     NOT NULL,
    title           text                    NOT NULL,
    details         text                    NOT NULL,
    primaryFaust    text                    NOT NULL,
    relatedFausts   jsonb                   NOT NULL,
    reviewer        integer,
    created         date                    NOT NULL,
    deadline        date,
    assigned        date,
    status          text                    NOT NULL,
    materialType    text                    NOT NULL,

    FOREIGN KEY     (reviewer)              REFERENCES Reviewer (id)
);

-- Table holding tasks for cases
CREATE TABLE task
(
    id              serial  PRIMARY KEY     NOT NULL
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
