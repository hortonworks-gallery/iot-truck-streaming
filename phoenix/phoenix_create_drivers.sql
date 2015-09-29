drop table if exists drivers;

create table drivers (
 driverid bigint not null,
 name varchar(50),
 certified char(1),
 wage_plan varchar(50),
 CONSTRAINT pk PRIMARY KEY (driverid));

