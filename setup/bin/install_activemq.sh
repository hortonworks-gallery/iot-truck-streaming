#!/bin/bash

mkdir /opt/activemq
cd /opt/activemq
wget https://repository.apache.org/content/repositories/snapshots/org/apache/activemq/apache-activemq/5.9-SNAPSHOT/apache-activemq-5.9-20131010.203434-114-bin.tar.gz
tar xvzf apache-activemq-*.tar.gz
ln -s apache-activemq-5.9-SNAPSHOT latest
cd latest
