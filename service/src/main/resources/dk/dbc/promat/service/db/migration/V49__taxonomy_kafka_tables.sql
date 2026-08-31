-- Flyway migration: files under db/migration are picked up automatically at application
-- startup (see DatabaseMigrator.java) based on their filename - "V49" is this migration's
-- version number, and everything after the double underscore is a free-text description used
-- only for logging. Flyway tracks which versions have already run in its own bookkeeping
-- table (schema_version, per this project's Flyway config) and only applies ones it hasn't
-- seen yet, in version order. Once a migration has run anywhere (staging, prod, a developer's
-- own database), its file must never be edited again - Flyway records a checksum of the file
-- alongside its version, and a mismatch on a later deploy fails validation. See V6 in this
-- same directory for a real example of what happens when that rule gets broken, and how to
-- recover from it.

-- `serial` is Postgres shorthand for "integer, backed by an auto-incrementing sequence" -
-- it's what TaxonomyCategory.java's `@GeneratedValue(strategy = GenerationType.IDENTITY)`
-- maps to; every new row gets the next number automatically, you never supply `id` yourself
-- on INSERT.
CREATE TABLE taxonomy_category
(
    id            serial PRIMARY KEY NOT NULL,
    parent_id     integer,
    name          text NOT NULL,
    is_leaf       boolean NOT NULL DEFAULT false,
    active        boolean NOT NULL DEFAULT false,
    display_order integer,
    -- A *self-referencing* foreign key: parent_id points at another row in this SAME table
    -- (taxonomy_category.id) - this is what actually implements the parent/child tree
    -- structure at the database level, mirrored by TaxonomyCategory.java's self-referencing
    -- @ManyToOne field.
    CONSTRAINT taxonomy_category_parent_id_fkey
        FOREIGN KEY (parent_id) REFERENCES taxonomy_category (id),
    -- A composite UNIQUE constraint across two columns together (not each individually) -
    -- guarantees no parent can have two children with the same name, while still allowing the
    -- same name to appear under different parents (e.g. nothing here stops two different
    -- top-level groups from each having a child literally named "genre", if that were ever
    -- needed - the uniqueness is scoped per-parent, not global).
    CONSTRAINT taxonomy_category_parent_id_name_key
        UNIQUE (parent_id, name)
);

CREATE TABLE taxonomy_subject
(
    -- Deliberately plain `integer`, NOT `serial`, unlike taxonomy_category.id above - this
    -- primary key is a "natural key" supplied by the caller (the subject's own id from the
    -- upstream Kafka messages), not something Postgres invents. See TaxonomySubject.java's
    -- @Id field (no @GeneratedValue there) for the Java side of this same choice.
    id               integer PRIMARY KEY NOT NULL,
    title            text NOT NULL,
    -- Postgres's native array type - `text[]` stores a genuine array value in one column,
    -- rather than needing a separate child table the way a relational-only design would
    -- require. See StringArrayConverter.java for how this maps to/from a Java String[] field.
    note             text[] NOT NULL DEFAULT '{}',
    often_used       boolean NOT NULL DEFAULT false,
    ref              text,
    category_id      integer NOT NULL,
    source_record_id text,
    -- `timestamptz` = "timestamp with time zone" - stores an actual point in time,
    -- unambiguous regardless of which time zone the database server or a client is in
    -- (compare to plain `timestamp`, which stores a date+time with NO time zone information,
    -- and is usually the wrong choice for anything meant to represent a real moment). `now()`
    -- is Postgres's current-timestamp function, used here as this column's default value.
    updated_at       timestamptz NOT NULL DEFAULT now(),
    -- ON DELETE RESTRICT: if something ever tries to DELETE a taxonomy_category row that a
    -- taxonomy_subject still references, Postgres refuses the delete outright, rather than
    -- either cascading the deletion (ON DELETE CASCADE, not used here - would silently delete
    -- subjects too) or nulling the reference out (ON DELETE SET NULL, also not used, and
    -- wouldn't even be legal here since category_id is NOT NULL). RESTRICT is the safest
    -- default when you never want an accidental category deletion to quietly take subjects
    -- down with it.
    CONSTRAINT taxonomy_subject_category_id_fkey
        FOREIGN KEY (category_id) REFERENCES taxonomy_category (id) ON DELETE RESTRICT
);

-- A "trigger function" - plpgsql (Postgres's own procedural SQL dialect) code that runs
-- automatically as part of certain data changes, defined here but not yet attached to
-- anything (that's the separate CREATE TRIGGER statement below). `NEW` refers to the row
-- being inserted/updated - this function's whole job is to look up whether NEW.category_id
-- actually points at a LEAF category, and abort the operation (RAISE EXCEPTION) if not.
-- This exists because a plain foreign key can only enforce "this category_id exists
-- somewhere in taxonomy_category" - it has no way to also express "...and specifically, one
-- with is_leaf = true". A database CHECK constraint can't do this either (it can't reference
-- OTHER rows/tables, only the current row's own columns) - a trigger is the standard Postgres
-- tool for constraints that need to look beyond the row currently being written.
CREATE OR REPLACE FUNCTION enforce_taxonomy_subject_leaf_category()
    RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM taxonomy_category
        WHERE id = NEW.category_id
          AND is_leaf = true
    ) THEN
        RAISE EXCEPTION 'taxonomy_subject.category_id % must reference a leaf taxonomy_category', NEW.category_id;
    END IF;

    RETURN NEW;
END
$$
LANGUAGE plpgsql;

-- Attaches the function above to actually run: BEFORE INSERT OR UPDATE means it fires prior
-- to each such write (giving it the chance to reject the write via RAISE EXCEPTION before any
-- data is actually changed), and FOR EACH ROW means it runs once per affected row rather than
-- once per statement (relevant for a multi-row INSERT ... VALUES (...), (...), (...), which
-- this project's seed migration below/next uses extensively).
CREATE TRIGGER taxonomy_subject_leaf_category_trigger
    BEFORE INSERT OR UPDATE ON taxonomy_subject
    FOR EACH ROW
EXECUTE FUNCTION enforce_taxonomy_subject_leaf_category();
