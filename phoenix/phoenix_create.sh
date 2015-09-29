#!/bin/bash

#TODO: Phoenix version dependency is baked into paths here.

# Note if installed by the script the path will be: /root/phoenix-4.1.0-bin/hadoop2/bin/psql.py'
# Otherwise it's /usr/hdp/current/phoenix-client/bin/psql.py
# So let's test and use the one we have!
if [ -f /usr/hdp/current/phoenix-client/bin/psql.py ]
 	then
 		psql_script=/usr/hdp/current/phoenix-client/bin/psql.py
    	echo "** Using psql script found at: /usr/hdp/current/phoenix-client/bin"

	elif [ -f /root/phoenix-4.1.0-bin/hadoop2/bin/psql.py ]
	    then
		    psql_script=/root/phoenix-4.1.0-bin/hadoop2/bin/psql.py
		    echo "** Using psql script found at: /root/phoenix-4.1.0-bin/hadoop2/bin/psql.py"
    else
        echo "** Error: psql script could not be found. Unable to create phoenix tables! **"
fi

# Phoenix cmd line requires jdk 1.7.X, ensuring default jdk is correct
rm /var/lib/alternatives/java
/usr/sbin/alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 1

# Run the scripts
$psql_script localhost:2181:/hbase-unsecure ./phoenix/phoenix_create_drivers.sql ./drivers.csv
$psql_script localhost:2181:/hbase-unsecure ./phoenix/phoenix_create_timesheet.sql ./timesheet.csv

