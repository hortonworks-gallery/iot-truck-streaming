#!/bin/bash

wget http://www.motorlogy.com/apache/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz  
mv apache-maven-3.2.5-bin.tar.gz /tmp
tar xvzf /tmp/apache-maven-*-bin.tar.gz -C /root
mv /root/apache-maven* /root/maven

echo 'M2_HOME=/root/maven' >> ~/.bashrc
echo 'M2=$M2_HOME/bin' >> ~/.bashrc
echo 'PATH=$PATH:$M2' >> ~/.bashrc
