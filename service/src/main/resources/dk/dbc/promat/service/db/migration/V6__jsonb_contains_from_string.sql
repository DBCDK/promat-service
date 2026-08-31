-- Helper function to call jsonb_contains on jsonb data from
-- within the javacode using CriterianBuilder which does
-- not (at time of writing) supports json data types
--
-- NOTE: the "json" parameter name was renamed to "jsondata" here because
-- Postgres 17 (the real target version, per staging/prod - "v17" in their
-- DB hostnames) rejects "json JSONB" as a parameter declaration with a
-- syntax error; a parameter literally named "json" immediately followed by
-- a type name is no longer valid there. This IS an edit to an
-- already-applied migration - anywhere V6 previously ran (staging/prod)
-- must have `flyway repair` run against it before the next deploy, or
-- validation will fail on a checksum mismatch. The function's behavior is
-- unchanged, so repair (which only reconciles the recorded checksum,
-- without re-running the migration) is safe here.
CREATE OR REPLACE FUNCTION JsonbContainsFromString(
    jsondata JSONB,
    string text)
    RETURNS bool AS $$
BEGIN
    RETURN jsondata ? string;
END
$$
LANGUAGE plpgsql;
