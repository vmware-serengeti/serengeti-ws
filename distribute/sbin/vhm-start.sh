#!/bin/sh

# Source function library
. /etc/init.d/functions

SERENGETI_HOME=/opt/serengeti

VHM_HOME=${SERENGETI_HOME}/vhm
VHM_JAR=`ls ${VHM_HOME}/elastic-runtime-*-jar-with-dependencies.jar`

JAVA_OPTS="-Dserengeti.home.dir=${SERENGETI_HOME}"

echo -n "Starting VHM service"
java ${JAVA_OPTS} -jar ${VHM_JAR} >> /opt/serengeti/logs/vhm.log 2>&1 &

VHM_SERVICE_PID=`ps aux | grep java | grep elastic-runtime | awk '{print $2}'`
if [ "${VHM_SERVICE_PID}" != "" ]; then
  success
  echo
else
  failure
  echo
fi
