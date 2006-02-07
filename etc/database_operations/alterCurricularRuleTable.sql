ALTER TABLE CURRICULAR_RULE DROP COLUMN MINIMUM;
ALTER TABLE CURRICULAR_RULE DROP COLUMN MAXIMUM;

ALTER TABLE CURRICULAR_RULE ADD COLUMN MINIMUM_CREDITS double default 0;
ALTER TABLE CURRICULAR_RULE ADD COLUMN MAXIMUM_CREDITS double default 0;

ALTER TABLE CURRICULAR_RULE ADD COLUMN MINIMUM_LIMIT int default 0;
ALTER TABLE CURRICULAR_RULE ADD COLUMN MAXIMUM_LIMIT int default 0;
