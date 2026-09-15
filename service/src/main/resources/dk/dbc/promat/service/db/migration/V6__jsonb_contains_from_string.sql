-- Helper function to call jsonb_contains on jsonb data from
-- within the javacode using CriterianBuilder which does
-- not (at time of writing) supports json data types
--
-- Parameter renamed json -> jsondata: Postgres 17 rejects a parameter literally
-- named `json` followed by a type, which breaks a fresh database's bootstrap at
-- this migration. Behavior is unchanged.
--
-- This edits an already-applied migration - run `flyway repair` wherever V6 has
-- already run, before the next deploy, or Flyway will fail on a checksum mismatch.
CREATE OR REPLACE FUNCTION JsonbContainsFromString(
    jsondata JSONB,
    string text)
    RETURNS bool AS $$
BEGIN
    RETURN jsondata ? string;
END
$$
LANGUAGE plpgsql;
