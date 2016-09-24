#!/bin/bash

source setup/bin/ambari_util.sh

echo '===='
echo 'Setting up demo requirements ...'
echo '===='

echo '*** Making HDFS directories...'
runuser -l hdfs -c 'hadoop fs -mkdir -p /truck-events-v4/staging'
runuser -l hdfs -c 'hadoop fs -chown -R storm /truck-events-v4'

runuser -l hdfs -c 'hadoop fs -mkdir /user/root'
runuser -l hdfs -c 'hadoop fs -chown root /user/root'

chmod -R 777 /tmp/hive


echo '*** Starting ActiveMQ...'
/opt/activemq/latest/bin/activemq start xbean:file:/opt/activemq/latest/conf/activemq.xml

echo '*** Importing hbase-site.xml into storm topology...'
cp /etc/hbase/conf/hbase-site.xml storm-streaming/src/main/resources

echo '*** Exporting demo configuration...'
mkdir -p /etc/storm_demo
cp config.properties /etc/storm_demo

echo '*** Stopping Falcon...'
stop FALCON

echo '*** Starting kafka....'
startWait KAFKA

echo '*** Starting HBase....'
startWait HBASE

# Note that auto-create topics should be on by default, but we explicitly create them here anyway
echo '*** Creating truck_events topic in Kafka...'
if [ -f /usr/hdp/current/kafka-broker/bin/kafka-topics.sh ]
then
  /usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper $ZK_HOST:2181 --replication-factor 1 --partitions 2 --topic truck_events
else
  echo "Skipping creation of truck_events topic in kafka as kafka-topics.sh not found on this host. You can create this manually from the host where kafka is installed"
fi

echo '*** Creating and populating Phoenix tables to store driver and timesheet data'
. ./phoenix/phoenix_create.sh

echo '*** Creating HBase and Hive Tables to store events...'
setup/bin/ddl_config.sh


echo '*** Creating default spark ML weights for prediction bolt to consume'
hdfs dfs -mkdir -p /tmp/sparkML_weights
hdfs dfs -put ./truckml/lrweights /tmp/sparkML_weights/lrweights

echo '*** Creating default PMML for JPMML prediction bolt to consume'
hdfs dfs -mkdir -p /tmp/pmml
hdfs dfs -put ./truckml/TruckDemoModel-pmml.xml /tmp/pmml/TruckDemoModel-pmml.xml

#Detect which pom file to use based on HDP version being run on
HDP_VERSION=`hdp-select status hadoop-client | sed 's/hadoop-client - \([0-9]\.[0-9]\).*/\1/'`
if [ $HDP_VERSION == "2.5" ]
then
	echo "Detected 2.5, copying pom25.xml"
	mv -f storm-streaming/pom25.xml storm-streaming/pom.xml
	mv -f storm-demo-webapp/pom25.xml storm-demo-webapp/pom.xml
elif [ $HDP_VERSION == "2.4" ]
then
	echo "Detected 2.4, copying pom24.xml"
	mv -f storm-streaming/pom24.xml storm-streaming/pom.xml
elif [ $HDP_VERSION == "2.3" ]
then
	echo "Detected 2.3, copying pom23.xml"
	mv -f storm-streaming/pom23.xml storm-streaming/pom.xml
elif [ $HDP_VERSION == "2.2" ]
then
	echo "Detected 2.2, copying pom22.xml"
	mv -f storm-streaming/pom22.xml storm-streaming/pom.xml	
else
	echo "Unrecognized HDP version: $HDP_VERSION - this repo currently only supports 2.2.x and 2.3.x and 2.4.x"	
	exit 1
fi

echo '*** Building and installing demo modules (may take a few minutes)...'
/root/maven/bin/mvn clean install -DskipTests=true
