#!/bin/bash

source setup/bin/ambari_util.sh

# stop_demo will kill the storm topology and stop ActiveMQ
setup/bin/stop_demo.sh

echo "*** Stopping Storm..."
stop STORM
# the stop call should wait, but seems like a little more time is needed
sleep 60

# create a list of storm supervisor nodes from config properties, read and assign to an array
s=`grep supervisors= config.properties | cut -d= -f3`
IFS=','
read -a ssarray <<< "${s}"

# Clean up Storm processes (local and remote)
echo "*** Cleaning up left over Storm processes..."
ps -u storm | grep -v grep | awk '{if(NR>1)print "kill -9 " $1}'

for node in "${ssarray[@]}"
do
   ssh -o StrictHostKeyChecking=no root@$node "ps -u storm | grep -v grep | awk '{if(NR>1)print \"kill -9 \" \$1}' | sh"
done

# This was deemed potentially unsafe
#/usr/hdp/2.*/zookeeper/bin/zkCli.sh -cmd rmr /storm

# Clean up Storm Directories (local and remote)
echo "*** Cleaning up Storm directories..."
setup/bin/cleanupstormdirs.sh

for node in "${ssarray[@]}"
do
   cat setup/bin/cleanupstormdirs.sh | ssh -o StrictHostKeyChecking=no root@$node /bin/bash
done


# Cleanup HDFS directories used by demo
echo "*** Cleaning up HDFS directories..."
su - hdfs -c "hdfs dfs -rm -r /tmp/predictions/"
su - hdfs -c "hdfs dfs -mkdir /tmp/predictions"
su - hdfs -c "hdfs dfs -chown storm:hdfs /tmp/predictions"


echo ""
echo "*** Starting Storm....can take 2-4 minutes"

started=0
tries=0
while [ $started -ne "1" -a $tries -lt 3 ]
do
  start STORM
  sleep 120

  started=$(check STORM "STARTED")
  tries=$((tries+1))
done

if [ $tries -ge 3 ]
then
   echo ""
   echo "*** ERROR! Couldn't start Storm, exiting......"
   exit 1
fi


echo "*** stopping Falcon"
stop FALCON

echo "*** starting Kafka"
start KAFKA

echo "*** Clean up complete.."
