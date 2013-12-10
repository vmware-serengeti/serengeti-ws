/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.software.mgmt.impl;


import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.software.mgmt.exception.SoftwareManagementException;
import com.vmware.bdd.software.mgmt.thrift.ClusterOperation;
import com.vmware.bdd.software.mgmt.thrift.ClusterOperationException;
import com.vmware.bdd.software.mgmt.thrift.OperationStatusWithDetail;
import com.vmware.bdd.software.mgmt.thrift.SoftwareManagement;

/**
 * @author Jarred Li
 * @version 0.8
 * @since 0.8
 * 
 */
public class SoftwareManagementClient implements SoftwareManagement.Iface {

   private static final Logger logger = Logger
         .getLogger(SoftwareManagementClient.class);

   private TTransport transport;
   private SoftwareManagement.Client managementClient;

   public void init() {
      if (transport != null && managementClient != null && transport.isOpen()) {
         return;
      }
      try {
         transport =
               new TSocket(Configuration.getString("management.thrift.server"),
                     Configuration.getInt("management.thrift.port"),
                     Configuration.getInt("management.thrift.timeout", 0));
         transport.open();
         TProtocol protocol = new TBinaryProtocol(transport);
         managementClient = new SoftwareManagement.Client(protocol);
      } catch (TTransportException e) {
         logger.error("Can not establish Thrift server connection");
         throw SoftwareManagementException.CONNECT_THRIFT_SERVER_FAILURE(e);
      }
   }


   public void close() {
      transport.close();
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.software.mgmt.thrift.SoftwareManagement.Iface#runClusterOperation(com.vmware.bdd.software.mgmt.thrift.ClusterOperation)
    */
   @Override
   public int runClusterOperation(ClusterOperation clusterOperation) {
      try {
         return managementClient.runClusterOperation(clusterOperation);
      } catch (ClusterOperationException e) {
         logger.error("Failed run cluseter operation for cluster: "
               + clusterOperation.getTargetName());
         throw SoftwareManagementException.CLUSTER_OPERATIOIN_FAILURE(e,
               clusterOperation.getTargetName(), clusterOperation.getAction()
                     .toString(), e.getMessage());
      } catch (Throwable t) {
         throw SoftwareManagementException.CLUSTER_OPERATIOIN_UNKNOWN_ERROR(t,
               clusterOperation.getTargetName(), clusterOperation.getAction()
                     .toString());
      }
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.software.mgmt.thrift.SoftwareManagement.Iface#getOperationStatusWithDetail(java.lang.String)
    */
   @Override
   public OperationStatusWithDetail getOperationStatusWithDetail(
         String targetName) {
      try {
         return managementClient.getOperationStatusWithDetail(targetName);
      } catch (ClusterOperationException e) {
         logger.error("Failed to get operation status for target: "
               + targetName);
         throw SoftwareManagementException.GET_OPERATIOIN_STATUS_FAILURE(e,
               targetName, e.getMessage());
      } catch (Throwable t) {
         throw SoftwareManagementException.GET_OPERATIOIN_STATUS_UNKNOWN_ERROR(
               t, targetName);
      }
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.software.mgmt.thrift.SoftwareManagement.Iface#resetNodeProvisionAttribute(java.lang.String)
    */
   @Override
   public void resetNodeProvisionAttribute(String targetName)
         throws ClusterOperationException, TException {
      try {
         managementClient.resetNodeProvisionAttribute(targetName);
      } catch (ClusterOperationException e) {
         logger.error("Failed to reset node provision attribute for target: "
               + targetName);
         throw SoftwareManagementException.GET_OPERATIOIN_STATUS_FAILURE(e,
               targetName, e.getMessage());
      } catch (Throwable t) {
         throw SoftwareManagementException.GET_OPERATIOIN_STATUS_UNKNOWN_ERROR(
               t, targetName);
      }
   }

}
