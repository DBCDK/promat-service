-- Helper function to call jsonb_contains on jsonb data from
-- within the javacode using CriterianBuilder which does
-- not (at time of writing) supports json data types
CREATE OR REPLACE FUNCTION JsonbContainsFromString(
    json JSONB,
    string text)
    RETURNS bool AS $$
BEGIN
    RETURN json ? string;
END
$$
LANGUAGE plpgsql;
