CREATE TABLE subject
(
    id       integer PRIMARY KEY NOT NULL,
    name     text    UNIQUE NOT NULL,
    parentid integer
);

DROP FUNCTION IF EXISTS subject_check_if_parent_exists();

CREATE FUNCTION subject_check_if_parent_exists()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    IF (new.parentid IS NOT NULL) AND (new.parentid>0) THEN
        IF NOT EXISTS(SELECT s.id FROM subject s WHERE s.id=new.parentid) THEN
            RAISE EXCEPTION 'Unknown parentId:%', new.parentid;
        END IF;
    END IF;
    RETURN new;
END
$$;

DROP FUNCTION IF EXISTS subject_check_that_no_children_exists();

CREATE FUNCTION subject_check_that_no_children_exists()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS(SELECT s.parentid FROM subject s WHERE s.parentid=old.id) THEN
        RAISE EXCEPTION 'Remove child subjects, before deleting subject with id: %', old.id;
    END IF;
    RETURN old;
END
$$;

DROP TRIGGER IF EXISTS subject_insert_trigger ON subject;

CREATE TRIGGER subject_insert_trigger
    BEFORE INSERT
    ON subject
    FOR EACH ROW
    EXECUTE PROCEDURE subject_check_if_parent_exists();

DROP TRIGGER IF EXISTS subject_update_trigger ON subject;

CREATE TRIGGER subject_update_trigger
    BEFORE UPDATE
    ON subject
    FOR EACH ROW
    EXECUTE PROCEDURE subject_check_if_parent_exists();

DROP TRIGGER IF EXISTS subject_delete_trigger ON subject;

CREATE TRIGGER subject_delete_trigger
    BEFORE DELETE
    ON subject
    FOR EACH ROW
    EXECUTE PROCEDURE subject_check_that_no_children_exists();