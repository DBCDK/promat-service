-- Move to subgroup 'Roman'
UPDATE subject SET path = 'Børn/skole/Roman/Handicap', parentid = 306 WHERE id = 5000;
UPDATE subject SET path = 'Børn/skole/Roman/LGBT+', parentid = 306 WHERE id = 5001;
UPDATE subject SET path = 'Børn/skole/Roman/Etnicitet, racegørelse', parentid = 306 WHERE id = 5002;
UPDATE subject SET path = 'Voksen/Roman/Eksperimenterende', parentid = 66 WHERE id = 5003;
UPDATE subject SET path = 'Voksen/Roman/Handicap', parentid = 66 WHERE id = 5004;
UPDATE subject SET path = 'Voksen/Roman/LGBT+', parentid = 66 WHERE id = 5005;
UPDATE subject SET path = 'Voksen/Roman/Etnicitet, racegørelse', parentid = 66 WHERE id = 5006;

-- Delete id 111 = "Voksen/Roman/Homoseksualitet"
DELETE FROM reviewersubjectnotes WHERE subjectnote_id IN (SELECT id FROM subjectnote WHERE subject_id = 111);
DELETE FROM reviewersubjects WHERE subject_id = 111;
DELETE FROM subjectnote where subject_id = 111;
DELETE FROM casesubjects where subject_id = 111;
DELETE FROM subject WHERE id = 111;
