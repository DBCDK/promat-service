-- Structured storage of what a reviewer actually selected from the Metakompas taxonomy tree
-- and the Buggi tag vocabulary, tied to a specific task + target faust. Keyed by (task_id,
-- faust), since a task can target multiple fausts and each needs its own independent selection.
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
