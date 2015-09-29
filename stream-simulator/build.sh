#!/bin/bash
mvn clean package
zip -r stream-simulator.zip run.sh target
mvn install:install-file -Dfile=target/stream-simulator.jar -DgroupId=com.hortonworks -Dversion=1.0-SNAPSHOT -DartifactId=stream-simulator -Dpackaging=jar
