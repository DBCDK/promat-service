-- Helper function to call jsonb_contains on jsonb data from
-- within the javacode using CriterianBuilder which does
-- not (at time of writing) supports json data types
--
-- Parameter renamed json -> json_data (same name V48 later renames it to anyway):
-- Postgres 17 rejects a parameter literally named `json` followed by a type,
-- which breaks a fresh database's bootstrap at this migration. Behavior is
-- unchanged - V48's DROP FUNCTION + CREATE OR REPLACE becomes a no-op for any
-- database that runs this version of V6.
--
-- This edits an already-applied migration - run `flyway repair` wherever V6 has
-- already run (staging, prod, any existing local/dev database), before the next
-- deploy, or Flyway will fail on a checksum mismatch.
CREATE OR REPLACE FUNCTION JsonbContainsFromString(
    json_data JSONB,
    string text)
    RETURNS bool AS $$
BEGIN
    RETURN json_data ? string;
END
$$
LANGUAGE plpgsql;
