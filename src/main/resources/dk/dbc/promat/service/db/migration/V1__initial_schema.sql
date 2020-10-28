CREATE TABLE subject
(
    id       serial PRIMARY KEY NOT NULL,
    name     text               NOT NULL,
    path     text UNIQUE,
    parentid integer,
    FOREIGN KEY (parentid) REFERENCES subject (id)
);

CREATE TABLE reviewer
(
    id           serial PRIMARY KEY NOT NULL,
    firstName    text               NOT NULL,
    lastName     text               NOT NULL,
    email        text               NOT NULL,
    phone        text,
    address1     text               NOT NULL,
    address2     text,
    zip          text               NOT NULL,
    city         text               NOT NULL,
    institution  text               NOT NULL,
    paycode      int,
    hiatus_begin date,
    hiatus_end   date
);

CREATE TABLE reviewersubjects
(
    subject_id  int NOT NULL,
    reviewer_id int NOT NULL,
    primary key (subject_id, reviewer_id),
    FOREIGN KEY (subject_id) REFERENCES subject (id),
    FOREIGN KEY (reviewer_id) REFERENCES reviewer (id)
);
