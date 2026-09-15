-- This script provides a minimal set of data for the promat staging database or local
-- developers databases. There is never any guarantee that no other data exists, and
-- one must expect that the data is modified at will when in the staging database.
-- THIS IS NOT DATA FOR STRUCTURED TESTING!
--
-- All titles/authors/publishers/reviewer & editor names below are invented for demo purposes -
-- none of this is real bibliographic or personal data.

-- Cleanup
-- Just delete those we want to create, DO NOT run a complete truncate on any table
delete from promattask
    where id in (400001, 400002, 400003, 400004, 400005, 400006, 400007, 400008, 400009, 400010, 400011, 400012,
                 400013, 400014, 400015, 400016, 400017, 400018, 400019, 400020, 400021, 400022, 400023);
delete from promattask
    where id in (401001, 401011, 401021, 401031, 401041, 401051, 401061, 401062, 401071, 401072, 401081, 401082, 401083, 401091,
                 401092, 401093, 401101, 401102, 401103, 401104, 401111, 401112, 401113, 401114, 401115, 401116, 401117, 401118, 401119,
                 401121, 401122, 401123, 401124, 401125, 401126, 401127, 401128, 401129, 401131, 401132, 401133, 401134, 401135, 401136,
                 401141, 401142, 401143, 401144, 401145, 401146, 401147, 401151, 401152, 401153, 401154, 401155, 401156, 401157,
                 401161, 401162, 401163, 401164, 401165, 401166, 401167, 401171, 401172, 401173, 401174, 401175, 401176, 401177,
                 401178, 401181, 401182, 401183, 400024, 400025, 400026, 400027, 400028, 400029, 400030, 400031, 400032, 400033,
                 400034, 400035, 400036, 400037, 400038, 400039, 400040, 400041, 400042, 400043, 400044, 400045, 400046, 400047,
                 400048, 400049, 400050, 400051, 400052, 402001, 402002);

--
delete from promatcase
    where id in (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22);
delete from promatcase
    where id in (1000, 1010, 1020, 1030, 1040, 1050, 1060, 1070, 1080, 1090, 1100, 1110, 1120, 1130, 1140, 1150, 1160, 1170, 1180);
--
delete from notification
    where id in (1, 2, 3);
--
delete from reviewersubjectnotes
where reviewer_id in (4900, 4901, 4902);
---
delete from subjectnote
    where id in (1, 2, 3);
--
delete from subject
where id in (4901, 4902, 4903, 4904, 4905);
--
delete from promatuser
where id in (4900, 4901, 4902, 4950, 4951, 4952, 4953, 4954, 4955,
             4956, 4957, 4958, 4959, 4960, 4961, 4962, 4963);
--

-- Reviewers
-- Last migrated reviewer has id 3441, sequence restarts at 5000 = put reviewers in range 4900-4949
insert into promatuser(id, role, active, culrid, firstname, lastname, email, address1, zip, city, institution, paycode, hiatus_begin, hiatus_end, accepts, note, capacity, agency, userid)
values (4900, 'REVIEWER', true, '41', 'Hans', 'Hansen', 'hans.hansen@eksempel.dk', 'Lillegade 1', '9999', 'Lilleved', 'Frederiksberg Bibliotek', 123, '2020-10-28', '2020-11-01', '["MULTIMEDIA", "PS4", "PS5"]', 'Anmelder primært skønlitteratur for voksne', 1, NULL, NULL),
       (4901, 'REVIEWER', true, '42', 'Ole', 'Olsen', 'ole.olsen@eksempel.dk', 'Storegade 99', '1111', 'Storeved', 'Aarhus Hovedbibliotek', 456, '2020-11-28', '2020-12-01', '["MULTIMEDIA", "PS4", "PS5"]', 'Anmelder primært film og multimedie', 2, NULL, NULL),
       -- Netpunkt/CULR login user for feature-branch preview testing.
       (4902, 'REVIEWER', true, '43', 'Peter', 'Andersen', 'proanm1@dbc.dk', 'Testvej 12', '2100', 'København Ø', 'Testbiblioteket', 789, NULL, NULL, '["MULTIMEDIA", "PS4", "PS5"]', 'Testbruger til feature branch preview', 1, '790900', 'proanm1');

-- Editors
-- Migrated editors has id 10001-10006, sequence restarts at 5000 = put editors in range 4950-4999
insert into promatuser(id, role, active, culrid, firstname, lastname, email, paycode, agency, userid)
values (4950, 'EDITOR', true, '51', 'Anne', 'Kristensen', 'anne.kristensen@dbc.dk', 5678, NULL, NULL),
       (4951, 'EDITOR', true, '52', 'Thomas', 'Berg', 'thomas.berg@dbc.dk', 1111, NULL, NULL),
       (4952, 'EDITOR', true, '53', 'Camilla', 'Dahl', 'camilla.dahl@dbc.dk', 2760, NULL, NULL),
       (4953, 'EDITOR', true, '54', 'Soeren', 'Vig', 'soeren.vig@dbc.dk', 2222, NULL, NULL),
       (4954, 'EDITOR', true, '56', 'Louise', 'Holm', 'louise.holm@dbc.dk', 2860, NULL, NULL),
       -- Netpunkt/CULR login user for feature-branch preview testing.
       (4955, 'EDITOR', true, '55', 'Pernille', 'Rode', 'prored1@dbc.dk', 3333, '790900', 'prored1'),
       -- Netpunkt/CULR login users for feature-branch preview testing, one per real editor's initials.
       (4956, 'EDITOR', true, '57', 'Ludvig', 'Borup', 'ludvig.borup@dbc.dk', 4001, '790900', 'lubo'),
       (4957, 'EDITOR', true, '58', 'Kim', 'Petersen', 'kim.petersen@dbc.dk', 4002, '790900', 'kipe'),
       (4958, 'EDITOR', true, '59', 'Aksel', 'Riis', 'aksel.riis@dbc.dk', 4003, '790900', 'akri'),
       (4959, 'EDITOR', true, '60', 'Kirsten', 'Schmidt', 'kirsten.schmidt@dbc.dk', 4004, '790900', 'kisc'),
       (4960, 'EDITOR', true, '61', 'Svend', 'Ibsen', 'svend.ibsen@dbc.dk', 4005, '790900', 'svib'),
       (4961, 'EDITOR', true, '62', 'Jens Gunnar', 'Nielsen', 'jens.gunnar.nielsen@dbc.dk', 4006, '790900', 'jgn'),
       (4962, 'EDITOR', true, '63', 'Jonas Bo', 'Ravn', 'jonas.bo.ravn@dbc.dk', 4007, '790900', 'jbr'),
       (4963, 'EDITOR', true, '64', 'Peter Mogens', 'Lund', 'peter.mogens.lund@dbc.dk', 4008, '790900', 'pml');

-- Notifications
INSERT INTO notification(id, bodytext, subject, toaddress, status, created)
values (1, '<p>Kaere anmelder,</p><p>Din anmeldelse naermer sig deadline. Husk at aflevere i god tid.</p>', 'Paamindelse: deadline naermer sig', 'hans.hansen@eksempel.dk', 0, now()),
       (2, '<p>Kaere anmelder,</p><p>Sagen er nu godkendt og klar til eksport.</p>', 'Sag godkendt', 'ole.olsen@eksempel.dk', 1, now()),
       (3, '<p>Kaere anmelder,</p><p>Deadline er overskredet - kontakt redaktionen hvis der er problemer.</p>', 'Deadline overskredet', 'hans.hansen@eksempel.dk', 0, now());

-- Subjects
-- Last migrated subject has id 2691, sequence restarts at 5000 = put subjects in the range 4901-4999
insert into subject(id, name, parentid)
VALUES (4901, 'Voksen', null),
       (4902, 'Roman', 4901),
       (4903, 'Eventyr, fantasy', 4902),
       (4904, 'Digte', 4901),
       (4905, 'Multimedie', null);

insert into reviewersubjects(subject_id, reviewer_id)
values(4905,4900),
      (4903,4900),
      (4905,4901),
      (4904,4901),
      (4904,4900);

-- Subjectnotes
insert into subjectnote(id, subject_id, note)
values(1, 4905, 'Hans Hansen anmelder gerne multimedie for voksne'),
      (2, 4903, 'Hans Hansen har erfaring med fantasy-genren'),
      (3, 4904, 'Ole Olsen anmelder gerne digtsamlinger');

insert into reviewersubjectnotes(subjectnote_id, reviewer_id)
values(1, 4900),
      (2, 4900),
      (3, 4901) ;

-- Cases and tasks.
-- First migrated case has at 130214, sequence starts at 500000 = put cases at id 1-10.000
-- Last migrated task has id 16100, sequence starts at 500000 = put tasks at id 400000-499999
--
-- Titles/authors/details below are drawn from a small invented "sample library" reused across
-- several cases below, purely so the data reads as plausible material instead of placeholder
-- text like "Title for 001111" - none of it refers to real books, films or people.
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (1, 'Vinterlys', 'Roman om en kvinde der vender hjem til sin fødeby efter tyve år i udlandet.', '001111', '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', 'BKM202101', '202101', 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400001,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2020-12-10', 'GROUP_1_LESS_THAN_100_PAGES', '2020-12-10',  NULL,         NULL,                     '["001111"]'),
       (400002,  'GROUP_2_100_UPTO_199_PAGES',  'DESCRIPTION', '2020-12-10', 'GROUP_2_100_UPTO_199_PAGES',  NULL,         '2020-12-10', NULL,                     '["001111"]'),
       (400003,  'GROUP_3_200_UPTO_499_PAGES',  'DESCRIPTION', '2020-12-10', 'GROUP_3_200_UPTO_499_PAGES',  NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["001111"]'),
       (400004,  'GROUP_4_500_OR_MORE_PAGES',   'DESCRIPTION', '2020-12-10', 'GROUP_4_500_OR_MORE_PAGES',   NULL,         NULL,         NULL,                     '["001111"]'),
       (400005,  'MOVIES_GR_1',                 'DESCRIPTION', '2020-12-10', 'MOVIES_GR_1',                 NULL,         NULL,         NULL,                     '["001111"]');

insert into casetasks(case_id, task_id)
values (1, 400001),
       (1, 400002),
       (1, 400003),
       (1, 400004),
       (1, 400005);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (2, 'Nattetog', 'Instruktøren følger en togkonduktør gennem en enkelt skæbnesvanger nattevagt.', '004444', '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'MOVIE', 'DBF202049', '202049', 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (3, 'Den Sidste Sommer', 'Roman om tre venners sidste sommerferie inden voksenlivet begynder.', '011111', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (4, 'Skyggeland', 'Fantasyroman om en ung kvinde der opdager en skjult verden bag sit spejl.', '012222', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'EXPORTED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (5, 'Havets Stemme', 'Digtsamling om havet, længsel og hjemstavn.', '013333', '[]', 4900, 4950, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(6, 'Under Broen', 'Roman om hjemløshed og fællesskab i en større dansk by.', '014444', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'ASSIGNED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(7, 'Regnvejrsbyen', 'Krimi der udspiller sig i en lille dansk provinsby en regnfuld efterårsuge.', '015555', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(8, 'Det Glemte Kort', 'Børnebog om to søskende der finder et gammelt landkort på loftet.', '016666', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(9, 'De Stille Timer', 'Dokumentarfilm om natarbejdere i en storby.', '017777', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'CLOSED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(10, 'Vejen Hjem', 'Roman om en soldats hjemkomst efter en udsendelse.', '018888', '[]', NULL, 4951, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(11, 'Vinterlys', 'Genudgivelse i paperback af den prisbelønnede roman.', '019999', '[]', 4901, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(12, 'Den Sidste Sommer', 'Lydbogsudgave af romanen, indlæst af forfatteren selv.', '019991', '[]', 4900, 4951, '2020-11-11', '2020-12-14', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(13, 'Skyggeland', 'Andet bind i fantasyserien.', '019992', '[]', 4900, 4951, '2021-01-11', '2021-01-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(14, 'Havets Stemme', 'Illustreret jubilæumsudgave af digtsamlingen.', '019993', '[]', 4900, 4950, '2021-01-13', '2021-01-13', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
-- Case 15 has one target faust per task (typical single-edition case).
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(15, 'Under Broen', 'Anden trykning efter udsolgt førsteoplag.', '019994', '[]', 4900, 4950, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400006,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["019994"]'),
       (400007,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Bogen er en samfundsrealistisk roman for voksne.', '["019994"]'),
       (400008,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Egnet til biblioteker med fokus på samfundsdebat.', '["019994"]'),
       (400009,  'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',  '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         NULL,    '["019994"]');

insert into casetasks(case_id, task_id)
values (15, 400006),
       (15, 400007),
       (15, 400008),
       (15, 400009);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(16, 'Regnvejrsbyen', 'Filmatiseringen af krimiromanen med samme titel.', '019995', '[]', 4900, 4950, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400010, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["019995"]'),
       (400011, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Bogen er en krimi rettet mod et voksent publikum.', '["019995"]'),
       (400012, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',  '2021-01-27', 'METAKOMPAS',                  '2021-01-27', NULL,         NULL,    '["019995"]');

insert into casetasks(case_id, task_id)
values (16, 400010),
       (16, 400011),
       (16, 400012);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(17, 'Det Glemte Kort', 'Nyoversat udgave med nye illustrationer.', '019996', '[]', 4900, 4950, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400013, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["019996"]'),
       (400014, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Bogen er en børnebog for aldersgruppen 8-12 år.', '["019996"]');

insert into casetasks(case_id, task_id)
values (17, 400013),
       (17, 400014);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(18, 'Vejen Hjem', 'Reprise-udgivelse i anledning af tiårsjubilæet.', '019997', '[]', 4900, 4950, '2021-01-27', '2021-02-27', '2021-01-27', 'REJECTED', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400015, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["019997"]'),
       (400016, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Anmelder afviste opgaven grundet inhabilitet.', '["019997"]');

insert into casetasks(case_id, task_id)
values (18, 400015),
       (18, 400016);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(19, 'Vinterlys', 'Storskrift-udgave til svagtseende.', '019998', '[]', 4900, 4950, '2020-12-27', '2021-01-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400017, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["019998"]'),
       (400018, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Bogen er en samfundsrealistisk roman for voksne.', '["019998"]');

insert into casetasks(case_id, task_id)
values (19, 400017),
       (19, 400018);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(20, 'Den Sidste Sommer', 'Bogklub-udgave med læsevejledning.', '019999', '[]', 4900, 4950, '2020-12-27', '2021-01-27', '2021-01-27', 'PENDING_ISSUES', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400019, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["019999"]'),
       (400020, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Redaktionen har bedt om uddybning af et afsnit.', '["019999"]');

insert into casetasks(case_id, task_id)
values (20, 400019),
       (20, 400020);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(21, 'Skyggeland', 'Tredje og afsluttende bind i fantasyserien.', '029999', '[]', 4900, 4950, '2020-12-27', '2021-01-27', '2021-01-27', 'PENDING_EXTERNAL', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400021, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["029999"]'),
       (400022, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Bogen er sidste bind i en populær fantasyserie.', '["029999"]'),
       (400023, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',  '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         NULL,    '["029999"]');

insert into casetasks(case_id, task_id)
values (21, 400021),
       (21, 400022),
       (21, 400023);
--
-- Case 22 is the fullest example: BOOK case with three related editions (fausts), tasks
-- covering (almost) every TaskFieldType, and one deliberately unapproved task
-- (COMPARISON, id 400044) to demonstrate an in-progress case.
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id, author, publisher)
values (22, 'Havets Stemme', 'Digtsamling om havet, længsel og hjemstavn, udgivet i tre parallelle formater.', '100000', '["100001", "100002"]', 4900, 4950, '2021-01-09', '2021-02-09', '2021-01-10', 'EXPORTED', 'BOOK', 'BKM202104', '202104', 4953, 'Karin Møller', 'Gyldendal');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (400024,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2020-12-10', 'GROUP_1_LESS_THAN_100_PAGES', '2020-12-10',  NULL,         NULL,                        '["100000", "100001", "100002"]'),
        (400025,  'GROUP_2_100_UPTO_199_PAGES',  'DESCRIPTION',    '2020-12-10', 'GROUP_2_100_UPTO_199_PAGES',  NULL,         '2020-12-10', NULL,                        '["100000", "100001", "100002"]'),
        (400026,  'GROUP_3_200_UPTO_499_PAGES',  'DESCRIPTION',    '2020-12-10', 'GROUP_3_200_UPTO_499_PAGES',  NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["100000", "100001", "100002"]'),
        (400027,  'GROUP_4_500_OR_MORE_PAGES',   'DESCRIPTION',    '2020-12-10', 'GROUP_4_500_OR_MORE_PAGES',   NULL,         NULL,         NULL,                        '["100000", "100001", "100002"]'),
        (400028,  'MOVIES_GR_1',                 'DESCRIPTION',    '2020-12-10', 'MOVIES_GR_1',                 NULL,         NULL,         NULL,                        '["100000", "100001", "100002"]'),
        (400029,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["100000", "100001", "100002"]'),
        (400030,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Digtsamlingen henvender sig til et voksent publikum.', '["100001"]'),
        (400031,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Lydbogsudgaven er indlæst af forfatteren selv.',       '["100002"]'),
        (400032,  'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         NULL,       '["100000"]'),
        (400033, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["100000", "100001", "100002"]'),
        (400034, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Storskrift-udgaven er velegnet til svagtseende.',      '["100002"]'),
        (400035, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-27', 'METAKOMPAS',                  '2021-01-27', NULL,         NULL,       '["100000"]'),
        (400036, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Kort beskrivelse af handling og persongalleri.', '["100000", "100001", "100002"]'),
        (400037, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'Førsteudgaven er indbundet med stofomslag.',           '["100000"]'),
        (400038, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', 'Digtsamling om havet, længsel og hjemstavn - førsteudgave.', '["100000"]'),
        (400039, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', 'Samme digtsamling som lydbog, indlæst af forfatteren.',      '["100001"]'),
        (400040, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', 'Samme digtsamling i storskrift-udgave.',                     '["100001"]'),
        (400041, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',            '2021-01-27', 'BKM',                         '2021-02-08', '2021-02-09', 'Vurderes relevant for almindelige folkebiblioteker.', '["100000", "100001", "100002"]'),
        (400042, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'Digtene kredser om hav, længsel og hjemstavn i et enkelt sprog.', '["100000", "100001", "100002"]'),
        (400043, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'Velskrevet og stemningsfuld samling med god sproglig rytme.', '["100000", "100001", "100002"]'),
        (400044, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'Sammenlignelig med forfatterens tidligere samling "Regnvejrsbyen" i tematik.', '["100000", "100001", "100002"]'),
        (400045, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'Anbefales til folkebiblioteker med fokus på dansk lyrik.', '["100000", "100001", "100002"]'),
        (400046, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'hav, længsel, hjemstavn',    '["100000"]'),
        (400047, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'hav, natur, eftertænksomhed', '["100001"]'),
        (400048, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'længsel, hjemstavn, identitet', '["100002"]'),
        (400049, 'GROUP_1_LESS_THAN_100_PAGES', 'AGE',            '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'Voksne',                     '["100000", "100001", "100002"]'),
        (400050, 'GROUP_1_LESS_THAN_100_PAGES', 'MATLEVEL',       '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'Let tilgængeligt',           '["100000", "100001", "100002"]'),
        (400051,  'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         NULL,       '["100001"]'),
        (400052,  'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         NULL,       '["100002"]');

insert into casetasks(case_id, task_id)
values (22, 400024),
       (22, 400025),
       (22, 400026),
       (22, 400027),
       (22, 400028),
       (22, 400029),
       (22, 400030),
       (22, 400031),
       (22, 400032),
       (22, 400033),
       (22, 400034),
       (22, 400035),
       (22, 400036),
       (22, 400037),
       (22, 400038),
       (22, 400039),
       (22, 400040),
       (22, 400041),
       (22, 400042),
       (22, 400043),
       (22, 400044),
       (22, 400045),
       (22, 400046),
       (22, 400047),
       (22, 400048),
       (22, 400049),
       (22, 400050),
       (22, 400051),
       (22, 400052);

-- Sample Metakompas selections for case 22's METAKOMPAS tasks (400035, 400051, 400052) - one
-- selection per task, shared across all of that task's target fausts, stored directly in
-- promattask.data like every other task type's content.
update promattask set data = '[{"path":["stemning","dramatisk"],"id":1,"title":"dramatisk","note":[],"oftenUsed":true,"ref":null}]' where id = 400035;
update promattask set data = '[{"path":["ramme","geografisk sted"],"id":2,"title":"kystby","note":[],"oftenUsed":false,"ref":null}]' where id = 400051;
update promattask set data = '[{"path":["fortælleteknik","tempo"],"id":3,"title":"langsomt tempo","note":[],"oftenUsed":false,"ref":null}]' where id = 400052;

--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1000, 'Vinterlys', 'Roman om en kvinde der vender hjem til sin fødeby efter tyve år i udlandet.', '1001000', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401001, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'Bogen er en samfundsrealistisk roman for voksne.', '["1001000"]');

insert into casetasks(case_id, task_id)
values (1000, 401001);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1010, 'Den Sidste Sommer', 'Roman om tre venners sidste sommerferie inden voksenlivet begynder.', '1001010', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_MEETING', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401011, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'Anmeldelsen skal drøftes på næste redaktionsmøde.', '["1001010"]');

insert into casetasks(case_id, task_id)
values (1010, 401011);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1020, 'Skyggeland', 'Fantasyroman om en ung kvinde der opdager en skjult verden bag sit spejl.', '1001020', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_EXPORT', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401021, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'Bogen er første bind i en fantasyserie.', '["1001020"]');

insert into casetasks(case_id, task_id)
values (1020, 401021);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1030, 'Havets Stemme', 'Digtsamling om havet, længsel og hjemstavn.', '1001030', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'EXPORTED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401031, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'Digtsamlingen henvender sig til et voksent publikum.', '["1001030"]');

insert into casetasks(case_id, task_id)
values (1030, 401031);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1040, 'Under Broen', 'Roman om hjemløshed og fællesskab i en større dansk by.', '1001040', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_REVERT', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401041, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'Sagen er sendt tilbage til anmelder for en rettelse.', '["1001040"]');

insert into casetasks(case_id, task_id)
values (1040, 401041);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1050, 'Regnvejrsbyen', 'Krimi der udspiller sig i en lille dansk provinsby en regnfuld efterårsuge.', '1001050', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'REVERTED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401051, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'Rettelsen er modtaget og godkendt.', '["1001050"]');

insert into casetasks(case_id, task_id)
values (1050, 401051);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1060, 'Det Glemte Kort', 'Børnebog om to søskende der finder et gammelt landkort på loftet.', '1001060', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_CLOSE', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401061, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', NULL,        NULL, 'Bogen er en børnebog for aldersgruppen 8-12 år.', '["1001060"]'),
        (401062, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',   '2021-01-13', 'BKM', '2021-01-13', NULL, 'Vurderes relevant for almindelige folkebiblioteker.', '["1001060"]');

insert into casetasks(case_id, task_id)
values (1060, 401061),
       (1060, 401062);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1070, 'Vejen Hjem', 'Roman om en soldats hjemkomst efter en udsendelse.', '1001070', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401071, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13', 'BRIEF', '2021-01-13', NULL, 'Bogen er en samfundsrealistisk roman for voksne.', '["1001070"]'),
        (401072, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',   '2021-01-13', 'BKM', '2021-01-13',   NULL, 'Vurderes relevant for almindelige folkebiblioteker.', '["1001070"]');

insert into casetasks(case_id, task_id)
values (1070, 401071),
       (1070, 401072);
--
-- Case 1080 keeps three distinct target fausts across its BRIEF tasks, each covering a
-- different edition of the same underlying work (populated in relatedFausts, unlike the
-- pre-existing legacy rows above).
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1080, 'Vinterlys', 'Roman udgivet samtidig som paperback, e-bog og lydbog.', '1001080', '["1001081", "1001082"]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401081, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'Paperback-udgave af romanen.', '["1001080"]'),
        (401082, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'E-bogsudgave, samme tekst som paperback.', '["1001081"]'),
        (401083, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'Lydbogsudgave, indlæst af forfatteren selv.', '["1001082"]');

insert into casetasks(case_id, task_id)
values (1080, 401081),
       (1080, 401082),
       (1080, 401083);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1090, 'Den Sidste Sommer', 'Roman om tre venners sidste sommerferie inden voksenlivet begynder.', '05510228', '["52880645"]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401091, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'Fælles anmeldelse for førsteudgave og genoptryk.', '["05510228", "52880645"]');

insert into casetasks(case_id, task_id)
values (1090, 401091);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1100, 'Skyggeland', 'Fantasyroman om en ung kvinde der opdager en skjult verden bag sit spejl.', '1001100', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401101, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'Bogen er første bind i en fantasyserie.', '["1001100"]');

insert into casetasks(case_id, task_id)
values (1100, 401101);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1110, 'Havets Stemme', 'Digtsamling udgivet i to formater: indbundet og som lydbog.', '1001110', '["1001111"]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401111, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'Indbundet førsteudgave.', '["1001110"]'),
        (401112, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'Lydbogsudgave, indlæst af forfatteren selv.', '["1001111"]'),
        (401113, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'Kort beskrivelse af digtsamlingens temaer.', '["1001110", "1001111"]'),
        (401114, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'Velskrevet og stemningsfuld samling med god sproglig rytme.', '["1001110", "1001111"]'),
        (401115, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'Sammenlignelig med forfatterens tidligere udgivelser.', '["1001110", "1001111"]'),
        (401116, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'Anbefales til folkebiblioteker med fokus på dansk lyrik.', '["1001110", "1001111"]'),
        (401118, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, NULL, '["1001110"]'),
        (401119, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, NULL, '["1001111"]');

insert into casetasks(case_id, task_id)
values (1110, 401111),
       (1110, 401112),
       (1110, 401113),
       (1110, 401114),
       (1110, 401115),
       (1110, 401116),
       (1110, 401118),
       (1110, 401119);

-- Two separate METAKOMPAS tasks on the same case (401118/401119), each targeting one faust -
-- a case whose Metakompas selection genuinely differs per faust is split into separate tasks
-- rather than one task carrying more than one selection.
update promattask set data = '[{"path":["handling","navngivet hovedperson"],"id":4,"title":"navngivet hovedperson","note":[],"oftenUsed":false,"ref":null}]' where id = 401118;
update promattask set data = '[{"path":["stemning","tankevækkende"],"id":5,"title":"tankevækkende","note":[],"oftenUsed":true,"ref":null}]' where id = 401119;

--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1120, 'Under Broen', 'Roman udgivet i to formater: indbundet og e-bog.', '1001120', '["1001121"]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_APPROVAL', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401121, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13','BRIEF',                        '2021-01-13', NULL, 'Indbundet førsteudgave.', '["1001120"]'),
        (401122, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13','BRIEF',                        '2021-01-13', NULL, 'E-bogsudgave, samme tekst som indbundet.', '["1001121"]'),
        (401123, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'Kort beskrivelse af handling og persongalleri.', '["1001120", "1001121"]'),
        (401124, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'Velskrevet roman med et vedkommende samfundstema.', '["1001120", "1001121"]'),
        (401125, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL, NULL, '["1001120", "1001121"]'), -- Endnu ikke udfyldt af anmelder
        (401126, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'Anbefales til biblioteker med fokus på samfundsdebat.', '["1001120", "1001121"]'),
        (401128, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, NULL, '["1001120"]'),
        (401129, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, NULL, '["1001121"]'),
        -- Two separate BUGGI tasks, one per faust, same pattern as the METAKOMPAS tasks above -
        -- this case previously had no BUGGI task at all.
        (402001, 'GROUP_1_LESS_THAN_100_PAGES', 'BUGGI',          '2021-01-13', 'BUGGI',                       '2021-01-13', NULL, NULL, '["1001120"]'),
        (402002, 'GROUP_1_LESS_THAN_100_PAGES', 'BUGGI',          '2021-01-13', 'BUGGI',                       '2021-01-13', NULL, NULL, '["1001121"]');

insert into casetasks(case_id, task_id)
values (1120, 401121),
       (1120, 401122),
       (1120, 401123),
       (1120, 401124),
       (1120, 401125),
       (1120, 401126),
       (1120, 401128),
       (1120, 401129),
       (1120, 402001),
       (1120, 402002);

update promattask set data = '{"tags":[{"name":"hovedperson","value":3},{"name":"handling","value":2}]}' where id = 402001;
update promattask set data = '{"tags":[{"name":"hovedperson","value":1}]}' where id = 402002;
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1130, 'Nattetog', 'Instruktøren følger en togkonduktør gennem en enkelt skæbnesvanger nattevagt.', '1001130', '[]', 4901, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_EXPORT', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401131, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13','BRIEF',        '2021-01-13', NULL, 'Filmen er et kammerspil med tre skuespillere.', '["1001130"]'),
        (401132, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'Kort beskrivelse af filmens handling og univers.', '["1001130"]'),
        (401133, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'Stram instruktion og stærke skuespillerpræstationer.', '["1001130"]'),
        (401134, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'Minder tematisk om instruktørens tidligere film.', '["1001130"]'),
        (401135, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'Anbefales til biblioteker med et bredt filmudvalg.', '["1001130"]');

insert into casetasks(case_id, task_id)
values (1130, 401131),
       (1130, 401132),
       (1130, 401133),
       (1130, 401134),
       (1130, 401135);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1140, 'De Stille Timer', 'Dokumentarfilm om natarbejdere i en storby.', '1001140', '[]', 4901, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'EXPORTED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401141, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2020-12-13', '2020-12-21 12:13:14.567', 'Dokumentarfilm optaget over et helt år.', '["1001140"]'),
        (401142, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'Kort beskrivelse af filmens handling og univers.', '["1001140"]'),
        (401143, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'Nærværende og velfortalt dokumentar.', '["1001140"]'),
        (401144, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'Minder om andre dokumentarfilm i samme genre.', '["1001140"]'),
        (401145, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'Anbefales til biblioteker med fokus på dokumentarfilm.', '["1001140"]'),
        (401147, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL,                      'Opfølgende bemærkning fra redaktionen.', '["1001140"]');

insert into casetasks(case_id, task_id)
values (1140, 401141),
       (1140, 401142),
       (1140, 401143),
       (1140, 401144),
       (1140, 401145),
       (1140, 401147);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1150, 'Vejen Hjem', 'Roman udgivet både som spillefilm og biograftrailer.', '1001150', '["1001151"]', 4901, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401151, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       NULL,         NULL, 'Spillefilm om en soldats hjemkomst.', '["1001150"]'),
        (401152, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'Kort beskrivelse af filmens handling og univers.', '["1001150", "1001151"]'),
        (401153, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'Gribende skildring af en hjemkomst efter krig.', '["1001150", "1001151"]'),
        (401154, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'Minder om andre danske krigsdramaer.', '["1001150", "1001151"]'),
        (401155, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'Anbefales til biblioteker med et bredt filmudvalg.', '["1001150", "1001151"]'),
        (401157, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL, 'Biograftrailer til samme film.', '["1001151"]');

insert into casetasks(case_id, task_id)
values (1150, 401151),
       (1150, 401152),
       (1150, 401153),
       (1150, 401154),
       (1150, 401155),
       (1150, 401157);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1160, 'Nattetog', 'Filmen genudgivet med et bonusinterview med instruktøren.', '1001160', '["1001161"]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401161, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       NULL,         NULL, 'Filmen er et kammerspil med tre skuespillere.', '["1001160"]'),
        (401162, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'Kort beskrivelse af filmens handling og univers.', '["1001160", "1001161"]'),
        (401163, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'Stram instruktion og stærke skuespillerpræstationer.', '["1001160", "1001161"]'),
        (401164, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'Minder tematisk om instruktørens tidligere film.', '["1001160", "1001161"]'),
        (401165, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'Anbefales til biblioteker med et bredt filmudvalg.', '["1001160", "1001161"]'),
        (401167, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL, 'Bonusinterview med instruktøren.', '["1001161"]');

insert into casetasks(case_id, task_id)
values (1160, 401161),
       (1160, 401162),
       (1160, 401163),
       (1160, 401164),
       (1160, 401165),
       (1160, 401167);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1170, 'Byggeklodser: Kodning for Børn', 'Læringsspil der introducerer børn til grundlæggende programmeringsbegreber.', '1001170', '["1001171"]', 4901, 4950, '2021-01-24', '2021-01-26', '2021-01-24', 'APPROVED', 'MULTIMEDIA');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401171, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',          '2021-01-26', NULL, 'Læringsspil for børn i alderen 6-10 år.', '["1001170", "1001171"]'),
        (401172, 'MULTIMEDIA_FEE', 'DESCRIPTION',    '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'Kort beskrivelse af spillets opbygning og formål.', '["1001170", "1001171"]'),
        (401173, 'MULTIMEDIA_FEE', 'EVALUATION',     '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'Pædagogisk velfungerende med tydelig progression.', '["1001170", "1001171"]'),
        (401174, 'MULTIMEDIA_FEE', 'COMPARISON',     '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'Minder om andre læringsspil i samme kategori.', '["1001170", "1001171"]'),
        (401175, 'MULTIMEDIA_FEE', 'RECOMMENDATION', '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'Anbefales til biblioteker med fokus på undervisningsmateriale.', '["1001170", "1001171"]'),
        (401177, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',          '2021-01-26', NULL, 'Opdateret version med nye baner.', '["1001172"]'),
        (401178, 'MULTIMEDIA_FEE', 'EXPRESS',        '2021-01-13', 'EXPRESS',        '2021-01-26', NULL, 'Hasteopgave grundet snarlig udgivelsesdato.', '["1001170", "1001171"]');

insert into casetasks(case_id, task_id)
values (1170, 401171),
       (1170, 401172),
       (1170, 401173),
       (1170, 401174),
       (1170, 401175),
       (1170, 401177),
       (1170, 401178);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1180, 'Rumrejsen', 'Computerspil om en gruppe rumfarere der udforsker et fremmed solsystem.', '1001180', '["1001181"]', 4901, 4950, '2021-01-24', '2021-01-26', '2021-01-24', 'APPROVED', 'MULTIMEDIA');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401181, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-26', NULL, 'Computerspil for aldersgruppen 10-14 år.', '["1001180", "1001181"]'),
        (401182, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-26', NULL, 'Udvidelsespakke til samme spil.', '["1001182"]'),
        (401183, 'MULTIMEDIA_FEE', 'EXPRESS',        '2021-01-13', 'EXPRESS',     '2021-01-26', NULL, 'Hasteopgave grundet snarlig udgivelsesdato.', '["1001180", "1001181"]');

insert into casetasks(case_id, task_id)
values (1180, 401181),
       (1180, 401182),
       (1180, 401183);
