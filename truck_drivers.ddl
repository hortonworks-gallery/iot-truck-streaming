CREATE TABLE `drivers`(
  `driverid` bigint,
  `name` string,
  `certified` string,
  `wage_plan` string)
ROW FORMAT DELIMITED
  FIELDS TERMINATED BY ',';

LOAD DATA LOCAL INPATH './drivers.csv' OVERWRITE INTO TABLE drivers;

CREATE TABLE `timesheet`(
  `driverid` bigint,
  `week` bigint,
  `hours_logged` bigint,
  `miles_logged` bigint)
ROW FORMAT DELIMITED
  FIELDS TERMINATED BY ',';

LOAD DATA LOCAL INPATH './timesheet.csv' OVERWRITE INTO TABLE timesheet;
