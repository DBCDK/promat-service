-- reviewers (IDs 1-9)
insert into promatuser(id, role, active, culrid, firstname, lastname, email, address1, zip, city, institution, paycode, hiatus_begin, hiatus_end, accepts, note, capacity)
values (1, 'REVIEWER', true, '41', 'Hans', 'Hansen', 'hans@hansen.dk', 'Lillegade 1', '9999', 'Lilleved', 'Hans Hansens Bix', 123, '2020-10-28', '2020-11-01', '["MULTIMEDIA", "PS4", "PS5"]', 'note1', 1),
       (2, 'REVIEWER', true, '42', 'Ole', 'Olsen', 'ole@olsen.dk', 'Storegade 99', '1111', 'Storeved', 'Ole Olsens Goodies', 456, '2020-11-28', '2020-12-01', '["MULTIMEDIA", "PS4", "PS5"]', 'note2', 2);

-- These reviewers is used for update tests. Please do not rely on the value of these reviewers in other tests
insert into promatuser(id, role, active, culrid, firstname, lastname, email, address1, zip, city, institution, paycode, hiatus_begin, hiatus_end, accepts, note, phone, capacity)
values (3, 'REVIEWER', true, '43', 'Peter', 'Petersen', 'peter@petersen.dk', 'Mellemgade 50', '5555', 'Mellemved', 'Peter Petersens pedaler', 22, null, null, '["BOOK"]', 'note3', '12345678', 2),
       (5, 'REVIEWER', true, '44', 'Boe', 'Boesen', 'boe@boesen.dk', null, null, null, 'Boe Boesens Bøjler', 23, null, null, '["BOOK"]', 'note5', '9123456789', 2),
-- This reviewer is used for message testing. Hands off.
       (4, 'REVIEWER', true, '55', 'Kirsten', 'Kirstensen', 'kirsten@kirstensen.dk', 'Overgade 50', '5432', 'Overlev', 'Kirstens Bix', 0, '2021-01-11', '2021-01-12', '["BOOK"]', 'note4', '123456789010', 2);
insert into reviewersubjects(subject_id, reviewer_id) values(5,1);
insert into reviewersubjects(subject_id, reviewer_id) values(3,1);
insert into reviewersubjects(subject_id, reviewer_id) values(5,2);
insert into reviewersubjects(subject_id, reviewer_id) values(5,3);
insert into reviewersubjects(subject_id, reviewer_id) values(5,5);
insert into reviewersubjects(subject_id, reviewer_id) values(5,4);

-- editors (IDs 10-19)
insert into promatuser(id, role, active, culrid, firstname, lastname, email, paycode) values(10, 'EDITOR', true, '51', 'Ed', 'Itor', 'ed.itor@dbc.dk', 5678);
insert into promatuser(id, role, active, culrid, firstname, lastname, email, paycode) values(11, 'EDITOR', true, '52', 'Edit', 'Or', 'edit.or@dbc.dk', 1111);
insert into promatuser(id, role, active, culrid, firstname, lastname, email, paycode) values(13, 'EDITOR', true, '54', 'Editte', 'Ore', 'editte.ore@dbc.dk', 2222);

-- This editor is used for update tests. Please do not rely on the value of this editor in other tests
insert into promatuser(id, role, active, culrid, firstname, lastname, email) values(12, 'EDITOR', true, '53', 'Edi', 'tor', 'edi.tor@dbc.dk');
-- This editor is used for message testing. Hands off.
insert into promatuser(id, role, active, culrid, firstname, lastname, email) values(14, 'EDITOR', true, '56', 'E', 'ditor', 'e.ditor@dbc.dk');
