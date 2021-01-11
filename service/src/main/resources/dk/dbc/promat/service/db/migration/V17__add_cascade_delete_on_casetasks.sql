ALTER TABLE casetasks
    DROP CONSTRAINT casetasks_task_id_fkey,
    ADD CONSTRAINT casetasks_task_id_fkey FOREIGN KEY (task_id) REFERENCES promattask(id) ON DELETE CASCADE;
