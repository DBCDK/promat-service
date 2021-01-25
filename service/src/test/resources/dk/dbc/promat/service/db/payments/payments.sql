-- expected CSV output when calling /payments/preview/?format=CSV:
--
-- --------------------------------------------------------------------------------------------------------------------
-- Dato;Lønnr.;Lønart;Antal;Tekst;Anmelder
-- mm-dd-åååå;123;1960;1;1001000 Note Case 1;Hans Hansen
-- mm-dd-åååå;123;1960;1;1001010 Note Case 2;Hans Hansen
-- mm-dd-åååå;123;1960;1;1001020 Note Case 3;Hans Hansen
-- mm-dd-åååå;123;1960;1;1001030 Note Case 4;Hans Hansen
-- mm-dd-åååå;123;1960;1;1001040 Note Case 5;Hans Hansen
-- mm-dd-åååå;123;1960;1;1001050 Note Case 6;Hans Hansen
-- mm-dd-åååå;123;1962;1;1001060 Bkm Case 7;Hans Hansen
-- mm-dd-åååå;123;1960;1;1001070 Note Case 8;Hans Hansen
-- mm-dd-åååå;123;1962;1;1001070 Bkm Case 8;Hans Hansen
-- mm-dd-åååå;123;1960;3;1001080,1001081,1001082,1001083 Note Case 9;Hans Hansen
-- mm-dd-åååå;123;1960;3;1001090,1001091,1001092,1001093,1001094 Note Case 10;Hans Hansen
-- mm-dd-åååå;123;1960;4;1001100,1001101,1001102,1001103,1001104 Note Case 11;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001110,1001111 Case 12;Hans Hansen
-- mm-dd-åååå;123;1960;2;1001110,1001111 Note Case 12;Hans Hansen
-- mm-dd-åååå;123;1987;1;1001110,1001111 Metadata Case 12;Hans Hansen
-- mm-dd-åååå;456;1981;1;1001130,1001131 Case 14;Ole Olsen
-- mm-dd-åååå;456;1960;1;1001130,1001131 Note Case 14;Ole Olsen
-- mm-dd-åååå;456;1960;1;1001140,1001141,1001142 Note Case 15;Ole Olsen
-- mm-dd-åååå;456;1960;1;1001140,1001141,1001142 Note Case 16;Ole Olsen
-- --------------------------------------------------------------------------------------------------------------------

-- ********************************************************************************************************************
-- Minimal cases with a single task, testing all payed statuses.
-- None of these cases has any of the default tasks with description, evalutation etc. So they should not have
-- an extra payment for the standard fields.
-- ********************************************************************************************************************

-- Case 1: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1960;1;1001000 Note Case 1;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1000, 'Case 1', 'Details', '1001000', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1001, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1000, 1001);

-- Case 2: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1960;1;1001010 Note Case 2;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1010, 'Case 2', 'Details', '1001010', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_MEETING', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1011, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1010, 1011);

-- Case 3: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1960;1;1001020 Note Case 3;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1020, 'Case 3', 'Details', '1001020', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_EXPORT', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1021, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1020, 1021);

-- Case 4: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1960;1;1001030 Note Case 4;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1030, 'Case 4', 'Details', '1001030', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'EXPORTED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1031, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1030, 1031);

-- Case 5: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1960;1;1001040 Note Case 5;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1040, 'Case 5', 'Details', '1001040', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_REVERT', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1041, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1040, 1041);

-- Case 6: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1960;1;1001050 Note Case 6;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1050, 'Case 6', 'Details', '1001050', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'REVERTED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1051, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1050, 1051);

-- Case 7: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1962;1;1001060 Bkm Case 7;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1060, 'Case 7', 'Details', '1001060', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_CLOSE', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1061, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', NULL,        NULL, 'data', NULL),
        (1062, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',   '2021-01-13', 'BKM', '2021-01-13', NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1060, 1061),
       (1060, 1062);

-- Case 8: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1960;1;1001070 Note Case 8;Hans Hansen
-- mm-dd-åååå;123;1962;1;1001070 Bkm Case 8;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1070, 'Case 8', 'Details', '1001070', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1071, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13', 'BRIEF', '2021-01-13', NULL, 'data', NULL),
        (1072, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',   '2021-01-13', 'BKM', '2021-01-13',   NULL, 'data', NULL);

insert into casetasks(case_id, task_id)
values (1070, 1071),
       (1070, 1072);

-- ********************************************************************************************************************
-- Minimal cases with collective reviews requests.
-- Also only notes, no standard fields
-- ********************************************************************************************************************

-- Case 9: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1960;3;1001080,1001081,1001082,1001083 Note Case 9;Hans Hansen
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

-- Case 10: PAYMENTS EXPECTED
-- Case has a task with two targetfausts (which is just fine, but still just 1 extra BRIEF task)
--
-- mm-dd-åååå;123;1960;3;1001090,1001091,1001092,1001093,1001094 Note Case 10;Hans Hansen
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

-- Case 11: PAYMENTS EXPECTED
-- Case has an extra BRIEF task without targetfausts. This is not strictly forbidden, but it is a mess.
-- The extra BRIEF task, allthough having its own data, will never be output to DBCKat and thus never used.
-- The reviewer gets payed for the extra BRIEF allthough it is wasted!
--
-- mm-dd-åååå;123;1960;4;1001100,1001101,1001102,1001103,1001104 Note Case 11;Hans Hansen
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

-- ********************************************************************************************************************
-- Case with multiple (standard tasks)
-- ********************************************************************************************************************

-- Case 12: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001110,1001111 Case 12;Hans Hansen
-- mm-dd-åååå;123;1960;2;1001110,1001111 Note Case 12;Hans Hansen
-- mm-dd-åååå;123;1987;1;1001110,1001111 Metadata Case 12;Hans Hansen
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

-- Case 13: payments NOT expected
-- Case has been changed back to ASSIGNED->PENDING_APPROVAL by adding and filling out an extra task
-- before the first tasks has been payed.
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

-- Case 14: PAYMENTS EXPECTED
--
-- mm-dd-åååå;456;1981;1;1001130,1001131 Case 14;Ole Olsen
-- mm-dd-åååå;456;1960;1;1001130,1001131 Note Case 14;Ole Olsen
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

-- Case 15: PAYMENTS EXPECTED
-- Case has been payed before, then an extra task has been added, filled out and approved
--
-- mm-dd-åååå;456;1960;1;1001140,1001141,1001142 Note Case 15;Ole Olsen
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


-- Case 16: PAYMENTS EXPECTED
-- Not all tasks has been approved, but some has - and they should be payed.
-- (this is a somewhat made-up situation, but in theory, this could happen)
--
-- mm-dd-åååå;456;1960;1;1001150,1001151,1001152 Note Case 16;Ole Olsen
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

-- Case 17: INVALID
-- This construct is highly unlikely, but it triggers an exception to be thrown while
-- building the paymentlist, which could happen for other reasons.. And we want to
-- check exception and transaction handling
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1160, 'Case 17', 'Details', '1001160', '[1001161,1001162]', NULL, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'MOVIE');

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
