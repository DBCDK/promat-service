-- Revert of V49, which added these fields to persist more bibliographic data
-- on the case. Turns out they're not used anywhere - the same data is
-- already available live via /v1/api/records/{id} (and /records/faust/
-- {faust}), so this reverts to not persisting them. author and publisher
-- are unaffected, since case search/listing still filters and sorts on them.
ALTER TABLE promatcase DROP COLUMN isbn;
ALTER TABLE promatcase DROP COLUMN dk5;
ALTER TABLE promatcase DROP COLUMN extent;
ALTER TABLE promatcase DROP COLUMN materialtypes;
ALTER TABLE promatcase DROP COLUMN series;
