#!/bin/bash
mvn package -DskipTests -f pom.xml
mvn package -DskipTests -f pom-spark.xml
