CREATE TABLE reviewerdatastash
(
    id              SERIAL PRIMARY KEY NOT NULL,
    reviewerId      integer,
    FOREIGN KEY (reviewerid) REFERENCES promatuser (id),
    reviewer        text,
    stashTime       timestamp default now()
);
ALTER TABLE promatuser ADD COLUMN lastChanged timestamp DEFAULT now();