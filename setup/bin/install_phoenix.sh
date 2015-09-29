#!/bin/bash

source setup/bin/ambari_util.sh

echo '*** Checking to see if Phoenix is already installed...'

# See if we need to install phoenix at all. Ambari 1.7 doesn't install it, but 2.0 probably does.
if [ ! -f /usr/hdp/2.2.*/hbase/lib/phoenix*-server-hadoop2.jar ]
 	then
 		skip_phoenix=false
    	echo "Phoenix JAR file not found. Phoenix will be installed."
	else
		skip_phoenix=true
		echo "Phoenix JAR file was found. Skipping Phoenix install."
fi

if [ "$skip_phoenix" = false ]; then
	echo 'Getting Phoenix binaries....'
	wget http://apache.cs.utah.edu/phoenix/phoenix-4.1.0/bin/phoenix-4.1.0-bin.tar.gz
	mv phoenix-4*bin.tar.gz /tmp

	tar xvzf /tmp/phoenix*-bin.tar.gz -C /root

	echo 'Copying phoenix jar to hbase'
	dn=`grep datanodes= config.properties`
	eval $(echo $dn)
	echo $datanodes
	IFS=',' read -a array <<< "$datanodes"

	# copy phoenix jar to hbase lib folder (locally)
	cp /root/phoenix-4*/hadoop2/phoenix-*server-hadoop2.jar /usr/hdp/2.2.*/hbase/lib/

	# copy phoenix jar to hbase lib folder (remotely)
	for node in "${array[@]}"
	do
	   scp -o StrictHostKeyChecking=no /root/phoenix-4*/hadoop2/phoenix-*server-hadoop2.jar root@$node:/usr/hdp/2.2.*/hbase/lib/
	done

	echo '*** Restarting hbase'
	stop HBASE
	startWait HBASE

    echo ''
	echo '*** Phoenix was installed. HBase was restarted. **'
fi
