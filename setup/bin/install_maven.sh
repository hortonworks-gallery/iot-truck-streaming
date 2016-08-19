#!/bin/bash

wget https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.2.5/apache-maven-3.2.5-bin.tar.gz
mv apache-maven-3.2.5-bin.tar.gz /tmp
tar xvzf /tmp/apache-maven-*-bin.tar.gz -C /root
mv /root/apache-maven* /root/maven

echo 'M2_HOME=/root/maven' >> ~/.bashrc
echo 'M2=$M2_HOME/bin' >> ~/.bashrc
echo 'PATH=$PATH:$M2' >> ~/.bashrc
