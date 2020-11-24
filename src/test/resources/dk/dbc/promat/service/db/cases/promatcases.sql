-- existing cases
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, editor_id, created, deadline, assigned, status, materialType)
values (1, 'Title for 001111', 'Details for 001111', '001111', '["002222","003333"]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK'),
       (2, 'Title for 004444', 'Details for 004444', '004444', '["005555","006666"]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'MOVIE'),
       (3, 'Title for 011111', 'Details for 011111', '011111', '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK'),
       (4, 'Title for 012222', 'Details for 012222', '012222', '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'DONE', 'BOOK'),
       (5, 'Title for 013333', 'Details for 013333', '013333', '[]', NULL, 10, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK'),
       (6, 'Title for 014444', 'Details for 014444', '014444', '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'ASSIGNED', 'BOOK'),
       (7, 'Title for 015555', 'Details for 015555', '015555', '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK'),
       (8, 'Title for 016666', 'Details for 016666', '016666', '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK'),
       (9, 'Title for 017777', 'Details for 017777', '017777', '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CLOSED', 'BOOK'),
       (10, 'Title for 018888', 'Details for 018888', '018888', '[]', NULL, 11, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK'),
       (11, 'Title for 019999', 'Details for 019999', '019999', '[]', NULL, NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK');
;