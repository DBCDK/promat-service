CREATE TABLE reviewerdatastash
(
    id              SERIAL PRIMARY KEY NOT NULL,
    reviewerId      integer,
    FOREIGN KEY (reviewerid) REFERENCES promatuser (id),
    reviewer        pg_catalog.jsonb,
    stashTime       timestamp default now()
);
ALTER TABLE promatuser ADD COLUMN lastChanged TEXT DEFAULT now();