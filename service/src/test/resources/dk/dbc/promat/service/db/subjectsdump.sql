insert into subject(id, name, parentid) VALUES (1, 'Voksen', null);
insert into subject(id, name, parentid) VALUES (2, 'Roman', 1);
insert into subject(id, name, parentid) VALUES (3, 'Eventyr, fantasy', 2);
insert into subject(id, name, parentid) VALUES (4, 'Digte', 1);
insert into subject(id, name, parentid) VALUES (5, 'Multimedie', null);
insert into subject(id, name, parentid) VALUES (6, '(Et Mulitmedie underemne)', 5);

insert into subjectnote(id, subject_id, note)
VALUES (1, 5, 'Anmelder det meste'),
       (2, 6, '(Hvis et eller andet underemne, så kommentar)');