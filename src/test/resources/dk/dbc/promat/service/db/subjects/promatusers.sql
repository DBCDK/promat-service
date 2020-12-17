-- reviewers (IDs 1-9)
insert into promatuser(id, role, active, culrid, firstname, lastname, email, address1, zip, city, institution, paycode, hiatus_begin, hiatus_end, accepts, note)
values (1, 'REVIEWER', true, '41', 'Hans', 'Hansen', 'hans@hansen.dk', 'Lillegade 1', '9999', 'Lilleved', 'Hans Hansens Bix', 0, '2020-10-28', '2020-11-01', '["MULTIMEDIA", "PS4", "PS5"]', 'note1'),
       (2, 'REVIEWER', true, '42', 'Ole', 'Olsen', 'ole@olsen.dk', 'Storegade 99', '1111', 'Storeved', 'Ole Olsens Goodies', 0, '2020-11-28', '2020-12-01', '["MULTIMEDIA", "PS4", "PS5"]', 'note2');

-- This reviewer is used for update tests. Please do not rely on the value of this reviewer in other tests
insert into promatuser(id, role, active, culrid, firstname, lastname, email, address1, zip, city, institution, paycode, hiatus_begin, hiatus_end, accepts, note, phone)
values (3, 'REVIEWER', true, '43', 'Peter', 'Petersen', 'peter@petersen.dk', 'Mellemgade 50', '5555', 'Mellemved', 'Peter Petersens pedaler', 22, null, null, '["BOOK"]', 'note3', '12345678');

insert into reviewersubjects(subject_id, reviewer_id) values(5,1);
insert into reviewersubjects(subject_id, reviewer_id) values(3,1);
insert into reviewersubjects(subject_id, reviewer_id) values(5,2);
insert into reviewersubjects(subject_id, reviewer_id) values(5,3);

-- editors (IDs 10-19)
insert into promatuser(id, role, active, culrid, firstname, lastname, email) values(10, 'EDITOR', true, '51', 'Ed', 'Itor', 'ed.itor@dbc.dk');
insert into promatuser(id, role, active, culrid, firstname, lastname, email) values(11, 'EDITOR', true, '52', 'Edit', 'Or', 'edit.or@dbc.dk');
