-- Backs the new isbn/dk5/extent/materialTypes/series fields on PromatCase
-- (model/.../persistence/PromatCase.java), populated from fbi-api by
-- CaseInformationUpdater. All nullable with no default, so this is a cheap,
-- fast ALTER TABLE on Postgres - existing rows just get NULL for these
-- columns until the next case-information update fills them in.
ALTER TABLE promatcase ADD COLUMN isbn jsonb DEFAULT NULL;
ALTER TABLE promatcase ADD COLUMN dk5 jsonb DEFAULT NULL;
ALTER TABLE promatcase ADD COLUMN extent TEXT DEFAULT NULL;
ALTER TABLE promatcase ADD COLUMN materialtypes jsonb DEFAULT NULL;
ALTER TABLE promatcase ADD COLUMN series jsonb DEFAULT NULL;
