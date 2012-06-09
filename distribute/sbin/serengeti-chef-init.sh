#!/bin/bash

COOKBOOKS_HOME="/opt/serengeti/cookbooks"

cd $COOKBOOKS_HOME
knife cookbook upload --all
for role in roles/*.rb ; do knife role from file $role ; done