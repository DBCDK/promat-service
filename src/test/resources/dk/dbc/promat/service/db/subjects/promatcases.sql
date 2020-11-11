-- existing cases
insert into promatcase(id, title, details, primaryFaust, relatedFausts, reviewer_id, created, deadline, assigned, status, materialType)
values (1, 'Title for 001111', 'Details for 001111', '001111', '["002222","003333"]', NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'BOOK'),
       (2, 'Title for 004444', 'Details for 004444', '004444', '["005555","006666"]', NULL, '2020-11-11', '2020-12-11', NULL, 'CREATED', 'MOVIE');
