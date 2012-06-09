#!/bin/bash

service="sudo /sbin/service"

for name in couchdb postgresql rabbitmq-server httpd tomcat chef-solr chef-expander chef-server chef-server-webui; do
  $service $name start

  if [ $? -ne 0 ]; then
    echo "Cannot start service $name, please start it mannually."
  fi
done
