-- expected CSV output when calling /payments/preview/?format=CSV:
--
-- --------------------------------------------------------------------------------------------------------------------
-- Dato;Lønnr.;Lønart;Antal;Tekst;Anmelder
-- mm-dd-åååå;123;1956;1;1001000 Case 1;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001010 Case 2;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001020 Case 3;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001030 Case 4;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001040 Case 5;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001050 Case 6;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001060 Case 7;Hans Hansen
-- mm-dd-åååå;123;1962;1;1001060 Bkm Case 7;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001070 Case 8;Hans Hansen
-- mm-dd-åååå;123;1962;1;1001070 Bkm Case 8;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001080,1001081,1001082,1001083 Case 9;Hans Hansen
-- mm-dd-åååå;123;1960;2;1001080,1001081,1001082,1001083 Note Case 9;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001090,1001091,1001092,1001093,1001094 Case 10;Hans Hansen
-- mm-dd-åååå;123;1960;2;1001090,1001091,1001092,1001093,1001094 Note Case 10;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001100,1001101,1001102,1001103,1001104 Case 11;Hans Hansen
-- mm-dd-åååå;123;1960;3;1001100,1001101,1001102,1001103,1001104 Note Case 11;Hans Hansen
-- mm-dd-åååå;123;1956;1;1001110,1001111 Case 12;Hans Hansen
-- mm-dd-åååå;123;1960;1;1001110,1001111 Note Case 12;Hans Hansen
-- mm-dd-åååå;123;1987;2;1001110,1001111 Metadata Case 12;Hans Hansen
-- mm-dd-åååå;456;1981;1;1001130,1001131 Case 14;Ole Olsen
-- mm-dd-åååå;456;1960;1;1001140,1001141,1001142 Note Case 15;Ole Olsen
-- mm-dd-åååå;456;1954;1;1001170,1001171,1001172 Case 18;Ole Olsen
-- mm-dd-åååå;456;1960;1;1001170,1001171,1001172 Note Case 18;Ole Olsen
-- mm-dd-åååå;456;1954;1;1001180,1001181,1001182 Case 19;Ole Olsen
-- mm-dd-åååå;456;1960;1;1001180,1001181,1001182 Note Case 19;Ole Olsen
-- mm-dd-åååå;123;1956;1;1001190,1001191 Case 20;Hans Hansen
-- mm-dd-åååå;123;1960;1;1001190,1001191 Note Case 20;Hans Hansen
-- mm-dd-åååå;123;1956;1;38529668,38529633 Case 20;Hans Hansen
-- mm-dd-åååå;123;1960;1;38529668,38529633 Note Case 20;Hans Hansen
-- --------------------------------------------------------------------------------------------------------------------

-- ********************************************************************************************************************
-- Minimal cases with a single task, testing all payed statuses.
-- None of these cases has any of the default tasks with description, evalutation etc. So they should not have
-- an extra payment for the standard fields.
-- ********************************************************************************************************************

-- Case 1: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001000 Case 1;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1000, 'Case 1', 'Details', '1001000', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1001, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', '["1001000"]');

insert into casetasks(case_id, task_id)
values (1000, 1001);

-- Case 2: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001010 Case 2;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1010, 'Case 2', 'Details', '1001010', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_MEETING', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1011, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', '["1001010"]');

insert into casetasks(case_id, task_id)
values (1010, 1011);

-- Case 3: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001020 Case 3;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1020, 'Case 3', 'Details', '1001020', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_EXPORT', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1021, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', '["1001020"]');

insert into casetasks(case_id, task_id)
values (1020, 1021);

-- Case 4: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001030 Case 4;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1030, 'Case 4', 'Details', '1001030', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'EXPORTED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1031, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', '["1001030"]');

insert into casetasks(case_id, task_id)
values (1030, 1031);

-- Case 5: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001040 Case 5;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1040, 'Case 5', 'Details', '1001040', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_REVERT', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1041, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', '["1001040"]');

insert into casetasks(case_id, task_id)
values (1040, 1041);

-- Case 6: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001050 Case 6;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1050, 'Case 6', 'Details', '1001050', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'REVERTED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1051, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-18', NULL, 'data', '["1001050"]');

insert into casetasks(case_id, task_id)
values (1050, 1051);

-- Case 7: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001060 Case 7;Hans Hansen
-- mm-dd-åååå;123;1962;1;1001060 Bkm Case 7;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1060, 'Case 7', 'Details', '1001060', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_CLOSE', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1061, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '["1001060"]'),
        (1062, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',   '2021-01-13', 'BKM',  '2021-01-13', NULL, 'data', '["1001060"]');

insert into casetasks(case_id, task_id)
values (1060, 1061),
       (1060, 1062);

-- Case 8: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001070 Case 8;Hans Hansen
-- mm-dd-åååå;123;1962;1;1001070 Bkm Case 8;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1070, 'Case 8', 'Details', '1001070', '[]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1071, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13', 'BRIEF', '2021-01-13', NULL, 'data', '["1001070"]'),
        (1072, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',   '2021-01-13', 'BKM', '2021-01-13',   NULL, 'data', '["1001070"]');

insert into casetasks(case_id, task_id)
values (1070, 1071),
       (1070, 1072);

-- ********************************************************************************************************************
-- Minimal cases with collective reviews requests.
-- Also only notes, no standard fields
-- ********************************************************************************************************************

-- Case 9: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001080,1001081,1001082,1001083 Case 9;Hans Hansen
-- mm-dd-åååå;123;1960;2;1001080,1001081,1001082,1001083 Note Case 9;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1080, 'Case 9', 'Details', '1001080', '[1001081, 1001082, 1001083]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1081, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '["1001080", "1001083"]'),
        (1082, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001081]'),
        (1083, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001082]');

insert into casetasks(case_id, task_id)
values (1080, 1081),
       (1080, 1082),
       (1080, 1083);

-- Case 10: PAYMENTS EXPECTED
-- Case has a task with two targetfausts (which is just fine, but still just 1 extra BRIEF task)
--
-- mm-dd-åååå;123;1956;1;1001090,1001091,1001092,1001093,1001094 Case 10;Hans Hansen
-- mm-dd-åååå;123;1960;3;1001090,1001091,1001092,1001093,1001094 Note Case 10;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1090, 'Case 10', 'Details', '1001090', '[1001091, 1001092, 1001093, 1001094]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1091, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '["1001090", "1001094"]'),
        (1092, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001091]'),
        (1093, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '[1001092, 1001093]');

insert into casetasks(case_id, task_id)
values (1090, 1091),
       (1090, 1092),
       (1090, 1093);

-- Case 11: PAYMENTS EXPECTED
-- Case has an extra repeated BRIEF task. This is not strictly forbidden, but it is a mess.
-- The extra BRIEF task, allthough having its own data, will never be output to DBCKat and thus never used.
-- The reviewer gets payed for the extra BRIEF allthough it is wasted!
--
-- mm-dd-åååå;123;1956;1;1001100,1001101,1001102,1001103,1001104 Note Case 11;Hans Hansen
-- mm-dd-åååå;123;1960;3;1001100,1001101,1001102,1001103,1001104 Case 11;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1100, 'Case 11', 'Details', '1001100', '[1001101, 1001102, 1001103, 1001104]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1101, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '["1001100", "1001104"]'),
        (1102, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '["1001102", "1001103"]'),
        (1103, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '["1001101"]'),
        (1104, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF', '2021-01-13','BRIEF', '2021-01-13', NULL, 'data', '["1001102", "1001103"]');

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
-- mm-dd-åååå;123;1960;1;1001110,1001111 Note Case 12;Hans Hansen
-- mm-dd-åååå;123;1987;2;1001110,1001111 Metadata Case 12;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1110, 'Case 12', 'Details', '1001110', '[1001111]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1111, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'data', '["1001110"]'),
        (1112, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'data', '["1001111"]'),
        (1113, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001110", "1001111"]'),
        (1114, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001110", "1001111"]'),
        (1115, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001110", "1001111"]'),
        (1116, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001110", "1001111"]'),
        (1117, 'GROUP_1_LESS_THAN_100_PAGES', 'AGE',            '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001110", "1001111"]'),
        (1118, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, 'data', '["1001110"]'),
        (1119, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, 'data', '["1001111"]');

insert into casetasks(case_id, task_id)
values (1110, 1111),
       (1110, 1112),
       (1110, 1113),
       (1110, 1114),
       (1110, 1115),
       (1110, 1116),
       (1110, 1117),
       (1110, 1118),
       (1110, 1119);

-- Case 13: payments NOT expected
-- Case has been changed back to ASSIGNED->PENDING_APPROVAL by adding and filling out an extra task
-- before the first tasks has been payed.
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1120, 'Case 13', 'Details', '1001120', '[1001121]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_APPROVAL', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1121, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13','BRIEF',                        '2021-01-13', NULL, 'data', '["1001120"]'),
        (1122, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13','BRIEF',                        '2021-01-13', NULL, 'data', '["1001121"]'),
        (1123, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001120", "1001121"]'),
        (1124, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001120", "1001121"]'),
        (1125, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL, 'data', '["1001120", "1001121"]'), -- Here be dragons!
        (1126, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001120", "1001121"]'),
        (1127, 'GROUP_1_LESS_THAN_100_PAGES', 'MATLEVEL',       '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001120", "1001121"]'),
        (1128, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, 'data', '["1001120"]'),
        (1129, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-13', 'METAKOMPAS',                  '2021-01-13', NULL, 'data', '["1001121"]');

insert into casetasks(case_id, task_id)
values (1120, 1121),
       (1120, 1122),
       (1120, 1123),
       (1120, 1124),
       (1120, 1125),
       (1120, 1126),
       (1120, 1127),
       (1120, 1128),
       (1120, 1129);

-- Case 14: PAYMENTS EXPECTED
--
-- mm-dd-åååå;456;1981;1;1001130,1001131 Case 14;Ole Olsen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1130, 'Case 14', 'Details', '1001130', '[1001131]', 2, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'PENDING_EXPORT', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1131, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13','BRIEF',        '2021-01-13', NULL, 'data', '["1001130", "1001131"]'),
        (1132, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', '["1001130", "1001131"]'),
        (1133, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', '["1001130", "1001131"]'),
        (1134, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', '["1001130", "1001131"]'),
        (1135, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', '["1001130", "1001131"]'),
        (1136, 'MOVIES_GR_2', 'AGE',            '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', '["1001130", "1001131"]'),
        (1137, 'MOVIES_GR_2', 'MATLEVEL',       '2021-01-13', 'MOVIES_GR_2', '2021-01-13', NULL, 'data', '["1001130", "1001131"]');

insert into casetasks(case_id, task_id)
values (1130, 1131),
       (1130, 1132),
       (1130, 1133),
       (1130, 1134),
       (1130, 1135),
       (1130, 1136),
       (1130, 1137);

-- Case 15: PAYMENTS EXPECTED
-- Case has been payed before, then an extra task has been added, filled out and approved
--
-- mm-dd-åååå;456;1960;1;1001140,1001141,1001142 Note Case 15;Ole Olsen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1140, 'Case 15', 'Details', '1001140', '[1001141,1001142]', 2, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'EXPORTED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1141, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2020-12-13', '2020-12-21 12:13:14.567', 'data', '["1001140", "1001141"]'),
        (1142, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', '["1001140", "1001141", "1001142"]'),
        (1143, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', '["1001140", "1001141", "1001142"]'),
        (1144, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', '["1001140", "1001141", "1001142"]'),
        (1145, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', '["1001140", "1001141", "1001142"]'),
        (1146, 'MOVIES_GR_2', 'AGE',            '2021-01-13', 'MOVIES_GR_2', '2020-12-13', '2020-12-21 12:13:14.567', 'data', '["1001140", "1001141", "1001142"]'),
        (1147, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL,                      'data', '["1001142"]');

insert into casetasks(case_id, task_id)
values (1140, 1141),
       (1140, 1142),
       (1140, 1143),
       (1140, 1144),
       (1140, 1145),
       (1140, 1146),
       (1140, 1147);


-- Case 16: PAYMENTS NOT EXPECTED
-- Not all tasks has been approved, Case should not be payed just yet
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1150, 'Case 16', 'Details', '1001150', '[1001151,1001152]', 2, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'MOVIE');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1151, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       NULL,         NULL, 'data', '["1001150", "1001151"]'),
        (1152, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', '["1001150", "1001151", "1001152"]'),
        (1153, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', '["1001150", "1001151", "1001152"]'),
        (1154, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', '["1001150", "1001151", "1001152"]'),
        (1155, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', '["1001150", "1001151", "1001152"]'),
        (1156, 'MOVIES_GR_2', 'MATLEVEL',       '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', '["1001150", "1001151", "1001152"]'),
        (1157, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL, 'data', '["1001150", "1001151", "1001152"]');

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
values  (1161, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       NULL,         NULL, 'data', '["1001160", "1001161"]'),
        (1162, 'MOVIES_GR_2', 'DESCRIPTION',    '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', '["1001160", "1001161", "1001162"]'),
        (1163, 'MOVIES_GR_2', 'EVALUATION',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', '["1001160", "1001161", "1001162"]'),
        (1164, 'MOVIES_GR_2', 'COMPARISON',     '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', '["1001160", "1001161", "1001162"]'),
        (1165, 'MOVIES_GR_2', 'RECOMMENDATION', '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', '["1001160", "1001161", "1001162"]'),
        (1166, 'MOVIES_GR_2', 'AGE',            '2021-01-13', 'MOVIES_GR_2', NULL,         NULL, 'data', '["1001160", "1001161", "1001162"]'),
        (1167, 'MOVIES_GR_2', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-13', NULL, 'data', '["1001162"]');

insert into casetasks(case_id, task_id)
values (1160, 1161),
       (1160, 1162),
       (1160, 1163),
       (1160, 1164),
       (1160, 1165),
       (1160, 1166),
       (1160, 1167);

-- Case 18: PAYMENTS EXPECTED, but EXPRESS should not be counted separately (should be ignored)
-- Case is an express case, so an additional payment for express delivery is expected
--
-- mm-dd-åååå;456;1954;1;1001170,1001171,1001172 Case 18;Ole Olsen
-- mm-dd-åååå;456;1960;1;1001170,1001171,1001172 Note Case 18;Ole Olsen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1170, 'Case 18', 'Details', '1001170', '[1001171,1001172]', 2, 10, '2021-01-24', '2021-01-26', '2021-01-24', 'APPROVED', 'MULTIMEDIA');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1171, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',          '2021-01-26', NULL, 'data', '["1001170", "1001171"]'),
        (1172, 'MULTIMEDIA_FEE', 'DESCRIPTION',    '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', '["1001170", "1001171", "1001172"]'),
        (1173, 'MULTIMEDIA_FEE', 'EVALUATION',     '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', '["1001170", "1001171", "1001172"]'),
        (1174, 'MULTIMEDIA_FEE', 'COMPARISON',     '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', '["1001170", "1001171", "1001172"]'),
        (1175, 'MULTIMEDIA_FEE', 'RECOMMENDATION', '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', '["1001170", "1001171", "1001172"]'),
        (1176, 'MULTIMEDIA_FEE', 'MATLEVEL',       '2021-01-13', 'MULTIMEDIA_FEE', '2021-01-26', NULL, 'data', '["1001170", "1001171", "1001172"]'),
        (1177, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',          '2021-01-26', NULL, 'data', '["1001172"]'),
        (1178, 'MULTIMEDIA_FEE', 'EXPRESS',        '2021-01-13', 'EXPRESS',        '2021-01-26', NULL, 'data', '["1001170", "1001171", "1001172"]');

insert into casetasks(case_id, task_id)
values (1170, 1171),
       (1170, 1172),
       (1170, 1173),
       (1170, 1174),
       (1170, 1175),
       (1170, 1176),
       (1170, 1177),
       (1170, 1178);

-- Case 19: PAYMENTS EXPECTED, but EXPRESS should not be counted separately (should be ignored)
--
-- mm-dd-åååå;456;1954;1;1001180,1001181,1001182 Case 19;Ole Olsen
-- mm-dd-åååå;456;1960;1;1001180,1001181,1001182 Note Case 19;Ole Olsen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1180, 'Case 19', 'Details', '1001180', '[1001181,1001182]', 2, 10, '2021-01-24', '2021-01-26', '2021-01-24', 'APPROVED', 'MULTIMEDIA');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1181, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-26', NULL, 'data', '["1001180", "1001181"]'),
        (1182, 'MULTIMEDIA_FEE', 'BRIEF',          '2021-01-13', 'BRIEF',       '2021-01-26', NULL, 'data', '["1001182"]'),
        (1183, 'MULTIMEDIA_FEE', 'EXPRESS',        '2021-01-13', 'EXPRESS',     '2021-01-26', NULL, 'data', '["1001180", "1001181", "1001182"]');

insert into casetasks(case_id, task_id)
values (1180, 1181),
       (1180, 1182),
       (1180, 1183);

-- Case 20: PAYMENTS EXPECTED
--
-- mm-dd-åååå;123;1956;1;1001190,1001191 Case 20;Hans Hansen
-- mm-dd-åååå;123;1960;1;1001190,1001191 Note Case 20;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1190, 'Case 20', 'Details', '1001190', '[1001191]', 1, 10, '2021-01-13', '2021-02-13', '2021-01-13', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1191, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'data', '["1001190"]'),
        (1192, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'data', '["1001191"]'),
        (1193, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001190", "1001191"]'),
        (1194, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001190", "1001191"]'),
        (1195, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001190", "1001191"]'),
        (1196, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001190", "1001191"]'),
        (1197, 'GROUP_1_LESS_THAN_100_PAGES', 'AGE',            '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001190", "1001191"]'),
        (1198, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001190"]'),
        (1199, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["1001191"]');

insert into casetasks(case_id, task_id)
values (1190, 1191),
       (1190, 1192),
       (1190, 1193),
       (1190, 1194),
       (1190, 1195),
       (1190, 1196),
       (1190, 1197),
       (1190, 1198),
       (1190, 1199);

-- Case 21: PAYMENTS EXPECTED
-- Example taken from latest (as of today) payment list
--
-- """
-- 1956	1	38529668,38529633, Min lille bog om kroppen	Line Hoffgaard
-- 1960	1	38529668,38529633, Kort om, +1: Min lille bog om kroppen	Line Hoffgaard
-- """
--
-- mm-dd-åååå;123;1956;1;38529668,38529633 Case 20;Hans Hansen
-- mm-dd-åååå;123;1960;1;38529668,38529633 Note Case 20;Hans Hansen
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1200, 'Case 20', 'Details', '38529668', '["38529633"]', 1, 10, '2021-02-18', '2021-03-18', '2021-03-18', 'APPROVED', 'BOOK');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1201, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'data', '["38529668"]'),
        (1202, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-13', 'BRIEF',                       '2021-01-13', NULL, 'data', '["38529633"]'),
        (1203, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["38529668", "38529633"]'),
        (1204, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["38529668", "38529633"]'),
        (1205, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["38529668", "38529633"]'),
        (1206, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["38529668", "38529633"]'),
        (1207, 'GROUP_1_LESS_THAN_100_PAGES', 'AGE',            '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["38529668", "38529633"]'),
        (1208, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["38529668"]'),
        (1209, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-13', 'GROUP_1_LESS_THAN_100_PAGES', '2021-01-13', NULL, 'data', '["38529633"]');

insert into casetasks(case_id, task_id)
values (1200, 1201),
       (1200, 1202),
       (1200, 1203),
       (1200, 1204),
       (1200, 1205),
       (1200, 1206),
       (1200, 1207),
       (1200, 1208),
       (1200, 1209);


