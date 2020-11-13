ALTER TABLE cases RENAME TO promatcase;

CREATE OR REPLACE FUNCTION CheckNoOpenCaseWithFaust(
    faust text)
    RETURNS bool AS $$
BEGIN
    RETURN NOT EXISTS (
            SELECT *
            FROM promatcase
            WHERE (primaryFaust = faust OR relatedFausts ? faust)
              AND status NOT IN ('CLOSED', 'DONE'));
END
$$
LANGUAGE plpgsql;

ALTER INDEX cases_primaryfaust_idx RENAME TO promatcase_primaryfaust_idx;
ALTER INDEX cases_relatedfausts_idx RENAME TO promatcase_relatedfausts_idx;
ALTER INDEX cases_pkey RENAME TO promatcase_pkey;

ALTER SEQUENCE cases_id_seq RENAME TO promatcase_id_seq;

ALTER TABLE promatcase RENAME CONSTRAINT cases_primaryfaust_check TO promatcase_primaryfaust_check;
ALTER TABLE promatcase RENAME CONSTRAINT cases_reviewer_id_fkey TO promatcase_reviewer_id_fkey;
