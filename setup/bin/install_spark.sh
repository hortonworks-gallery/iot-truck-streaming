#!/bin/bash

wget http://public-repo-1.hortonworks.com/HDP-LABS/Projects/spark/1.2.0/spark-1.2.0.2.2.0.0-82-bin-2.6.0.2.2.0.0-2041.tgz

mv spark*.tgz /tmp
tar xvzf /tmp/spark*.tgz -C /root
mv /root/spark* /root/spark

echo 'SPARK_HOME=/root/spark' >> ~/.bashrc
echo 'YARN_CONF_DIR=/etc/hadoop/conf' >> ~/.bashrc

echo 'spark.driver.extraJavaOptions -Dhdp.version=2.2.0.0-2041' >> /root/spark/conf/spark-defaults.conf
echo 'spark.yarn.am.extraJavaOptions -Dhdp.version=2.2.0.0-2041' >> /root/spark/conf/spark-defaults.conf
