## Setup instuctions

### Prereqs:
1. The setup scripts for the demo must be run from the Ambari machine
2. Demo will be installed and run under the root user
3. wget must be available
4. A zookeeper server must be present on the node where you run the script.
5. If KAFKA is not installed on the Ambari server node, then you must manually create the kafka topic ahead of time
  ```/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper $ZK_HOST:2181 --replication-factor 1 --partitions 2 --topic truck_events```

6. Ensure HBase and Storm are up
7. Ensure HBase, Storm, Kafka, Falcon and Spark are not in maintenance mode
9. If running on sandbox:
	- ensure there is at least 8GB of RAM assigned to VM
	- ensure the hosts file is correct: in /etc/hosts, ensure hostname (e.g. sandbox.hortonworks.com) is mapped to actual IP of VM instead of 127.0.0.1

### Instructions to install demo:
- set JAVA_HOME if not defined
- For sandbox 2.2.4 and later, check the Ranger config. See Ranger config note below **
- copy the demo's directory (storm_demo_2.2/) to the local filesystem under /root
- make the scripts executable:
  cd storm_demo_2.2/storm_demo/
  chmod 750 *.sh setup/bin/*.sh
- update config.properties with host names where your services run,  including the names of the supervisor nodes
	- NOTE: the demo will pick up the version of config.properties at /etc/storm_demo at runtime
- update variables defined at the top in user-env.sh
	-user is ambari user
	-pass is ambari password
	-cluster is the name of cluster you will install demo on
	-host is the ambari url, eg: localhost:8080
- installdemo.sh
- source root's bashrc ". /root/.bashrc"
- If on sandbox, run 'rundemo.sh clean', else run 'rundemo.sh'
- When you see the "[INFO] Started Jetty Server" message, the demo is up at: http://<yourhost>:8081/storm-demo-web-app/index.html

In a subsequent run, you may want to do 'rundemo.sh clean' which will kill the topology, stop storm, cleanup storm dirs, and restart storm.

### Ranger Config:
1. Start Ranger service in case its not up: service ranger-admin start
2. Login to Ranger ui at http://sandbox.hortonworks.com:6080
3. Open the HBase policies (sandbox_hbase) page and click the "HBase Global Allow" policy (link below) and ensure that groups "root" and "hadoop" have access. If not, add them. Click "Save" to refresh the policy.
http://sandbox.hortonworks.com:6080/index.html#!/hbase/3/policy/8


### Spark extensions to demo:

- By default the demo uses a pre-built Spark model, so the Prediction UI and the associated Prediction Storm Bolts work out of the box.
- To demo the Spark model:
	- If doing a live demo to a customer, ensure you have done the following before the demo:
		- ensure you have generated a bunch of trucking events that got written to HDFS 
		- cd ../truckml
		- run transformEventsForSpark.sh
			- this will invoke a Pig script that will enrich and transform raw truck events for input into Spark ML. 
	- Live demo:
		- runspark.sh
		- this will compile the Spark ML code (BinaryClassification.scala) and will submit a Spark job to YARN.
		- the Spark Job can be viewed in the YARN resource manager UI. 
		- when the job finishes look at the output of job in YARN RM UI to see how the model performed (precision & recall metrics)
		- the output (or coefficients) of the Spark logistic regression model is an array of doubles thats written to HDFS at /tmp/sparkML_weights_<timestamp>
		- the Prediction Storm bolt uses default coefficients at /tmp/sparkML_weights/lrweights to recreate the model at runtime. If you want it to pickup weights from your updated model, then r		    emove /tmp/sparkML_weights/lrweights from HDFS and replace it with /tmp/sparkML_weights_<timestamp> of your choice.
		- the Prediction bolt puts prediction events on active MQ, which are consumed by the Prediction UI
		- the Prediction UI renders non-violation predictions as green dots. A violation prediction is rendered yellow/orange. If 3 successive violation predictions are received for a driver, th		    en the dot changes to red. 


### Credit
- Hortonworks SE team
