insert into reviewer(id, firstname, lastname, email, address1, zip, city, institution, paycode, hiatus_begin, hiatus_end) values(1, 'Hans', 'Hansen', 'hans@hansen.dk', 'Lillegade 1', '9999', 'Lilleved', 'Hans Hansens Bix', 0, '2020-10-28', '2020-11-01');
insert into reviewersubjects(subject_id, reviewer_id) values(5,1);
insert into reviewersubjects(subject_id, reviewer_id) values(3,1);
