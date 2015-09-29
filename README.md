# iot-truck-streaming


Analyzing IoT Data with Storm, Kafka and Spark 
============================================================

This is a reference app for Internet of Things (IoT) use cases, built around the following tools from the Hortonworks Data Platform: Storm, Kafka, Spark, HBase, and Hive. Here are the steps to install this app on the HDP Sandbox.

Prerequisites
-------------

* Download the HDP 2.3 Sandbox from [here](http://hortonworks.com/products/hortonworks-sandbox/#install)
* Start the Sandbox, and add its IP address into your local machine's /etc/hosts file:

```bash
$ sudo echo "172.16.139.139 sandbox.hortonworks.com" >> /etc/hosts
```

* Log into the sandbox, and clone this repository:  

```bash
$ ssh root@sandbox.hortonworks.com
$ cd
$ git clone https://github.com/DhruvKumar/iot-truck-streaming
```


Build and package the code
-----------------------------------------

Follow the README for more instructions.
