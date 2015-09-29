#!/bin/bash
mvn clean package
mvn jetty:run -Djetty.port=8081
