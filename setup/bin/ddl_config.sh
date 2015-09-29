#!/bin/bash
echo '** Creating Hive tables...'

runuser -l hdfs -c 'hdfs dfs -mkdir /user/root'
runuser -l hdfs -c 'hdfs dfs -chmod -R 777 /tmp'
hive -f truck_events.ddl

runuser -l hdfs -c 'hdfs dfs -chown -R storm /apps/hive/warehouse/truck_events_text_partition'
runuser -l hdfs -c 'hdfs dfs -chown -R storm /apps/hive/warehouse/truck_events_orc_partition_single'

hive -f truck_drivers.ddl

echo '** Creating HBase tables...'

echo '** Creating driver_dangerous_events table...'
echo "create 'driver_dangerous_events', {NAME=> 'events', VERSIONS=>3}" | hbase shell

echo '** Creating driver_dangerous_events_count table...'
echo "create 'driver_dangerous_events_count', {NAME=> 'counters', VERSIONS=>3}" | hbase shell

echo '** Creating driver_events table...'
echo "create 'driver_events', {NAME=> 'allevents', VERSIONS=>3}" | hbase shell

#TODO: might want to move the ddl and csv files to their own folder
