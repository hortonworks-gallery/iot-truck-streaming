drop table if exists timesheet;

create table timesheet (
 driverid bigint not null,
 week bigint not null,
 hours_logged bigint,
 miles_logged bigint,
 CONSTRAINT pk PRIMARY KEY (driverid,week));
