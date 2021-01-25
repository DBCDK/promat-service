ALTER TABLE PromatTask RENAME COLUMN paycode TO payCategory;

-- This 'kind-of' fixes now-invalid tasktype values in the task table
-- If we where in production, we had to find the correct tasktype from
-- one of the default tasks on the case, but being on an early prototype,
-- there is no guarantee that we have standard fields for all cases.
-- If something blows up, it must be fixed manually.
UPDATE PromatTask
   SET taskType = 'NONE'
 WHERE taskType = 'BKM';

UPDATE PromatTask
SET taskType = 'NONE'
WHERE taskType = 'METAKOMPAS';
