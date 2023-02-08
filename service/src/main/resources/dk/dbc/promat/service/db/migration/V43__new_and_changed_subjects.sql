INSERT INTO subject(name, path, parentid) VALUES('Handicap', 'Børn/skole/Handicap', 58) ON CONFLICT DO NOTHING;
INSERT INTO subject(name, path, parentid) VALUES('LGBT+', 'Børn/skole/LGBT+', 58) ON CONFLICT DO NOTHING;
INSERT INTO subject(name, path, parentid) VALUES('Etnicitet, racegørelse', 'Børn/skole/Etnicitet, racegørelse', 58) ON CONFLICT DO NOTHING;
INSERT INTO subject(name, path, parentid) VALUES('Eksperimenterende', 'Voksen/Eksperimenterende', 57) ON CONFLICT DO NOTHING;
INSERT INTO subject(name, path, parentid) VALUES('Handicap', 'Voksen/Handicap', 57) ON CONFLICT DO NOTHING;
INSERT INTO subject(name, path, parentid) VALUES('LGBT+', 'Voksen/LGBT+', 57) ON CONFLICT DO NOTHING;
INSERT INTO subject(name, path, parentid) VALUES('Etnicitet, racegørelse', 'Voksen/Etnicitet, racegørelse', 57) ON CONFLICT DO NOTHING;

UPDATE subject SET name = 'Billedbog, pegebog', path = 'Børn/skole/Billedbog, pegebog' WHERE id = 296;
UPDATE subject SET name = 'Eventyr, sagn', path = 'Børn/skole/Roman/Eventyr, sagn' WHERE id = 312;
UPDATE subject SET name = 'Humor, sjove bøger', path = 'Børn/skole/Roman/Humor, sjove bøger' WHERE id = 314;
UPDATE subject SET name = 'Indvandrere, etnicitet', path = 'Børn/skole/Roman/Indvandrere, etnicitet' WHERE id = 317;
UPDATE subject SET name = 'Tegneserier, graphic novels', path = 'Børn/skole/Tegneserier, graphic novels' WHERE id = 304;
UPDATE subject SET name = 'Forelskelse, kærlighed', path = 'Børn/skole/Roman/Forelskelse, kærlighed' WHERE id = 326;
UPDATE subject SET name = 'Trivi, feel good', path = 'Voksen/Roman/Trivi, feel good' WHERE id = 162;
UPDATE subject SET name = 'Tegneserier, graphic novels', path = 'Voksen/Tegneserier, graphic novels' WHERE id = 65;

DELETE FROM reviewersubjectnotes WHERE subjectnote_id IN (SELECT id FROM subjectnote WHERE subject_id = 323);
DELETE FROM reviewersubjects WHERE subject_id = 323;
DELETE FROM subjectnote where subject_id = 323;
DELETE FROM casesubjects where subject_id = 323;
DELETE FROM subject WHERE id = 323;

DELETE FROM reviewersubjectnotes WHERE subjectnote_id IN (SELECT id FROM subjectnote WHERE subject_id = 101);
DELETE FROM reviewersubjects WHERE subject_id = 101;
DELETE FROM subjectnote where subject_id = 101;
DELETE FROM casesubjects where subject_id = 101;
DELETE FROM subject WHERE id = 101;
