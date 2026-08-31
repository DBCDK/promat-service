-- Seeds the taxonomy_category tree with exactly what used to be hardcoded in
-- Taxonomy.java's constructor - see that file for the Java-side equivalent of this same
-- structure. Each INSERT below is one level of the tree; later INSERTs reference the rows
-- created by earlier ones via a subquery, since a child row's parent_id has to be an actual,
-- already-assigned id, not a name.

-- The four root categories: parent_id IS NULL marks a row with no parent, i.e. a top-level
-- group. display_order controls the order they're returned in when the tree is rebuilt
-- (see DbTaxonomyBuilder.java's CATEGORY_COMPARATOR) - without it, row order from a SELECT
-- would be unspecified in general.
INSERT INTO taxonomy_category (parent_id, name, is_leaf, active, display_order)
VALUES
    (NULL, 'ramme', false, true, 1),
    (NULL, 'handling', false, true, 2),
    (NULL, 'fortælleteknik', false, true, 3),
    (NULL, 'stemning', false, true, 4);

-- A subquery inside VALUES: `(SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND
-- name = 'ramme')` looks up the id Postgres just assigned the 'ramme' row above (via its
-- `serial` column) at the moment THIS statement runs, and uses that id as parent_id here -
-- this is how the migration links a child to its parent without needing to know the parent's
-- id ahead of time (which would be fragile - the actual id values depend on whatever else has
-- been inserted into this table before, e.g. during local testing).
INSERT INTO taxonomy_category (parent_id, name, is_leaf, active, display_order)
VALUES
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'ramme'), 'handlingens tid udtrykt i ord', true, true, 1),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'ramme'), 'handlingens tid udtrykt i tal', true, true, 2),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'ramme'), 'geografisk sted', true, true, 3),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'ramme'), 'fiktivt sted', true, true, 4),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'ramme'), 'miljø', true, true, 5),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'ramme'), 'genre', true, true, 6),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'ramme'), 'univers', true, true, 7);

-- Note 'hovedperson(er) - beskrivelse' has is_leaf = false, unlike its siblings here - it's a
-- GROUP node with its own children (inserted next), not a place subjects attach directly to.
-- This is the one branch of the whole tree that goes 3 levels deep instead of 2 (root ->
-- group -> leaf); everywhere else in this seed data is exactly 2 levels.
INSERT INTO taxonomy_category (parent_id, name, is_leaf, active, display_order)
VALUES
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'handling'), 'handler om', true, true, 1),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'handling'), 'navngivet hovedperson', true, true, 2),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'handling'), 'hovedperson(er) - beskrivelse', false, true, 3);

-- Resolving THIS level's parent id needs a self-JOIN (taxonomy_category joined against
-- itself, aliased as `child` and `parent`) rather than the simpler single-table subquery used
-- above - because "hovedperson(er) - beskrivelse" isn't unique by name alone across the whole
-- table in principle (the UNIQUE constraint from V49 only guarantees uniqueness per-parent),
-- so the subquery has to also pin down which "handling" its parent is, by walking through the
-- join: find a `child` row named 'hovedperson(er) - beskrivelse' whose `parent` row is itself
-- a root-level (parent_id IS NULL) row named 'handling'.
INSERT INTO taxonomy_category (parent_id, name, is_leaf, active, display_order)
VALUES
    ((SELECT child.id
      FROM taxonomy_category child
      JOIN taxonomy_category parent ON parent.id = child.parent_id
      WHERE parent.parent_id IS NULL
        AND parent.name = 'handling'
        AND child.name = 'hovedperson(er) - beskrivelse'), 'om hovedpersonen', true, true, 1),
    ((SELECT child.id
      FROM taxonomy_category child
      JOIN taxonomy_category parent ON parent.id = child.parent_id
      WHERE parent.parent_id IS NULL
        AND parent.name = 'handling'
        AND child.name = 'hovedperson(er) - beskrivelse'), 'hovedpersonens karaktertræk', true, true, 2),
    ((SELECT child.id
      FROM taxonomy_category child
      JOIN taxonomy_category parent ON parent.id = child.parent_id
      WHERE parent.parent_id IS NULL
        AND parent.name = 'handling'
        AND child.name = 'hovedperson(er) - beskrivelse'), 'hovedpersonens konflikt', true, true, 3);

INSERT INTO taxonomy_category (parent_id, name, is_leaf, active, display_order)
VALUES
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'fortælleteknik'), 'skrivestil og struktur', true, true, 1),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'fortælleteknik'), 'fortællerstemme', true, true, 2),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'fortælleteknik'), 'tempo', true, true, 3);

INSERT INTO taxonomy_category (parent_id, name, is_leaf, active, display_order)
VALUES
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'stemning'), 'positiv', true, true, 1),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'stemning'), 'humoristisk', true, true, 2),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'stemning'), 'romantisk', true, true, 3),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'stemning'), 'erotisk', true, true, 4),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'stemning'), 'dramatisk', true, true, 5),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'stemning'), 'trist', true, true, 6),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'stemning'), 'uhyggelig', true, true, 7),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'stemning'), 'fantasifuld', true, true, 8),
    ((SELECT id FROM taxonomy_category WHERE parent_id IS NULL AND name = 'stemning'), 'tankevækkende', true, true, 9);
