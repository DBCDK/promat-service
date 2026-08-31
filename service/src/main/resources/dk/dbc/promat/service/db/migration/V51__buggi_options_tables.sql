-- A simpler sibling of V49's taxonomy_category/taxonomy_subject: a plain one-to-many rather
-- than a self-referencing tree, since Buggi's tag vocabulary only ever needs one level of
-- grouping (see BuggiOptionGroup.java/BuggiOption.java for the JPA entities mapped to these
-- two tables).
CREATE TABLE buggi_option_group
(
    id                     serial PRIMARY KEY NOT NULL,
    name                   text NOT NULL,
    subfield_code          text NOT NULL,
    requires_nonzero_value boolean NOT NULL DEFAULT false,
    active                 boolean NOT NULL DEFAULT true,
    display_order          integer
);

CREATE TABLE buggi_option
(
    id            serial PRIMARY KEY NOT NULL,
    -- `REFERENCES buggi_option_group (id)` written directly on the column, rather than as a
    -- separate named CONSTRAINT ... FOREIGN KEY clause (compare to V49's taxonomy_subject,
    -- which uses the more verbose named-constraint style) - both forms create an equivalent
    -- foreign key; this shorthand is fine when you don't need to reference the constraint by
    -- name later (e.g. to drop or alter it in a future migration).
    group_id      integer NOT NULL REFERENCES buggi_option_group (id),
    name          text NOT NULL,
    display_order integer,
    active        boolean NOT NULL DEFAULT true,
    CONSTRAINT buggi_option_group_id_name_key UNIQUE (group_id, name)
);
