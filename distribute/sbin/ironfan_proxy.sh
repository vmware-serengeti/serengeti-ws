#!/bin/bash

if [ "${SERENGETI_HOME}" == "" ]; then 
  SERENGETI_HOME='/opt/serengeti' 
fi

cd ${SERENGETI_HOME}/cookbooks

if [ $1 = "query" ]; then
  cmd="knife cluster show $2 -f $3 $4"
elif [ $1 = "create" ]; then
  cmd="knife cluster create $2 -f $3 --yes --bootstrap $4"
elif [ $1 = "update" ]; then
  cmd="knife cluster create $2 -f $3 --yes --bootstrap $4"
elif [ $1 = "start" ]; then
  cmd="knife cluster start $2 -f $3 --yes --bootstrap $4"
elif [ $1 = "stop" ]; then
  cmd="knife cluster stop $2 -f $3 --yes $4"
elif [ $1 = "destroy" ]; then
  cmd="knife cluster kill $2 -f $3 --yes $4"
elif [ $1 = "configure" ]; then
  cmd="knife cluster bootstrap $2 -f $3 --yes $4"
elif [ $1 = "configurehw" ]; then
  cmd="knife cluster config $2 -f $3 --yes $4"
else
  echo "wrong command $1"
fi

echo $cmd
$cmd
