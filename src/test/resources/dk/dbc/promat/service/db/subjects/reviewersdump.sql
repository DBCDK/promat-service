insert into subject(id, name, parentid) VALUES (5, 'Multimedie', null);
insert into reviewer(id, firstname, lastname, email, address1, zip, city, institution, paycode) values(1, 'Hans', 'Hansen', 'hans@hansen.dk', 'Lillegade 1', '9999', 'Lilleved', 'Hans Hansens Bix', 0);
insert into reviewersubjects(subject_id, reviewer_id) values(5,1);
insert into reviewersubjects(subject_id, reviewer_id) values(3,1);
