Analyzing IoT Data with Storm, Kafka and Spark 
============================================================

This is a reference app for Internet of Things (IoT) use cases, built around the following tools from the Hortonworks Data Platform: Storm, Kafka, Spark, HBase, and Hive. Here are the steps to install this app on the HDP Sandbox.

Videos on the demo available here:
  - [Arun's Hadoop Summit 2015 keynote](https://youtu.be/FHMMcMYhmNI?t=1h25m13s)
  - [Shaun/George's Hadoop Summit 2014 Keynote](http://library.fora.tv/program_landing_frameview?id=20333&type=clip)
  - [Nauman's Demo at Phoenix Data conference 2014](http://www.youtube.com/watch?v=ErDmSIQ4gX0)
  - [![Phoenix Data conference 2014](http://img.youtube.com/vi/ErDmSIQ4gX0/0.jpg)](http://www.youtube.com/watch?v=ErDmSIQ4gX0)

Deployment options
-----------------
* Deploy on Ambari installed HDP cluster (local or cloud)
* Deploy on sandbox VM
  * Download the HDP 2.3 Sandbox from [here](http://hortonworks.com/products/hortonworks-sandbox/#install)
  * Start the Sandbox, and add its IP address into your local machine's /etc/hosts file:
  ```bash
  $ sudo echo "172.16.139.139 sandbox.hortonworks.com" >> /etc/hosts
  ```



Setup
-----
* Option 1: script setup
  * clone the git and run setup scripts using instructions in [SETUP.md](https://github.com/hortonworks-gallery/iot-truck-streaming/blob/master/SETUP.md)
  
```bash
$ ssh root@sandbox.hortonworks.com
$ cd
$ git clone https://github.com/hortonworks-gallery/iot-truck-streaming
```

* Option 2: Ambari setup
  * deploy using Ambari service using instructions from https://github.com/abajwa-hw/iotdemo-service
