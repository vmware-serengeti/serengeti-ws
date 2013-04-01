#!/usr/bin/env bash

if [ "${SERENGETI_HOME}" == "" ]; then
  SERENGETI_HOME='/opt/serengeti'
fi

chef_config_file="${SERENGETI_HOME}/.chef/knife.rb"
property_file="${SERENGETI_HOME}/conf/serengeti.properties"

management_listen_host=`cat ${property_file} | grep management.thrift.server | cut -f2 -d'=' | sed 's/^ *//g'`
if [ "${management_listen_host}" == "" ]; then
  management_listen_host="localhost"
fi

management_listen_port=`cat ${property_file} | grep management.thrift.port | cut -f2 -d'=' | sed 's/^ *//g'`
if [ "${management_listen_port}" == "" ]; then
  management_listen_port="9090"
fi

cd "${SERENGETI_HOME}"

#start software management thrift server
${SERENGETI_HOME}/sbin/run_management_server.rb "${SERENGETI_HOME}" "${chef_config_file}" "${management_listen_host}" "${management_listen_port}" 2>&1 | rotatelogs -l "${SERENGETI_HOME}/logs/ironfan.log.%Y-%m-%d-%H_%M_%S" 20M &

