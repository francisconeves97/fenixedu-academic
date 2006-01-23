alter table CMS_MAILING_LIST drop index KEY_READERS_GROUP;
alter table CMS_MAILING_LIST drop index KEY_WRITERS_GROUP;
ALTER TABLE CMS_MAILING_LIST
	CHANGE COLUMN KEY_READERS_GROUP READERS_GROUP BLOB DEFAULT NULL,
	CHANGE COLUMN KEY_WRITERS_GROUP WRITERS_GROUP BLOB DEFAULT NULL,
	ADD COLUMN MEMBERS_GROUP BLOB DEFAULT NULL;

alter table CMS_MAIL_MESSAGE drop index KEY_READERS_GROUP;
alter table CMS_MAIL_MESSAGE drop index KEY_WRITERS_GROUP;
ALTER TABLE CMS_MAIL_MESSAGE
	CHANGE COLUMN KEY_READERS_GROUP READERS_GROUP BLOB DEFAULT NULL,
	CHANGE COLUMN KEY_WRITERS_GROUP WRITERS_GROUP BLOB DEFAULT NULL;

alter table CMS_MAIL_CONVERSATION drop index KEY_READERS_GROUP;
alter table CMS_MAIL_CONVERSATION drop index KEY_WRITERS_GROUP;
ALTER TABLE CMS_MAIL_CONVERSATION
	CHANGE COLUMN KEY_READERS_GROUP READERS_GROUP BLOB DEFAULT NULL,
	CHANGE COLUMN KEY_WRITERS_GROUP WRITERS_GROUP BLOB DEFAULT NULL;

alter table CMS_EXECUTION_COURSE_WEBSITE drop index KEY_READERS_GROUP;
alter table CMS_EXECUTION_COURSE_WEBSITE drop index KEY_WRITERS_GROUP;
ALTER TABLE CMS_EXECUTION_COURSE_WEBSITE
	CHANGE COLUMN KEY_READERS_GROUP READERS_GROUP BLOB DEFAULT NULL,
	CHANGE COLUMN KEY_WRITERS_GROUP WRITERS_GROUP BLOB DEFAULT NULL;

alter table DEGREE_CURRICULAR_PLAN drop index KEY_DEGREE_CURRICULAR_PLAN_MEMBERS_GROUP;
ALTER TABLE DEGREE_CURRICULAR_PLAN
	CHANGE COLUMN KEY_DEGREE_CURRICULAR_PLAN_MEMBERS_GROUP DEGREE_CURRICULAR_PLAN_MEMBERS_GROUP BLOB DEFAULT NULL;

alter table DEPARTMENT drop index KEY_COMPETENCE_COURSE_MEMBERS_GROUP;
ALTER TABLE DEPARTMENT
	CHANGE COLUMN KEY_COMPETENCE_COURSE_MEMBERS_GROUP COMPETENCE_COURSE_MEMBERS_GROUP BLOB DEFAULT NULL;
