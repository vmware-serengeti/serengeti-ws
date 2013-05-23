#/***************************************************************************
# * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
# * Licensed under the Apache License, Version 2.0 (the "License");
#* you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
#***************************************************************************/
#

#
# Interface for Hadoop software management, such as create, start, stop
#

namespace java com.vmware.bdd.software.mgmt.thrift
namespace rb software.mgmt.thrift



enum ClusterAction {
  QUERY = 1,
  CREATE = 2,
  UPDATE = 3,
  START = 4,
  STOP = 5,
  DESTROY = 6,
  CONFIGURE = 7
} 
enum ServerStatus{
  VM_READY=1,
  SERVICE_READY=2,
  BOOTSTRAP_FAILED=3
}


struct ServerData{
  1:optional string action,
  2:optional bool bootstrapped,
  3:optional bool created,
  4:optional bool deleted,
  5:optional i32 error_code,
  6:optional string error_msg,
  7:optional bool finished,
  8:optional bool ha,
  9:optional string hostName,
  10:optional string ipAddress,
  11:optional string name,
  12:optional string physicalHost,
  13:optional i32 progress,
  14:optional string rack,
  15:optional string status,
  16:optional bool succeed,
}


struct GroupData{
  1:optional string groupName,
  2:optional list<ServerData> instances,
}

struct ClusterData{
  1:optional string clusterName,
  2:optional map<string, GroupData> groups,
}

/**
 Operation Status data structure
*/
struct OperationStatus{
  1:optional bool finished,
  2:optional bool succeed,
  3:optional i32 progress,
  4:optional string error_msg,
  5:optional i32 total,
  6:optional i32 success,
  7:optional i32 failure,
  8:optional i32 running,

}

struct OperationStatusWithDetail{
  1:optional OperationStatus operationStatus,
  2:optional ClusterData clusterData,
}

/**
 Cluster operation data structure
*/
struct ClusterOperation{
  1:required ClusterAction action,
  2:required string targetName,
  3:optional string specFileName,
  4:optional string logLevel,
}


exception ClusterOperationException {
  1:string message,
}

service SoftwareManagement{
  /**
    Run cluster operation, such as create, start, stop
  */
  i32 runClusterOperation(1:ClusterOperation clusterOperation) throws (1:ClusterOperationException coe),

  /**
    Query operation progress
   */
  OperationStatusWithDetail getOperationStatusWithDetail(1:string targetName) throws (1:ClusterOperationException coe),
}

