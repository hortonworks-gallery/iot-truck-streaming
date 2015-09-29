#!/bin/bash

source setup/bin/ambari_util.sh

if [ "$1" = "clean" ]
then
        echo '**** Cleaning up....'
	setup/bin/cleanup.sh
	
	cd storm-streaming
	/root/maven/bin/mvn -DskipTests clean install
	cd ..
fi

echo '*** Re-Importing hbase-site.xml into storm topology...'
cp /etc/hbase/conf/hbase-site.xml storm-streaming/src/main/resources

echo '*** Re-Exporting demo configuration...'
cp ./config.properties /etc/storm_demo

echo 'Ensuring Falcon is stopped...'
stop FALCON

echo 'Ensuring Storm is up...'
startWait STORM

echo 'Deploying the storm topology...'
storm jar storm-streaming/target/storm-streaming-1.0-SNAPSHOT.jar com.hortonworks.streaming.impl.topologies.TruckEventProcessorKafkaTopology /etc/storm_demo/config.properties

echo 'Attempting to start ActiveMQ...'
/opt/activemq/latest/bin/activemq start xbean:file:/opt/activemq/latest/conf/activemq.xml

echo 'Ensuring hbase is up...'
startWait HBASE

echo 'Ensuring KAFKA is up...'
startWait KAFKA

echo 'Starting the demo webapp...'
cd storm-demo-webapp
cp -R routes /etc/storm_demo
/root/maven/bin/mvn -DskipTests clean package
/root/maven/bin/mvn jetty:run -Djetty.port=8081
