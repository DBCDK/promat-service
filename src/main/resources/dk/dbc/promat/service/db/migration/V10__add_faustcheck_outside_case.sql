DROP FUNCTION IF EXISTS checknoopencasewithfaust(faust text);

CREATE OR REPLACE FUNCTION CheckNoOpenCaseWithFaust(
    faust text,
    caseid integer default NULL)
    RETURNS bool AS $$
BEGIN
    RETURN NOT EXISTS (
            SELECT *
            FROM promatcase
            WHERE (primaryFaust = faust OR relatedFausts ? faust)
              AND status NOT IN ('CLOSED', 'DONE')
              AND (caseid is NULL OR id <> caseid));
END
$$
    LANGUAGE plpgsql;
