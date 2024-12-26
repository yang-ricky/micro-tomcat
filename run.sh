#!/bin/bash
mvn clean package
java -cp target/micro-tomcat-1.0-SNAPSHOT.jar com.microtomcat.HttpServer