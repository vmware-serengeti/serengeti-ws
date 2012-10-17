#!/bin/bash

vmware-rpctool "info-get guestinfo.ovfEnv" > /tmp/ovfEnv

sed -i "s|<evs:Token>.*</evs:Token>|<evs:Token>*** HIDDEN ***</evs:Token>|g" /tmp/ovfEnv

vmware-rpctool "info-set guestinfo.ovfEnv `cat /tmp/ovfEnv`"

rm -f /tmp/ovfEnv
