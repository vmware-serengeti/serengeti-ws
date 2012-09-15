#!/bin/sh

SERENGETI_HOME=/opt/serengeti

VHM_HOME=${SERENGETI_HOME}/vhm
VHM_JAR=`ls ${VHM_HOME}/elastic-runtime-*-jar-with-dependencies.jar`

JAVA_OPTS="-Dserengeti.home.dir=${SERENGETI_HOME}"

echo "starting VHM service"
java ${JAVA_OPTS} -jar ${VHM_JAR} >/dev/null 2>&1 &
