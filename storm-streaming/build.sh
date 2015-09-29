#!/bin/bash
mvn clean package
zip -r storm-streaming.zip *
