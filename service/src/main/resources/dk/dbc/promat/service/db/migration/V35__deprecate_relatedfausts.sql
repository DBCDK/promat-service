DROP FUNCTION IF EXISTS checknoopencasewithfaust(faust text, caseid integer);

CREATE OR REPLACE FUNCTION CheckNoOpenCaseWithFaust(
    faust text,
    caseid integer default NULL)
    RETURNS bool AS $$
BEGIN
    RETURN NOT EXISTS (
            SELECT    *
            FROM      promatcase c
            JOIN casetasks ct
                   ON      c.id = ct.case_id
            JOIN promattask t
                   ON      t.id = ct.task_id
            WHERE     (
                          primaryFaust = faust
                        OR targetfausts ? faust
                      )
              AND     status NOT IN ('CLOSED', 'DONE', 'DELETED')
              AND     (caseid is NULL OR c.id <> caseid));
END
$$
    LANGUAGE plpgsql;
