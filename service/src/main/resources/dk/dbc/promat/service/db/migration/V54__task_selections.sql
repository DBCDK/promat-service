-- Structured storage of what a reviewer actually selected from the Metakompas taxonomy tree
-- and the Buggi tag vocabulary, tied to a specific task + target faust. Replaces the previous
-- approach of stashing Buggi tags as one opaque string on promattask.data (which couldn't
-- represent independent selections for a task with more than one target faust) and adds
-- persistence for Metakompas selections, which previously had none at all.
CREATE TABLE metakompas_selection
(
    task_id    integer NOT NULL REFERENCES promattask (id) ON DELETE CASCADE,
    faust      text NOT NULL,
    data       text NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (task_id, faust)
);

CREATE TABLE buggi_selection
(
    task_id    integer NOT NULL REFERENCES promattask (id) ON DELETE CASCADE,
    faust      text NOT NULL,
    data       text NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (task_id, faust)
);
