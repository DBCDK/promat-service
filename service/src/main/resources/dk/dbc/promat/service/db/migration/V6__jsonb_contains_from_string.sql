-- Helper function to call jsonb_contains on jsonb data from
-- within the javacode using CriterianBuilder which does
-- not (at time of writing) supports json data types
--
-- Parameter is named `jsondata`, not `json` - Postgres 17 rejects a parameter
-- literally named `json` immediately followed by a type name (e.g. `json JSONB`)
-- as a syntax error. Without this rename, a *fresh* database (no prior migration
-- history) fails to bootstrap here - V48__fix_rename_input_var.sql later
-- DROP/CREATEs this same function under a Postgres-17-safe parameter name, but
-- that fix only helps once Flyway can get past V6 in the first place. V48's
-- DROP FUNCTION matches by type signature, not parameter name, so it still
-- drops/replaces this version cleanly regardless of what it's called here.
--
-- This is an edit to an already-applied migration: anywhere V6 previously ran
-- must have `flyway repair` run against it before the next deploy, or
-- validation will fail on a checksum mismatch. The function's behavior is
-- unchanged, so repair is safe here.
CREATE OR REPLACE FUNCTION JsonbContainsFromString(
    jsondata JSONB,
    string text)
    RETURNS bool AS $$
BEGIN
    RETURN jsondata ? string;
END
$$
LANGUAGE plpgsql;
