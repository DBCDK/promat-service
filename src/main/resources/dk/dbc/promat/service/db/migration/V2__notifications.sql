CREATE TABLE notification
(
    id SERIAL PRIMARY KEY NOT NULL,
    toaddress text NOT NULL ,
    subject text NOT NULL,
    bodytext text NOT NULL,
    status int NOT NULL,
    created date NOT NULL
);
