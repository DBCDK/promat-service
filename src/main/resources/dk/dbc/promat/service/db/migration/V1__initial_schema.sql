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
    zip          int                NOT NULL,
    city         text               NOT NULL,
    institution  text               NOT NULL,
    paycode      int,
    hiatus_begin TIMESTAMP WITH TIME ZONE,
    hiatus_end   TIMESTAMP WITH TIME ZONE
);

CREATE TABLE reviewersubjects
(
    subject_id  int not null,
    reviewer_id int,
    FOREIGN KEY (subject_id) REFERENCES subject (id),
    FOREIGN KEY (reviewer_id) REFERENCES reviewer (id)
);
