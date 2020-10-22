CREATE TABLE subject
(
    id       serial PRIMARY KEY NOT NULL,
    name     text                NOT NULL,
    path     text UNIQUE,
    parentid integer,
    FOREIGN KEY (parentid) REFERENCES subject (id)
);
INSERT INTO subject(ID, NAME) VALUES (0, 'ROOT');