CREATE TABLE promat_case (
  id             INTEGER PRIMARY KEY,
  relatedFausts  JSONB
);


INSERT INTO promat_case VALUES (1, '["1111111", "11112222", "11113333"]');
INSERT INTO promat_case VALUES (2, '["2222222", "11112222"]');
INSERT INTO promat_case VALUES (3, '["3333333"]');