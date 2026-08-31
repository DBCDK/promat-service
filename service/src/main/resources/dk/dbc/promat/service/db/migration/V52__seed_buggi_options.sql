-- Seeded with the values previously hardcoded in metakompasset's
-- DanMARC2Converter.buggiToDanMARC2() and the Buggi editor's initial state.
--
-- Same pattern as V50's taxonomy seed data: insert the "one" side first (groups), then the
-- "many" side (options) referencing each group's id via a subquery - simpler here since it's
-- only one level deep, so no self-join is ever needed the way V50's 3-level branch required.
INSERT INTO buggi_option_group (name, subfield_code, requires_nonzero_value, display_order)
VALUES
    ('Læsbarhed', 's', false, 1),
    ('Fantasi/virkelighed', 'u', false, 2),
    ('Stemning', 'n', true, 3),
    ('Tema', 'e', true, 4);

INSERT INTO buggi_option (group_id, name, display_order)
VALUES
    ((SELECT id FROM buggi_option_group WHERE name = 'Læsbarhed'), 'let/svær', 1),
    ((SELECT id FROM buggi_option_group WHERE name = 'Læsbarhed'), 'tekst/tegninger', 2),
    ((SELECT id FROM buggi_option_group WHERE name = 'Læsbarhed'), 'kort/lang', 3),

    ((SELECT id FROM buggi_option_group WHERE name = 'Fantasi/virkelighed'), 'virkelig/fantasi', 1),

    ((SELECT id FROM buggi_option_group WHERE name = 'Stemning'), 'rar', 1),
    ((SELECT id FROM buggi_option_group WHERE name = 'Stemning'), 'sjov', 2),
    ((SELECT id FROM buggi_option_group WHERE name = 'Stemning'), 'romantisk', 3),
    ((SELECT id FROM buggi_option_group WHERE name = 'Stemning'), 'spændende', 4),
    ((SELECT id FROM buggi_option_group WHERE name = 'Stemning'), 'trist', 5),
    ((SELECT id FROM buggi_option_group WHERE name = 'Stemning'), 'uhyggelig', 6),
    ((SELECT id FROM buggi_option_group WHERE name = 'Stemning'), 'tankevækkende', 7),

    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'dyr', 1),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'sport', 2),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'venskaber', 3),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'mit liv', 4),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'ud i fremtiden', 5),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'skæve karakterer', 6),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'den store verden', 7),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'fantasy', 8),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'gys', 9),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'eventyrlig', 10),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'action', 11),
    ((SELECT id FROM buggi_option_group WHERE name = 'Tema'), 'gaming', 12);
