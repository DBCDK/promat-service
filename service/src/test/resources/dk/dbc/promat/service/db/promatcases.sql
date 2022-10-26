-- data for testDbckatXmlViewMultipleBKMReasons test
insert into promatcase( id, title, details, primaryFaust, relatedFausts, reviewer_id, created, deadline, assigned, status,
                       materialtype, editor_id, weekcode, author, creator_id, publisher, trimmedWeekcode, fulltextlink, remindersent, codes, keepeditor)
               values (500706, 'Far!', '', '22436406', '[]', 10001, '2022-10-20', '2022-10-31', '2022-10-20', 'REJECTED',
                       'BOOK', 10, null, 'Jacobsen, Steffen', 13, 'Kbh., Høst, 1999', null, null, null, '["DBF199921", "SFD199921"]' , false );

insert into promattask(id, tasktype, created, paycategory, approved, payed, data, targetfausts, taskfieldtype, recordid)
 values (504433, 'GROUP_1_LESS_THAN_100_PAGES', '2022-10-20', 'BRIEF'                      , null, null, '', '["22436406"]', 'BRIEF'          , null),
        (504434, 'GROUP_1_LESS_THAN_100_PAGES', '2022-10-20', 'GROUP_1_LESS_THAN_100_PAGES', null, null, '', '["22436406"]', 'EVALUATION'     , null),
        (504435, 'GROUP_1_LESS_THAN_100_PAGES', '2022-10-20', 'GROUP_1_LESS_THAN_100_PAGES', null, null, '', '["22436406"]', 'DESCRIPTION'    , null),
        (504436, 'GROUP_1_LESS_THAN_100_PAGES', '2022-10-20', 'GROUP_1_LESS_THAN_100_PAGES', null, null, '', '["22436406"]', 'DESCRIPTION'    , null),
        (504437, 'GROUP_1_LESS_THAN_100_PAGES', '2022-10-20', 'GROUP_1_LESS_THAN_100_PAGES', null, null, '', '["22436406"]', 'DESCRIPTION'    , null),
        (504438, 'GROUP_1_LESS_THAN_100_PAGES', '2022-10-20', 'BKM'                        , null, null,
         'Illustrationerne er af for ringe kvalitet\nHistorien/plottet hænger ikke sammen' , '["22436406"]', 'BKM', '');

insert into casetasks(task_id, case_id)
values (504433, 500706),
       (504434, 500706),
       (504435, 500706),
       (504436, 500706),
       (504437, 500706),
       (504438, 500706);

-- data for other cases
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id, author, publisher, codes)
values (1, 'Title for 001111', 'Details for 001111', '001111',    '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL,         'CREATED',          'BOOK',  'BKM202101', '202101', 13, '',                  '', '[]'),
       (2, 'Title for 004444', 'Details for 004444', '004444',    '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL,         'CREATED',          'MOVIE', 'DBF202049', '202049', 13, '',                  '', '[]'),
       (3, 'Title for 011111', 'Details for 011111', '011111',    '[]', 1,    NULL, '2020-11-11', '2020-12-11', NULL,         'CREATED',          'BOOK',  null,        null,     13, '',                  '', '[]'),
       (4, 'Title for 012222', 'Details for 012222', '012222',    '[]', 1,    NULL, '2020-11-11', '2020-12-11', NULL,         'EXPORTED',         'BOOK',  null,        null,     13, '',                  '', '[]'),
       (5, 'Title for 013333', 'Details for 013333', '013333',    '[]', 1,    10,   '2020-11-11', '2020-12-11', NULL,         'CREATED',          'BOOK',  null,        null,     13, '',                  '', '[]'),
       (6, 'Title for 014444', 'Details for 014444', '014444',    '[]', 1,    10,   '2020-11-11', '2020-12-11', '2020-11-11', 'ASSIGNED',         'BOOK',  null,        null,     13, '',                  '', '[]'),
       (7, 'Title for 015555', 'Details for 015555', '015555',    '[]', 1,    NULL, '2020-11-11', '2020-12-11', NULL,         'CREATED',          'BOOK',  null,        null,     13, '',                  '', '[]'),
       (8, 'Title for 016666', 'Details for 016666', '016666',    '[]', 1,    NULL, '2020-11-11', '2020-12-11', NULL,         'CREATED',          'BOOK',  null,        null,     13, '',                  '', '[]'),
       (9, 'Title for 017777', 'Details for 017777', '017777',    '[]', 1,    NULL, '2020-11-11', '2020-12-11', NULL,         'CLOSED',           'BOOK',  null,        null,     13, '',                  '', '[]'),
       (10, 'Title for 018888', 'Details for 018888', '018888',   '[]', NULL, 11,   '2020-11-11', '2020-12-11', NULL,         'CREATED',          'BOOK',  null,        null,     13, '',                  '', '[]'),
       (11, 'Title for 019999', 'Details for 019999', '019999',   '[]', 2,    NULL, '2020-11-11', '2020-12-11', NULL,         'CREATED',          'BOOK',  null,        null,     13, '',                  '', '[]'),
       (12, 'Title for 019991', 'Details for 019991', '019991',   '[]', 1,    11,   '2020-11-11', '2020-12-14', NULL,         'CREATED',          'BOOK',  null,        null,     13, '',                  '', '[]'),
       (13, 'Title for 019992', 'Details for 019992', '019992',   '[]', 1,    11,   '2021-01-11', '2021-01-11', NULL,         'CREATED',          'BOOK',  null,        null,     13, '',                  '', '[]'),
       (14, 'Title for 019993', 'Details for 019993', '019993',   '[]', 1,    10,   '2021-01-13', '2021-01-13', NULL,         'CREATED',          'BOOK',  null,        null,     13, '',                  '', '[]'),
       (15, 'Title for 019994', 'Details for 019994', '019994',   '[]', 1,    10,   '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED',         'BOOK',  null,        null,     13, '',                  '', '[]'),
       (16, 'Title for 019995', 'Details for 019995', '019995',   '[]', 1,    10,   '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED',         'BOOK',  null,        null,     13, '',                  '', '[]'),
       (17, 'Title for 019996', 'Details for 019996', '019996',   '[]', 1,    10,   '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED',         'BOOK',  null,        null,     13, '',                  '', '[]'),
       (18, 'Title for 019997', 'Details for 019997', '019997',   '[]', 1,    10,   '2021-01-27', '2021-02-27', '2021-01-27', 'PENDING_EXPORT',   'BOOK',  null,        null,     13, '',                  '', '[]'),
       (19, 'Title for 019998', 'Details for 019998', '019998',   '[]', 1,    10,   '2021-01-27', '2021-02-27', '2021-01-27', 'EXPORTED',         'BOOK',  null,        null,     13, '',                  '', '[]'),
       (20, 'Title for 100000', 'Details for 100000', '100000',   '[]', 1,    10,   '2021-01-09', '2021-02-09', '2021-01-10', 'EXPORTED',         'BOOK',  'BKM202104', '202104', 13, 'Author for 100000', 'Publisher for 100000', '["BKM202110", "BKX202107"]'),
       (21, 'Title for 100003', 'Details for 100003', '100003',   '[]', 1,    NULL, '2021-01-09', '2021-02-09', '2021-01-10', 'EXPORTED',         'BOOK',  NULL,        '202104', 13, NULL,                 NULL, '[]'),
       (22, 'Title for 100004', 'Details for 100004', '100004',   '[]', 1,    NULL, '2021-01-09', '2021-02-09', '2021-01-10', 'EXPORTED',         'BOOK',  NULL,        '202104', 13, NULL,                 NULL, '[]'),
       (23, 'Title for 100005', 'Details for 100005', '48959938', '[]', 1,    10,   '2021-03-08', '2021-04-08', '2021-03-08', 'PENDING_EXTERNAL', 'BOOK',  '202108',    null,     1,  null,                 null, '[]'),
       (24, 'Title for 100006', 'Details for 100006', '100006',   '[]', 1,    10,   '2021-01-09', '2021-02-09', '2021-01-10', 'ASSIGNED',         'BOOK',  'BKM202104', '202104', 13, 'Author for 100000', 'Publisher for 100006', '["BKX202107"]'),
       (25, 'Title for 100007', 'Details for 100007', '100007',   '[]', 1,    10,   '2021-01-27', '2021-02-27', '2021-01-27', 'PENDING_EXPORT',   'BOOK',  null,        null,     13, '',                  '', '[]'),
       (26, 'Title for 100008', 'Details for 100008', '100008',   '[]', 1,    10,   '2021-01-27', '2021-02-27', '2021-01-27', 'PENDING_EXPORT',   'BOOK',  null,        null,     13, '',                  '', '[]'),
       (27, 'Title for 100009', 'Details for 100009', '100009',   '[]', 1,    10, '  2021-01-27', '2021-02-27', '2021-01-27', 'PENDING_EXPORT',   'BOOK',  null,        null,     13, '',                  '', '[]');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '202-12-10', 'GROUP_1_LESS_THAN_100_PAGES', '2020-12-10',  NULL,         NULL,                           '["001111", "002222", "003333"]'),
        (2,  'GROUP_2_100_UPTO_199_PAGES',  'DESCRIPTION',    '2020-12-10', 'GROUP_2_100_UPTO_199_PAGES',  NULL,         '2020-12-10', NULL,                           '["001111", "002222", "003333"]'),
        (3,  'GROUP_3_200_UPTO_499_PAGES',  'DESCRIPTION',    '2020-12-10', 'GROUP_3_200_UPTO_499_PAGES',  NULL,         NULL,         'here is some data',            '["001111", "002222", "003333"]'),
        (4,  'GROUP_4_500_OR_MORE_PAGES',   'DESCRIPTION',    '2020-12-10', 'GROUP_4_500_OR_MORE_PAGES',   NULL,         NULL,         '',                             '["001111", "002222", "003333"]'),
        (5,  'MOVIES_GR_1',                 'DESCRIPTION',    '2020-12-10', 'MOVIES_GR_1',                 NULL,         NULL,         NULL,                           '["001111", "002222", "003333"]'),
        (6,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',            '["019994"]'),
        (7,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data',       '["019994"]'),
        (8,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data',       '["019994"]'),
        (9,  'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         'here is unused data',          '["019994"]'),
        (10, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',            '["019995"]'),
        (11, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data',       '["019995"]'),
        (12, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-01-27', 'METAKOMPAS',                  '2021-01-27', NULL,         'here is unused data',          '["019995"]'),
        (13, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',            '["019996"]'),
        (14, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data',       '["019996"]'),
        (15, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', 'BRIEF TEXT FOR 100000 ÆØÅ',    '["100000"]'),
        (16, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', 'BRIEF TEXT FOR 100001 æøå',    '["100001"]'),
        (17, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', 'BRIEF TEXT FOR 100002 éö^',    '["100002"]'),
        (18, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',            '2021-01-27', 'BKM',                         '2021-02-08', '2021-02-09', 'Bogen findes relevant for biblioteker på Christiansø', '["100000", "100001", "100002"]'),
        (19, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'DESCRIPTION TEXT',             '["100000", "100001", "100002"]'),
        (20, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'EVALUATION TEXT',              '["100000", "100001", "100002"]'),
        (21, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'TEXT BEFORE <t>48611435 Jeg tæller mine skridt<t> TEXT BETWEEN <t>48611435 Hest horse Pferd cheval love<t> TEXT AFTER', '["100000", "100001", "100002"]'),
        (22, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'RECOMMENDATION TEXT',          '["100000", "100001", "100002"]'),
        (23, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'TOPIC_1, TOP IC_2, TOPIC_3,',  '["100000"]'),
        (24, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'TOPIC_1,TOP IC_2,,TOPIC_4',    '["100001"]'),
        (25, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'TOP IC_2; ; TOPIC_3; TOPIC_5', '["100002"]'),
        (26, 'GROUP_1_LESS_THAN_100_PAGES', 'AGE',            '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'AGE TEXT',                     '["100000", "100001", "100002"]'),
        (27, 'GROUP_1_LESS_THAN_100_PAGES', 'MATLEVEL',       '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'MATLEVEL;;TEXT',               '["100000", "100001", "100002"]'),
        (28, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', null,                           '["100003"]'),
        (29, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', null,                           '["100003"]'),
        (30, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', null,                           '["100003"]'),
        (31, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', NULL,                           '["100003"]'),
        (32, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', NULL,                           '["100003"]'),
        (33, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', null,                           '["100004"]'),
        (34, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', null,                           '["100004"]'),
        (35, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', null,                           '["100004"]'),
        (36, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', NULL,                           '["100004"]'),
        (37, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', NULL,                           '["100004"]'),
        (38, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-03-08', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         '2021-02-09', NULL,                           '["48959911", "48959954"]'),
        (39, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',     '2021-03-08', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         '2021-02-09', NULL,                           '["48959938"]'),
        (40, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', '2021-02-09', 'BRIEF TEXT FOR 100000 ÆØÅ',    '["100006"]'),
        (41, 'GROUP_1_LESS_THAN_100_PAGES', 'BKM',            '2021-01-27', 'BKM',                         '2021-02-08', '2021-02-09', 'Bogen findes relevant for biblioteker på Christiansø', '["100006"]'),
        (42, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'DESCRIPTION TEXT',             '["100006"]'),
        (43, 'GROUP_1_LESS_THAN_100_PAGES', 'EVALUATION',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'EVALUATION TEXT',              '["100006"]'),
        (44, 'GROUP_1_LESS_THAN_100_PAGES', 'COMPARISON',     '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'TEXT BEFORE <t>48611435 Jeg tæller mine skridt<t> TEXT BETWEEN <t>48611435 Hest horse Pferd cheval love<t> TEXT AFTER', '["100006"]'),
        (45, 'GROUP_1_LESS_THAN_100_PAGES', 'RECOMMENDATION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'RECOMMENDATION TEXT',          '["100006"]'),
        (46, 'GROUP_1_LESS_THAN_100_PAGES', 'TOPICS',         '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'TOPIC_1, TOPIC_2, TOPIC_3',    '["100006"]'),
        (47, 'GROUP_1_LESS_THAN_100_PAGES', 'AGE',            '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'AGE TEXT',                     '["100006"]'),
        (48, 'GROUP_1_LESS_THAN_100_PAGES', 'MATLEVEL',       '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', '2021-02-09', 'MATLEVEL;TEXT',                '["100006"]'),
        (53, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '202-12-10', 'GROUP_1_LESS_THAN_100_PAGES', '2020-12-10',  NULL,         NULL,                          '["004444", "005555", "006666"]'),
        (54, 'GROUP_2_100_UPTO_199_PAGES',  'DESCRIPTION',    '2020-12-10', 'GROUP_2_100_UPTO_199_PAGES',  NULL,         '2020-12-10', NULL,                          '["004444", "005555", "006666"]'),
        (55, 'GROUP_3_200_UPTO_499_PAGES',  'DESCRIPTION',    '2020-12-10', 'GROUP_3_200_UPTO_499_PAGES',  NULL,         NULL,         'here is some data',           '["004444", "005555", "006666"]'),
        (56, 'GROUP_4_500_OR_MORE_PAGES',   'DESCRIPTION',    '2020-12-10', 'GROUP_4_500_OR_MORE_PAGES',   NULL,         NULL,         '',                            '["004444", "005555", "006666"]'),
        (57, 'MOVIES_GR_1',                 'DESCRIPTION',    '2020-12-10', 'MOVIES_GR_1',                 NULL,         NULL,         NULL,                          '["004444", "005555", "006666"]');

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts, recordId)
values  (49, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', NULL, 'Ready to export, no faust', '["019997"]', NULL),
        (50, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',          '2021-01-27', 'BRIEF',                       '2021-02-08', NULL, 'Ready to export, with faust', '["100007"]', '123456789'),
        (51, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', NULL, 'Ready to export, no faust, not BRIEF', '["100008"]', NULL),
        (52, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION',    '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', '2021-02-08', NULL, 'Ready to export, with faust, not BRIEF', '["100009"]', '987654321');

insert into casetasks(case_id, task_id)
values (1, 1),
       (1, 2),
       (1, 3),
       (1, 4),
       (1, 5),
       (15, 6),
       (15, 7),
       (15, 8),
       (15, 9),
       (16, 10),
       (16, 11),
       (16, 12),
       (17, 13),
       (17, 14),
       (20, 15),
       (20, 16),
       (20, 17),
       (20, 18),
       (20, 19),
       (20, 20),
       (20, 21),
       (20, 22),
       (20, 23),
       (20, 24),
       (20, 25),
       (20, 26),
       (20, 27),
       (21, 28),
       (21, 29),
       (21, 30),
       (21, 31),
       (21, 32),
       (22, 33),
       (22, 34),
       (22, 35),
       (22, 36),
       (22, 37),
       (23, 38),
       (23, 39),
       (24, 40),
       (24, 41),
       (24, 42),
       (24, 43),
       (24, 44),
       (24, 45),
       (24, 46),
       (24, 47),
       (24, 48),
       (18, 49),
       (25, 50),
       (26, 51),
       (27, 52),
       (2, 53),
       (2, 54),
       (2, 55),
       (2, 56),
       (2, 57);
