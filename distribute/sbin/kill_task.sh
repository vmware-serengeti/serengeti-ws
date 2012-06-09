#!/bin/bash

kill_subprocs() {
   local pid=$1
   for child in $(ps -o pid --no-headers --ppid ${pid}); do
      # kill the children first, or else the they will be orphan
      kill_subprocs ${child}
      echo "killing process with pid ${child}"
      kill -KILL ${child}
   done
}

if [ $# -ne 1 ]; then
    echo "Usage: $0 pid"
    exit 1
fi

pid=$1
kill_subprocs $pid

timeout=3
while [ $timeout -gt 0 -a -n "$(ps -o command --no-headers -p $pid)" ]; do
  sleep 1
  timeout=$((timeout-1))
done

if [ -n "$(ps -o command --no-headers -p $pid)" ]; then
  echo "kill failed, process still alive"
  exit 2
fi

exit 0
