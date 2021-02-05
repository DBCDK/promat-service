-- Reviewers
insert into promatuser(id, role, active, culrid, firstname, lastname, email, address1, zip, city, institution, paycode, hiatus_begin, hiatus_end, accepts, note, capacity)
values (1, 'REVIEWER', true, '41', 'Hans', 'Hansen', 'hans@hansen.dk', 'Lillegade 1', '9999', 'Lilleved', 'Hans Hansens Bix', 123, '2020-10-28', '2020-11-01', '["MULTIMEDIA", "PS4", "PS5"]', 'note1', 1),
       (2, 'REVIEWER', true, '42', 'Ole', 'Olsen', 'ole@olsen.dk', 'Storegade 99', '1111', 'Storeved', 'Ole Olsens Goodies', 456, '2020-11-28', '2020-12-01', '["MULTIMEDIA", "PS4", "PS5"]', 'note2', 2);

-- Editors
insert into promatuser(id, role, active, culrid, firstname, lastname, email, paycode)
values (10, 'EDITOR', true, '51', 'Ed', 'Itor', 'ed.itor@dbc.dk', 5678),
       (11, 'EDITOR', true, '52', 'Edit', 'Or', 'edit.or@dbc.dk', 1111),
       (12, 'EDITOR', true, '53', 'Edi', 'tor', 'edi.tor@dbc.dk', 2760),
       (13, 'EDITOR', true, '54', 'Editte', 'Ore', 'editte.ore@dbc.dk', 2222),
       (14, 'EDITOR', true, '56', 'E', 'ditor', 'e.ditor@dbc.dk', 2860);

-- Notifications
INSERT INTO notification(bodytext, subject, toaddress, status, created)
values ('<h1>hej1</h1>', 'test1', 'test1@test.dk', 0, now()),
       ('<h1>hej2</h1>', 'test2', 'test2@test.dk', 1, now()),
       ('<h1>hej3</h1>', 'test3', 'test3@test.dk', 0, now());

-- Subjects
insert into subject(id, name, parentid)
VALUES (1, 'Voksen', null),
       (2, 'Roman', 1),
       (3, 'Eventyr, fantasy', 2),
       (4, 'Digte', 1),
       (5, 'Multimedie', null);

insert into reviewersubjects(subject_id, reviewer_id)
values(5,1),
      (3,1),
      (5,2),
      (4,2),
      (4,1);

-- Cases
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (1, 'Title for 001111', 'Details for 001111', '001111', '["002222","003333"]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', 'BKM202101', '202101', 13);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (1,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '202-12-10', 'GROUP_1_LESS_THAN_100_PAGES', '2020-12-10',  NULL,         NULL,                     NULL),
       (2,  'GROUP_2_100_UPTO_199_PAGES',  'DESCRIPTION', '2020-12-10', 'GROUP_2_100_UPTO_199_PAGES',  NULL,         '2020-12-10', NULL,                     NULL),
       (3,  'GROUP_3_200_UPTO_499_PAGES',  'DESCRIPTION', '2020-12-10', 'GROUP_3_200_UPTO_499_PAGES',  NULL,         NULL,         'here is some data',      NULL),
       (4,  'GROUP_4_500_OR_MORE_PAGES',   'DESCRIPTION', '2020-12-10', 'GROUP_4_500_OR_MORE_PAGES',   NULL,         NULL,         '',                       NULL),
       (5,  'MOVIES_GR_1',                 'DESCRIPTION', '2020-12-10', 'MOVIES_GR_1',                 NULL,         NULL,         NULL,                     NULL);

insert into casetasks(case_id, task_id)
values (1, 1),
       (1, 2),
       (1, 3),
       (1, 4),
       (1, 5);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (2, 'Title for 004444', 'Details for 004444', '004444', '["005555","006666"]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'MOVIE', 'DBF202049', '202049', 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (3, 'Title for 011111', 'Details for 011111', '011111', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (4, 'Title for 012222', 'Details for 012222', '012222', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'EXPORTED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (5, 'Title for 013333', 'Details for 013333', '013333', '[]', 1, 10, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(6, 'Title for 014444', 'Details for 014444', '014444', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'ASSIGNED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(7, 'Title for 015555', 'Details for 015555', '015555', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(8, 'Title for 016666', 'Details for 016666', '016666', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(9, 'Title for 017777', 'Details for 017777', '017777', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'CLOSED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(10, 'Title for 018888', 'Details for 018888', '018888', '[]', NULL, 11, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(11, 'Title for 019999', 'Details for 019999', '019999', '[]', 2, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(12, 'Title for 019991', 'Details for 019991', '019991', '[]', 1, 11, '2020-11-11', '2020-12-14', NULL, 'CREATED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(13, 'Title for 019992', 'Details for 019992', '019992', '[]', 1, 11, '2021-01-11', '2021-01-11', NULL, 'CREATED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(14, 'Title for 019993', 'Details for 019993', '019993', '[]', 1, 10, '2021-01-13', '2021-01-13', NULL, 'CREATED', 'BOOK', null, null, 13);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(15, 'Title for 019994', 'Details for 019994', '019994', '[]', 1, 10, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 13);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (6,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
       (7,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL),
       (8,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', '["019994"]'),
       (9,  'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',  '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         'here is unused data',    NULL);

insert into casetasks(case_id, task_id)
values (15, 6),
       (15, 7),
       (15, 8),
       (15, 9);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(16, 'Title for 019995', 'Details for 019995', '019995', '[]', 1, 10, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 13);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (10, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
       (11, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL),
       (12, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',  '2021-01-27', 'METAKOMPAS',                  '2021-01-27', NULL,         'here is unused data',    NULL);

insert into casetasks(case_id, task_id)
values (16, 10),
       (16, 11),
       (16, 12);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values(17, 'Title for 019996', 'Details for 019996', '019996', '[]', 1, 10, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 13);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values (13, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
       (14, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL);

insert into casetasks(case_id, task_id)
values (17, 13),
       (17, 14);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1000, 'Case 1', 'Details', '1001000', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1001, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1000, 1001);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1010, 'Case 2', 'Details', '1001010', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_MEETING', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1011, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1010, 1011);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1020, 'Case 3', 'Details', '1001020', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_EXPORT', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1021, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1020, 1021);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1030, 'Case 4', 'Details', '1001030', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'EXPORTED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1031, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1030, 1031);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1040, 'Case 5', 'Details', '1001040', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_REVERT', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1041, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1040, 1041);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1050, 'Case 6', 'Details', '1001050', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'REVERTED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1051, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1050, 1051);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1060, 'Case 7', 'Details', '1001060', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_CLOSE', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1061, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', NULL,        NULL, 'data', NULL),
        (1062, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',   '2021-01-13', 'BKM', '2021-01-13', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1060, 1061),
       (1060, 1062);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1070, 'Case 8', 'Details', '1001070', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1071, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13', 'BRIEF', '2021-01-13', NULL, 'data', NULL),
        (1072, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',   '2021-01-13', 'BKM', '2021-01-13',   NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1070, 1071),
       (1070, 1072);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1080, 'Case 9', 'Details', '1001080', '[1001081, 1001082, 1001083]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1081, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', null),
        (1082, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001081]'),
        (1083, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001082]');

insert into casetasks(case_id, task_id)
values (1080, 1081),
       (1080, 1082),
       (1080, 1083);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1090, 'Case 10', 'Details', '1001090', '[1001091, 1001092, 1001093, 1001094]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1091, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', null),
        (1092, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001091]'),
        (1093, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001092, 1001093]');

insert into casetasks(case_id, task_id)
values (1090, 1091),
       (1090, 1092),
       (1090, 1093);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1100, 'Case 11', 'Details', '1001100', '[1001101, 1001102, 1001103, 1001104]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1101, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', null),
        (1102, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', null),
        (1103, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001101]'),
        (1104, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001102, 1001103]');

insert into casetasks(case_id, task_id)
values (1100, 1101),
       (1100, 1102),
       (1100, 1103),
       (1100, 1104);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1110, 'Case 12', 'Details', '1001110', '[1001111]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1111, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'data', NULL),
        (1112, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'data', '[1001111]'),
        (1113, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (1114, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (1115, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (1116, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (1117, 'GROUP_1_LESS_THAN_100_PAGES', 'BIBLIOGRAPHIC',  '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (1118, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1110, 1111),
       (1110, 1112),
       (1110, 1113),
       (1110, 1114),
       (1110, 1115),
       (1110, 1116),
       (1110, 1117),
       (1110, 1118);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1120, 'Case 13', 'Details', '1001120', '[1001121]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_APPROVAL', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1121, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13','BRIEF',                        '2021-01-13', NULL, 'data', NULL),
        (1122, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13','BRIEF',                        '2021-01-13', NULL, 'data', '[1001111]'),
        (1123, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (1124, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (1125, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL, 'data', NULL), -- Here be dragons!
        (1126, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (1127, 'GROUP_1_LESS_THAN_100_PAGES', 'BIBLIOGRAPHIC',  '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', NULL),
        (1128, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1120, 1121),
       (1120, 1122),
       (1120, 1123),
       (1120, 1124),
       (1120, 1125),
       (1120, 1126),
       (1120, 1127),
       (1120, 1128);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1130, 'Case 14', 'Details', '1001130', '[1001131]', 2, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_EXPORT', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1131, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13','BRIEF',        '2021-01-13', NULL, 'data', NULL),
        (1132, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', NULL),
        (1133, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', NULL),
        (1134, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', NULL),
        (1135, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', NULL),
        (1136, 'MOVIES_GR_2', 'BIBLIOGRAPHIC',  '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1130, 1131),
       (1130, 1132),
       (1130, 1133),
       (1130, 1134),
       (1130, 1135),
       (1130, 1136);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1140, 'Case 15', 'Details', '1001140', '[1001141,1001142]', 2, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'EXPORTED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1141, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (1142, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (1143, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (1144, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (1145, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (1146, 'MOVIES_GR_2', 'BIBLIOGRAPHIC',  '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', NULL),
        (1147, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL,                      'data', '[1001142]');

insert into casetasks(case_id, task_id)
values (1140, 1141),
       (1140, 1142),
       (1140, 1143),
       (1140, 1144),
       (1140, 1145),
       (1140, 1146),
       (1140, 1147);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1150, 'Case 16', 'Details', '1001150', '[1001151,1001152]', 2, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1151, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       NULL,         NULL, 'data', NULL),
        (1152, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (1153, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (1154, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (1155, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (1156, 'MOVIES_GR_2', 'BIBLIOGRAPHIC',  '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (1157, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL, 'data', '[1001152]');

insert into casetasks(case_id, task_id)
values (1150, 1151),
       (1150, 1152),
       (1150, 1153),
       (1150, 1154),
       (1150, 1155),
       (1150, 1156),
       (1150, 1157);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1160, 'Case 17', 'Details', '1001160', '[1001161,1001162]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1161, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       NULL,         NULL, 'data', NULL),
        (1162, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (1163, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (1164, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (1165, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (1166, 'MOVIES_GR_2', 'BIBLIOGRAPHIC',  '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', NULL),
        (1167, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL, 'data', '[1001152]');

insert into casetasks(case_id, task_id)
values (1160, 1161),
       (1160, 1162),
       (1160, 1163),
       (1160, 1164),
       (1160, 1165),
       (1160, 1166),
       (1160, 1167);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1170, 'Case 18', 'Details', '1001170', '[1001171,1001172]', 2, 10, '2021-01-24', '2021-01-26', '2021-01-24', 'APPROVED', 'MULTIMEDIA');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1171, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',          '2021-01-26', NULL, 'data', NULL),
        (1172, 'MULTIMEDIA_FEE', 'DESCRIPTION',    '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', NULL),
        (1173, 'MULTIMEDIA_FEE', 'EVALUATION',     '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', NULL),
        (1174, 'MULTIMEDIA_FEE', 'COMPARISON',     '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', NULL),
        (1175, 'MULTIMEDIA_FEE', 'RECOMMENDATION', '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', NULL),
        (1176, 'MULTIMEDIA_FEE', 'BIBLIOGRAPHIC',  '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', NULL),
        (1177, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',          '2021-01-26', NULL, 'data', '[1001172]'),
        (1178, 'MULTIMEDIA_FEE', 'EXPRESS',        '2021-01-13', 'EXPRESS',        '2021-01-26', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1170, 1171),
       (1170, 1172),
       (1170, 1173),
       (1170, 1174),
       (1170, 1175),
       (1170, 1176),
       (1170, 1177),
       (1170, 1178);
--
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1180, 'Case 19', 'Details', '1001180', '[1001181,1001182]', 2, 10, '2021-01-24', '2021-01-26', '2021-01-24', 'APPROVED', 'MULTIMEDIA');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1181, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-26', NULL, 'data', NULL),
        (1182, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-26', NULL, 'data', '[1001182]'),
        (1183, 'MULTIMEDIA_FEE', 'EXPRESS',        '2021-01-13', 'EXPRESS',     '2021-01-26', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1180, 1181),
       (1180, 1182),
       (1180, 1183);



