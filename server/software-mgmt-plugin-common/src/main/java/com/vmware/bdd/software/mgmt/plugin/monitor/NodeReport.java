/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.software.mgmt.plugin.monitor;

import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;

/**
 * Author: Xiaoding Bian
 * Date: 6/10/14
 * Time: 7:33 PM
 */
public class NodeReport implements Cloneable {

   private String name;

   private String ipAddress;

   private String action;

   private ServiceStatus status;

   private boolean finished;

   private boolean success;

   private int progress;

   private int errCode;

   private String errMsg;

   private boolean useClusterMsg;

   public NodeReport() {}

   public NodeReport(NodeInfo nodeInfo) {
      this.name = nodeInfo.getName();
      this.ipAddress = nodeInfo.getMgtIpAddress();
      this.action = null;
      this.status = null;
      this.success = false;
      this.finished = false;
      this.errCode = 0;
      this.errMsg = null;
      this.progress = 0;
      this.useClusterMsg = true;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public String getAction() {
      return action;
   }

   public void setAction(String action) {
      this.action = action;
   }

   public ServiceStatus getStatus() {
      return status;
   }

   public void setStatus(ServiceStatus status) {
      this.status = status;
   }

   public boolean isFinished() {
      return finished;
   }

   public void setFinished(boolean finished) {
      this.finished = finished;
   }

   public boolean isSuccess() {
      return success;
   }

   public void setSuccess(boolean success) {
      this.success = success;
   }

   public int getProgress() {
      return progress;
   }

   public void setProgress(int progress) {
      this.progress = progress;
   }

   public int getErrCode() {
      return errCode;
   }

   public void setErrCode(int errCode) {
      this.errCode = errCode;
   }

   public String getErrMsg() {
      return errMsg;
   }

   public void setErrMsg(String errMsg) {
      this.errMsg = errMsg;
   }

   public boolean isUseClusterMsg() {
      return useClusterMsg;
   }

   public void setUseClusterMsg(boolean useClusterMsg) {
      this.useClusterMsg = useClusterMsg;
   }

   /**
    * deep clone
    * @return
    */
   @Override
   public NodeReport clone() {
      try {
         return (NodeReport) super.clone();
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }

}
