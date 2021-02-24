-- This script provides a minimal set of data for the promat staging database or local
-- developers databases. There is never any guarantee that no other data exists, and
-- one must expect that the data is modified at will when in the staging database.
-- THIS IS NOT DATA FOR STRUCTURED TESTING!

-- Cleanup
-- Just delete those we want to create, DO NOT run a complete truncate on any table
delete from promattask
    where id in (400001, 400002, 400003, 400004, 400005, 400006, 400007, 400008, 400009, 400010, 400011, 400012,
                 400013, 400014, 400015, 400016, 400017, 400018, 400019, 400020, 400021, 400022, 400023);
delete from promattask
    where id in (401001, 401011, 401021, 401031, 401041, 401051, 401061, 401062, 401071, 401072, 401081, 401082, 401083, 401091,
                 401092, 401093, 401101, 401102, 401103, 401104, 401111, 401112, 401113, 401114, 401115, 401116, 401117, 401118,
                 401121, 401122, 401123, 401124, 401125, 401126, 401127, 401128, 401131, 401132, 401133, 401134, 401135, 401136,
                 401141, 401142, 401143, 401144, 401145, 401146, 401147, 401151, 401152, 401153, 401154, 401155, 401156, 401157,
                 401161, 401162, 401163, 401164, 401165, 401166, 401167, 401171, 401172, 401173, 401174, 401175, 401176, 401177,
                 401178, 401181, 401182, 401183);
--
delete from promatcase
    where id in (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);
delete from promatcase
    where id in (1000, 1010, 1020, 1030, 1040, 1050, 1060, 1070, 1080, 1090, 1100, 1110, 1120, 1130, 1140, 1150, 1160, 1170, 1180);
--
delete from promatuser
where id in (4900, 4901, 4950, 4951, 4952, 4953, 4954);
--
delete from notification
    where id in (1, 2, 3);
--
delete from subject
    where id in (4901, 4902, 4903, 4904, 4905);

-- Reviewers
-- Last migrated reviewer has id 3441, sequence restarts at 5000 = put reviewers in range 4900-4949
insert into promatuser(id, role, active, culrid, firstname, lastname, email, address1, zip, city, institution, paycode, hiatus_begin, hiatus_end, accepts, note, capacity)
values (4900, 'REVIEWER', true, '41', 'Hans', 'Hansen', 'hans@hansen.dk', 'Lillegade 1', '9999', 'Lilleved', 'Hans Hansens Bix', 123, '2020-10-28', '2020-11-01', '["MULTIMEDIA", "PS4", "PS5"]', 'note1', 1),
       (4901, 'REVIEWER', true, '42', 'Ole', 'Olsen', 'ole@olsen.dk', 'Storegade 99', '1111', 'Storeved', 'Ole Olsens Goodies', 456, '2020-11-28', '2020-12-01', '["MULTIMEDIA", "PS4", "PS5"]', 'note2', 2);

-- Editors
-- Migrated editors has id 10001-10006, sequence restarts at 5000 = put editors in range 4950-4999
insert into promatuser(id, role, active, culrid, firstname, lastname, email, paycode)
values (4950, 'EDITOR', true, '51', 'Ed', 'Itor', 'ed.itor@dbc.dk', 5678),
       (4951, 'EDITOR', true, '52', 'Edit', 'Or', 'edit.or@dbc.dk', 1111),
       (4952, 'EDITOR', true, '53', 'Edi', 'tor', 'edi.tor@dbc.dk', 2760),
       (4953, 'EDITOR', true, '54', 'Editte', 'Ore', 'editte.ore@dbc.dk', 2222),
       (4954, 'EDITOR', true, '56', 'E', 'ditor', 'e.ditor@dbc.dk', 2860);

-- Notifications
INSERT INTO notification(id, bodytext, subject, toaddress, status, created)
values (1, '<h1>hej1</h1>', 'test1', 'test1@test.dk', 0, now()),
       (2, '<h1>hej2</h1>', 'test2', 'test2@test.dk', 1, now()),
       (3, '<h1>hej3</h1>', 'test3', 'test3@test.dk', 0, now());

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

-- Cases and tasks.
-- First migrated case has at 130214, sequence starts at 500000 = put cases at id 1-10.000
-- Last migrated task has id 16100, sequence starts at 500000 = put tasks at id 400000-499999
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (1, 'Title for 001111', 'Details for 001111', '001111', '["002222","003333"]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', 'BKM202101', '202101', 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400001,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '202-12-10', 'GROUP_1_LESS_THAN_100_PAGES', '2020-12-10',  NULL,         NULL,                     NULL),
       (400002,  'GROUP_2_100_UPTO_199_PAGES',  'DESCRIPTION', '2020-12-10', 'GROUP_2_100_UPTO_199_PAGES',  NULL,         '2020-12-10', NULL,                     NULL),
       (400003,  'GROUP_3_200_UPTO_499_PAGES',  'DESCRIPTION', '2020-12-10', 'GROUP_3_200_UPTO_499_PAGES',  NULL,         NULL,         'here is some data',      NULL),
       (400004,  'GROUP_4_500_OR_MORE_PAGES',   'DESCRIPTION', '2020-12-10', 'GROUP_4_500_OR_MORE_PAGES',   NULL,         NULL,         '',                       NULL),
       (400005,  'MOVIES_GR_1',                 'DESCRIPTION', '2020-12-10', 'MOVIES_GR_1',                 NULL,         NULL,         NULL,                     NULL);

insert into casetasks(case_id, task_id)
values (1, 400001),
       (1, 400002),
       (1, 400003),
       (1, 400004),
       (1, 400005);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (2, 'Title for 004444', 'Details for 004444', '004444', '["005555","006666"]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'MOVIE', 'DBF202049', '202049', 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (3, 'Title for 011111', 'Details for 011111', '011111', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (4, 'Title for 012222', 'Details for 012222', '012222', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'EXPORTED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (5, 'Title for 013333', 'Details for 013333', '013333', '[]', 4900, 4950, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(6, 'Title for 014444', 'Details for 014444', '014444', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'ASSIGNED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(7, 'Title for 015555', 'Details for 015555', '015555', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(8, 'Title for 016666', 'Details for 016666', '016666', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(9, 'Title for 017777', 'Details for 017777', '017777', '[]', 4900, NULL, '2020-11-11', '2020-12-11', NULL, 'CLOSED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(10, 'Title for 018888', 'Details for 018888', '018888', '[]', NULL, 4951, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(11, 'Title for 019999', 'Details for 019999', '019999', '[]', 4901, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(12, 'Title for 019991', 'Details for 019991', '019991', '[]', 4900, 4951, '2020-11-11', '2020-12-14', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(13, 'Title for 019992', 'Details for 019992', '019992', '[]', 4900, 4951, '2021-01-11', '2021-01-11', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(14, 'Title for 019993', 'Details for 019993', '019993', '[]', 4900, 4950, '2021-01-13', '2021-01-13', NULL, 'CREATED', 'BOOK', null, null, 4953);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(15, 'Title for 019994', 'Details for 019994', '019994', '[]', 4900, 4950, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400006,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
       (400007,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL),
       (400008,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', '["019994"]'),
       (400009,  'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',  '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         'here is unused data',    NULL);

insert into casetasks(case_id, task_id)
values (15, 400006),
       (15, 400007),
       (15, 400008),
       (15, 400009);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(16, 'Title for 019995', 'Details for 019995', '019995', '[]', 4900, 4950, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400010, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
       (400011, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL),
       (400012, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',  '2021-01-27', 'METAKOMPAS',                  '2021-01-27', NULL,         'here is unused data',    NULL);

insert into casetasks(case_id, task_id)
values (16, 400010),
       (16, 400011),
       (16, 400012);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(17, 'Title for 019996', 'Details for 019996', '019996', '[]', 4900, 4950, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400013, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
       (400014, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL);

insert into casetasks(case_id, task_id)
values (17, 400013),
       (17, 400014);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(18, 'Title for 019997', 'Details for 019997', '019997', '[]', 4900, 4950, '2021-01-27', '2021-02-27', '2021-01-27', 'REJECTED', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400015, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
       (400016, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL);

insert into casetasks(case_id, task_id)
values (18, 400015),
       (18, 400016);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(19, 'Title for 019998', 'Details for 019998', '019998', '[]', 4900, 4950, '2020-12-27', '2021-01-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400017, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
       (400018, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL);

insert into casetasks(case_id, task_id)
values (19, 400017),
       (19, 400018);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(20, 'Title for 019999', 'Details for 019999', '019999', '[]', 4900, 4950, '2020-12-27', '2021-01-27', '2021-01-27', 'PENDING_ISSUES', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400019, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
       (400020, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL);

insert into casetasks(case_id, task_id)
values (20, 400019),
       (20, 400020);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(21, 'Title for 029999', 'Details for 029999', '029999', '[]', 4900, 4950, '2020-12-27', '2021-01-27', '2021-01-27', 'PENDING_EXTERNAL', 'BOOK', null, null, 4953);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (400021, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
       (400022, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL),
       (400023, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',  '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         'here is also some data', NULL);

insert into casetasks(case_id, task_id)
values (21, 400021),
       (21, 400022),
       (21, 400023);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id, author, publisher)
values (22, 'Title for 100000', 'Details for 100000', '100000', '["100001", "100002"]', 4900, 4950, '2021-01-09', '2021-02-09', '2021-01-10', 'EXPORTED', 'BOOK', 'BKM202104', '202104', 4953, 'Author for 100000', 'Publisher for 100000');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (400024,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '202-12-10', 'GROUP_1_LESS_THAN_100_PAGES', '2020-12-10',  NULL,         NULL,                        NULL),
        (400025,  'GROUP_2_100_UPTO_199_PAGES',  'DESCRIPTION',    '2020-12-10', 'GROUP_2_100_UPTO_199_PAGES',  NULL,         '2020-12-10', NULL,                        NULL),
        (400026,  'GROUP_3_200_UPTO_499_PAGES',  'DESCRIPTION',    '2020-12-10', 'GROUP_3_200_UPTO_499_PAGES',  NULL,         NULL,         'here is some data',         NULL),
        (400027,  'GROUP_4_500_OR_MORE_PAGES',   'DESCRIPTION',    '2020-12-10', 'GROUP_4_500_OR_MORE_PAGES',   NULL,         NULL,         '',                          NULL),
        (400028,  'MOVIES_GR_1',                 'DESCRIPTION',    '2020-12-10', 'MOVIES_GR_1',                 NULL,         NULL,         NULL,                        NULL),
        (400029,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',         NULL),
        (400030,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data',    NULL),
        (400031,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data',    '["019994"]'),
        (400032,  'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         'here is unused data',       NULL),
        (400033, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',         NULL),
        (400034, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data',    NULL),
        (400035, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-27', 'METAKOMPAS',                  '2021-01-27', NULL,         'here is unused data',       NULL),
        (400036, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',         NULL),
        (400037, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data',    NULL),
        (400038, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', 'BRIEF TEXT FOR 100000',     NULL),
        (400039, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', 'BRIEF TEXT FOR 100001',     '["100001"]'),
        (400040, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', 'BRIEF TEXT FOR 100002',     '["100001"]'),
        (400041, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',            '2021-01-27', 'BKM',                         '2021-02-08', '2021-02-09', 'Bogen findes relevant for biblioteker på Christiansø', NULL),
        (400042, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'DESCRIPTION TEXT',          NULL),
        (400043, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'EVALUATION TEXT',           NULL),
        (400044, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'TEXT BEFORE <t>48611435 Jeg tæller mine skridt</t> TEXT BETWEEN <t>48611435 Hest horse Pferd cheval love</t> TEXT AFTER', NULL),
        (400045, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'RECOMMENDATION TEXT',       NULL),
        (400046, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'TOPIC_1, TOPIC_2, TOPIC_3', '["100000"]'),
        (400047, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'TOPIC_1, TOPIC_2, TOPIC_4', '["100001"]'),
        (400048, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'TOPIC_2, TOPIC_3, TOPIC_5', '["100002"]'),
        (400049, 'GROUP_1_LESS_THAN_100_PAGES', 'AGE',            '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'AGE TEXT',                  NULL),
        (400050, 'GROUP_1_LESS_THAN_100_PAGES', 'MATLEVEL',       '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'MATLEVEL TEXT',             NULL);

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
       (22, 400050);

--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1000, 'Case 1', 'Details', '1001000', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401001, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1000, 401001);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1010, 'Case 2', 'Details', '1001010', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_MEETING', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401011, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1010, 401011);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1020, 'Case 3', 'Details', '1001020', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_EXPORT', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401021, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1020, 401021);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1030, 'Case 4', 'Details', '1001030', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'EXPORTED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401031, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1030, 401031);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1040, 'Case 5', 'Details', '1001040', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_REVERT', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401041, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1040, 401041);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1050, 'Case 6', 'Details', '1001050', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'REVERTED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401051, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1050, 401051);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1060, 'Case 7', 'Details', '1001060', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_CLOSE', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401061, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', NULL,        NULL, 'data', NULL),
        (401062, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',   '2021-01-13', 'BKM', '2021-01-13', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1060, 401061),
       (1060, 401062);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1070, 'Case 8', 'Details', '1001070', '[]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401071, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13', 'BRIEF', '2021-01-13', NULL, 'data', NULL),
        (401072, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',   '2021-01-13', 'BKM', '2021-01-13',   NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1070, 401071),
       (1070, 401072);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1080, 'Case 9', 'Details', '1001080', '[1001081, 1001082, 1001083]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401081, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', null),
        (401082, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001081]'),
        (401083, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001082]');

insert into casetasks(case_id, task_id)
values (1080, 401081),
       (1080, 401082),
       (1080, 401083);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1090, 'Case 10', 'Details', '1001090', '[1001091, 1001092, 1001093, 1001094]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401091, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', null),
        (401092, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001091]'),
        (401093, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001092, 1001093]');

insert into casetasks(case_id, task_id)
values (1090, 401091),
       (1090, 401092),
       (1090, 401093);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1100, 'Case 11', 'Details', '1001100', '[1001101, 1001102, 1001103, 1001104]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401101, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', null),
        (401102, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', null),
        (401103, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001101]'),
        (401104, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001102, 1001103]');

insert into casetasks(case_id, task_id)
values (1100, 401101),
       (1100, 401102),
       (1100, 401103),
       (1100, 401104);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1110, 'Case 12', 'Details', '1001110', '[1001111]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401111, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'data', NULL),
        (401112, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'data', '[1001111]'),
        (401113, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (401114, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (401115, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (401116, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (401117, 'GROUP_1_LESS_THAN_100_PAGES', 'BIBLIOGRAPHIC',  '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (401118, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1110, 401111),
       (1110, 401112),
       (1110, 401113),
       (1110, 401114),
       (1110, 401115),
       (1110, 401116),
       (1110, 401117),
       (1110, 401118);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1120, 'Case 13', 'Details', '1001120', '[1001121]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_APPROVAL', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401121, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13','BRIEF',                        '2021-01-13', NULL, 'data', NULL),
        (401122, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13','BRIEF',                        '2021-01-13', NULL, 'data', '[1001111]'),
        (401123, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (401124, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (401125, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL, 'data', NULL), -- Here be dragons!
        (401126, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (401127, 'GROUP_1_LESS_THAN_100_PAGES', 'BIBLIOGRAPHIC',  '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (401128, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1120, 401121),
       (1120, 401122),
       (1120, 401123),
       (1120, 401124),
       (1120, 401125),
       (1120, 401126),
       (1120, 401127),
       (1120, 401128);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1130, 'Case 14', 'Details', '1001130', '[1001131]', 4901, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_EXPORT', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401131, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13','BRIEF',        '2021-01-13', NULL, 'data', NULL),
        (401132, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', NULL),
        (401133, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', NULL),
        (401134, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', NULL),
        (401135, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', NULL),
        (401136, 'MOVIES_GR_2', 'BIBLIOGRAPHIC',  '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1130, 401131),
       (1130, 401132),
       (1130, 401133),
       (1130, 401134),
       (1130, 401135),
       (1130, 401136);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1140, 'Case 15', 'Details', '1001140', '[1001141,1001142]', 4901, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'EXPORTED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401141, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (401142, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (401143, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (401144, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (401145, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (401146, 'MOVIES_GR_2', 'BIBLIOGRAPHIC',  '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (401147, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL,                      'data', '[1001142]');

insert into casetasks(case_id, task_id)
values (1140, 401141),
       (1140, 401142),
       (1140, 401143),
       (1140, 401144),
       (1140, 401145),
       (1140, 401146),
       (1140, 401147);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1150, 'Case 16', 'Details', '1001150', '[1001151,1001152]', 4901, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401151, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       NULL,         NULL, 'data', NULL),
        (401152, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (401153, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (401154, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (401155, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (401156, 'MOVIES_GR_2', 'BIBLIOGRAPHIC',  '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (401157, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL, 'data', '[1001152]');

insert into casetasks(case_id, task_id)
values (1150, 401151),
       (1150, 401152),
       (1150, 401153),
       (1150, 401154),
       (1150, 401155),
       (1150, 401156),
       (1150, 401157);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1160, 'Case 17', 'Details', '1001160', '[1001161,1001162]', 4900, 4950, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401161, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       NULL,         NULL, 'data', NULL),
        (401162, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (401163, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (401164, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (401165, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (401166, 'MOVIES_GR_2', 'BIBLIOGRAPHIC',  '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (401167, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL, 'data', '[1001152]');

insert into casetasks(case_id, task_id)
values (1160, 401161),
       (1160, 401162),
       (1160, 401163),
       (1160, 401164),
       (1160, 401165),
       (1160, 401166),
       (1160, 401167);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1170, 'Case 18', 'Details', '1001170', '[1001171,1001172]', 4901, 4950, '2021-01-24', '2021-01-26', '2021-01-24', 'APPROVED', 'MULTIMEDIA');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401171, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',          '2021-01-26', NULL, 'data', NULL),
        (401172, 'MULTIMEDIA_FEE', 'DESCRIPTION',    '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', NULL),
        (401173, 'MULTIMEDIA_FEE', 'EVALUATION',     '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', NULL),
        (401174, 'MULTIMEDIA_FEE', 'COMPARISON',     '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', NULL),
        (401175, 'MULTIMEDIA_FEE', 'RECOMMENDATION', '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', NULL),
        (401176, 'MULTIMEDIA_FEE', 'BIBLIOGRAPHIC',  '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', NULL),
        (401177, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',          '2021-01-26', NULL, 'data', '[1001172]'),
        (401178, 'MULTIMEDIA_FEE', 'EXPRESS',        '2021-01-13', 'EXPRESS',        '2021-01-26', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1170, 401171),
       (1170, 401172),
       (1170, 401173),
       (1170, 401174),
       (1170, 401175),
       (1170, 401176),
       (1170, 401177),
       (1170, 401178);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1180, 'Case 19', 'Details', '1001180', '[1001181,1001182]', 4901, 4950, '2021-01-24', '2021-01-26', '2021-01-24', 'APPROVED', 'MULTIMEDIA');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (401181, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-26', NULL, 'data', NULL),
        (401182, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-26', NULL, 'data', '[1001182]'),
        (401183, 'MULTIMEDIA_FEE', 'EXPRESS',        '2021-01-13', 'EXPRESS',     '2021-01-26', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1180, 401181),
       (1180, 401182),
       (1180, 401183);
