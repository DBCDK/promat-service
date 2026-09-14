-- Helper function to call jsonb_contains on jsonb data from
-- within the javacode using CriterianBuilder which does
-- not (at time of writing) supports json data types
--
-- Parameter is named `jsondata`, not `json` - Postgres 17 rejects a parameter
-- literally named `json` immediately followed by a type name (e.g. `json JSONB`)
-- as a syntax error. This is an edit to an already-applied migration: anywhere
-- V6 previously ran must have `flyway repair` run against it before the next
-- deploy, or validation will fail on a checksum mismatch. The function's
-- behavior is unchanged, so repair is safe here.
CREATE OR REPLACE FUNCTION JsonbContainsFromString(
    jsondata JSONB,
    string text)
    RETURNS bool AS $$
BEGIN
    RETURN jsondata ? string;
END
$$
LANGUAGE plpgsql;
