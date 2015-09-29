#!/bin/bash

wget http://scala-lang.org/files/archive/scala-2.10.4.tgz 

mv scala*.tgz /tmp
tar xvzf /tmp/scala*.tgz -C /root
mv /root/scala* /root/scala

echo 'SCALA_HOME=/root/scala' >> ~/.bashrc
echo 'PATH=$PATH:$SCALA_HOME/bin' >> ~/.bashrc

