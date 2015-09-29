#!/bin/bash
cd target
java -Xmx1024m -jar stream-simulator.jar "$@" 
