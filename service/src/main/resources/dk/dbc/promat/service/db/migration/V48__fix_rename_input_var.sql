
-- Helper function to call jsonb_contains on jsonb data from
-- within the javacode using CriterianBuilder which does
-- not (at time of writing) supports json data types
-- Fix: "json" is now a keyword in postgres 17
DROP FUNCTION jsonbcontainsfromstring(jsonb,text);
CREATE OR REPLACE FUNCTION JsonbContainsFromString(
    json_data JSONB,
    string text)
    RETURNS bool AS $$
BEGIN
RETURN json_data ? string;
END
$$
LANGUAGE plpgsql;