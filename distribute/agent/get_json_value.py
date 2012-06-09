#!/usr/bin/python

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

#
# Provides functions to return the value for a given key from a JSON-encoded
# input string of key, value pairs.
#

try:
    import json
except ImportError:
    import simplejson as json
import os
import sys

def checkType(progName, e, t):
   if not isinstance(e, t):
      sys.stderr.write('%s: Expression %s must be of type %s\n' % (progName, e, t))
      return -1
   else:
      return 0

#
# progName: name of the executable. Used instead of direct access to argv[0].
# jsonString: JSON-encoded string of key, value pairs.
# key: the key to be searched.
#
# returns: None if the key is not found, otherwise its value.
#
def getValue(progName, jsonString, key):
   kvDict = json.loads(jsonString)
 #  if checkType(progName, kvDict, dict) == -1:
 #     return None

   if key not in kvDict:
       return None

   value = kvDict[key]
 #  if checkType(progName, value, unicode) == -1:
 #     return None

   return value.encode('utf8')

#
# returns: 0 on success, -1 on error.
#          The found value is written to stdout.
#
def main():
   if len(sys.argv) != 3:
      sys.stderr.write('Usage: %s <json_string> <key>\n' % sys.argv[0])
      return -1

   value = getValue(sys.argv[0], sys.argv[1], sys.argv[2])
   if value is not None:
      sys.stdout.write(value)
      return 0
   else:
      return -1

if __name__ == '__main__':
   sys.exit(main())
