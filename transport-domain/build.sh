#!/bin/bash
mvn clean package
mvn install:install-file -Dfile=target/transport-domain-1.0.0-SNAPSHOT.jar -DgroupId=poc.hortonworks.domain -Dversion=1.0.0-SNAPSHOT -DartifactId=transport-domain -Dpackaging=jar
