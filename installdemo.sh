#!/bin/bash

if [ -n "${JAVA_HOME+1}" ]; then
  echo "JAVA_HOME is" $JAVA_HOME
else
  echo "JAVA_HOME is not set, please set JAVA_HOME to a JDK before installing demo"
  exit 1
fi

echo "**** Preparing to install the Storm Truck Demo."
echo "Please ensure that you updated both config.properties and user-env.sh!"

echo "**** Preparing to install Maven..."
setup/bin/install_maven.sh

echo "**** Preparing to install Scala..."
setup/bin/install_scala.sh

echo "**** Preparing to install ActiveMQ..."
setup/bin/install_activemq.sh

echo "**** Calling setup_demo.sh ..."
setup/bin/setup_demo.sh
