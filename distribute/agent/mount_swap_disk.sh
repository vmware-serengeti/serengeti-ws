#!/bin/bash

# ***** BEGIN LICENSE BLOCK *****
#    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
#    Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ***** END LICENSE BLOCK *****

SWAP_DISK=/dev/sdb

if [ -b ${SWAP_DISK} ]; then
  mkswap ${SWAP_DISK} && swapon ${SWAP_DISK}
  if [ $? == 0 ]; then
    # edit fstab file
    swapoff /dev/sda2
    sed -i '/swap/d' /etc/fstab
    echo "${SWAP_DISK}           swap            swap            defaults       0 0" >> /etc/fstab
  fi
fi
