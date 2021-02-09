-- existing cases
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType, weekcode, trimmedWeekcode, creator_id)
values (1, 'Title for 001111', 'Details for 001111', '001111', '["002222","003333"]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', 'BKM202101', '202101', 13),
       (2, 'Title for 004444', 'Details for 004444', '004444', '["005555","006666"]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'MOVIE', 'DBF202049', '202049', 13),
       (3, 'Title for 011111', 'Details for 011111', '011111', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13),
       (4, 'Title for 012222', 'Details for 012222', '012222', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'EXPORTED', 'BOOK', null, null, 13),
       (5, 'Title for 013333', 'Details for 013333', '013333', '[]', 1, 10, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13),
       (6, 'Title for 014444', 'Details for 014444', '014444', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'ASSIGNED', 'BOOK', null, null, 13),
       (7, 'Title for 015555', 'Details for 015555', '015555', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13),
       (8, 'Title for 016666', 'Details for 016666', '016666', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13),
       (9, 'Title for 017777', 'Details for 017777', '017777', '[]', 1, NULL, '2020-11-11', '2020-12-11', NULL, 'CLOSED', 'BOOK', null, null, 13),
       (10, 'Title for 018888', 'Details for 018888', '018888', '[]', NULL, 11, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13),
       (11, 'Title for 019999', 'Details for 019999', '019999', '[]', 2, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK', null, null, 13),
       (12, 'Title for 019991', 'Details for 019991', '019991', '[]', 1, 11, '2020-11-11', '2020-12-14', NULL, 'CREATED', 'BOOK', null, null, 13),
       (13, 'Title for 019992', 'Details for 019992', '019992', '[]', 1, 11, '2021-01-11', '2021-01-11', NULL, 'CREATED', 'BOOK', null, null, 13),
       (14, 'Title for 019993', 'Details for 019993', '019993', '[]', 1, 10, '2021-01-13', '2021-01-13', NULL, 'CREATED', 'BOOK', null, null, 13),
       (15, 'Title for 019994', 'Details for 019994', '019994', '[]', 1, 10, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 13),
       (16, 'Title for 019995', 'Details for 019995', '019995', '[]', 1, 10, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 13),
       (17, 'Title for 019996', 'Details for 019996', '019996', '[]', 1, 10, '2021-01-27', '2021-02-27', '2021-01-27', 'ASSIGNED', 'BOOK', null, null, 13),
       (18, 'Title for 019997', 'Details for 019997', '019997', '[]', 1, 10, '2021-01-27', '2021-02-27', '2021-01-27', 'PENDING_EXPORT', 'BOOK', null, null, 13),
       (19, 'Title for 019998', 'Details for 019998', '019998', '[]', 1, 10, '2021-01-27', '2021-02-27', '2021-01-27', 'EXPORTED', 'BOOK', null, null, 13);

insert into promattask(id, tasktype, taskfieldtype, created, paycategory, approved, payed, data, targetFausts)
values  (1,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '202-12-10', 'GROUP_1_LESS_THAN_100_PAGES', '2020-12-10',  NULL,         NULL,                     NULL),
        (2,  'GROUP_2_100_UPTO_199_PAGES',  'DESCRIPTION', '2020-12-10', 'GROUP_2_100_UPTO_199_PAGES',  NULL,         '2020-12-10', NULL,                     NULL),
        (3,  'GROUP_3_200_UPTO_499_PAGES',  'DESCRIPTION', '2020-12-10', 'GROUP_3_200_UPTO_499_PAGES',  NULL,         NULL,         'here is some data',      NULL),
        (4,  'GROUP_4_500_OR_MORE_PAGES',   'DESCRIPTION', '2020-12-10', 'GROUP_4_500_OR_MORE_PAGES',   NULL,         NULL,         '',                       NULL),
        (5,  'MOVIES_GR_1',                 'DESCRIPTION', '2020-12-10', 'MOVIES_GR_1',                 NULL,         NULL,         NULL,                     NULL),
        (6,  'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
        (7,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL),
        (8,  'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', '["019994"]'),
        (9,  'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',  '2021-01-27', 'METAKOMPAS',                  NULL,         NULL,         'here is unused data',    NULL),
        (10, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
        (11, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL),
        (12, 'GROUP_1_LESS_THAN_100_PAGES', 'METAKOMPAS',  '2021-01-27', 'METAKOMPAS',                  '2021-01-27', NULL,         'here is unused data',    NULL),
        (13, 'GROUP_1_LESS_THAN_100_PAGES', 'DESCRIPTION', '2021-01-27', 'GROUP_1_LESS_THAN_100_PAGES', NULL,         NULL,         'here is some data',      NULL),
        (14, 'GROUP_1_LESS_THAN_100_PAGES', 'BRIEF',       '2021-01-27', 'BRIEF',                       NULL,         NULL,         'here is also some data', NULL);


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
       (17, 14);
