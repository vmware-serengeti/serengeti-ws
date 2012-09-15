#!/bin/bash

VHM_SERVICE_PID=`ps aux | grep java | grep elastic-runtime | awk '{print $2}'`

if [ ${VHM_SERVICE_PID} != "" ]; then
  echo "stopping vhm services"
  kill ${VHM_SERVICE_PID}
fi

