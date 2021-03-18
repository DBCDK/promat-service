-- Set a faustnumber on tasks with targetfaust=NULL. This is not
-- a complete solution, but since we are running on testdata and
-- never will have this migration run on production data - it is
-- close enough.
update promattask t
   set targetfausts = (select jsonb_build_array(primaryfaust)
                         from promatcase c
                              join casetasks ct
                                on c.id = ct.case_id
                        where ct.task_id = t.id)
 where targetfausts is null;

alter table promattask alter column targetfausts set not null;
