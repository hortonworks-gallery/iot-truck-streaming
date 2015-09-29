#!/bin/bash

#TODO would be nice if this would kill the webapp too in case you nohup it

echo '*** Killing the storm topology...'
storm kill truck-event-processor

# Stop ActiveMQ
echo '*** Stopping ActiveMQ...'
/opt/activemq/latest/bin/activemq stop
