create table `FILE_LOCAL_CONTENT` (`OID` bigint unsigned, `OID_FILE` bigint unsigned, `CONTENT` blob, `PATH` text, `OID_ROOT_DOMAIN_OBJECT` bigint unsigned, `ID_INTERNAL` int(11) NOT NULL auto_increment, primary key (ID_INTERNAL), index (OID), index (OID_ROOT_DOMAIN_OBJECT)) ENGINE=InnoDB, character set latin1;
create table `QUESTION` (`OID` bigint unsigned, `XML_FILE_NAME` text, `OID_METADATA` bigint unsigned, `VISIBILITY` tinyint(1), `XML_FILE` text, `OID_ROOT_DOMAIN_OBJECT` bigint unsigned, `ID_INTERNAL` int(11) NOT NULL auto_increment, primary key (ID_INTERNAL), index (OID), index (OID_METADATA), index (OID_ROOT_DOMAIN_OBJECT)) ENGINE=InnoDB, character set latin1;