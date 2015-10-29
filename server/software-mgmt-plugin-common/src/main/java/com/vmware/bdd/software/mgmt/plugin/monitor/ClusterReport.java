/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;

/**
 * Author: Xiaoding Bian
 * Date: 6/10/14
 * Time: 7:34 PM
 */
public class ClusterReport implements Cloneable {

   private String name;

   private String action;

   private boolean success;

   private boolean finished;

   private int progress;
   private String errMsg;
   private String errCode;

   private ServiceStatus status;

   /**
    * This map should not be empty
    */
   private Map<String, NodeReport> nodeReports = new HashMap<String, NodeReport>();

   public ClusterReport() {}

   public ClusterReport (ClusterBlueprint clusterBlueprint) {
      this.name = clusterBlueprint.getName();
      this.action = null;
      this.finished = false;
      this.success = false;
      this.progress = 0;
      for (NodeGroupInfo ng : clusterBlueprint.getNodeGroups()) {
         for (NodeInfo nodeInfo : ng.getNodes()) {
            NodeReport nodeReport = new NodeReport(nodeInfo);
            this.nodeReports.put(nodeReport.getName(), nodeReport);
         }
      }
   }

   public Map<String, NodeReport> getNodeReports() {
      return nodeReports;
   }

   public void setNodeReports(Map<String, NodeReport> nodeReports) {
      this.nodeReports = nodeReports;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getAction() {
      return action;
   }

   public void setAction(String action) {
      this.action = action;
   }

   public void setClusterAndNodesAction(String action) {
      setAction(action);
      for (NodeReport nodeReport : this.nodeReports.values()) {
         nodeReport.setAction(action);
      }
   }

   public void setNodesAction(String action, List<String> nodeNames) {
      if (nodeNames == null) {
         return;
      }
      for (String nodeName : nodeNames) {
         nodeReports.get(nodeName).setAction(action);
      }
   }

   public void setNodesStatus(ServiceStatus status, List<String> nodeNames) {
      if (nodeNames == null) {
         return;
      }
      for (String nodeName : nodeNames) {
         nodeReports.get(nodeName).setStatus(status);
      }
   }

   public void setNodesError(String errMsg, List<String> nodeNames) {
      if (nodeNames == null) {
         return;
      }
      for (String nodeName : nodeNames) {
         nodeReports.get(nodeName).setErrMsg(errMsg);
      }
   }

   public void setClusterAndNodesServiceStatus(ServiceStatus status) {
      setStatus(status);
      for (NodeReport nodeReport : this.nodeReports.values()) {
         nodeReport.setStatus(status);
      }
   }

   public void setClusterAndNodesServiceStatus(ServiceStatus status, List<String> nodeNames) {
      setStatus(status);
      for (String nodeName: nodeNames) {
         nodeReports.get(nodeName).setStatus(status);
      }
   }

   public void clearAllNodesErrorMsg() {
      for(NodeReport nodeReport: this.nodeReports.values()) {
         nodeReport.setErrMsg(null);
      }
   }

   public boolean isSuccess() {
      return success;
   }

   public void setSuccess(boolean success) {
      this.success = success;
   }

   public boolean isFinished() {
      return finished;
   }

   public void setFinished(boolean finished) {
      this.finished = finished;
   }

   public int getProgress() {
      return progress;
   }

   public void setProgress(int progress) {
      this.progress = progress;
   }

   public String getErrMsg() {
      return errMsg;
   }

   public void setErrMsg(String errMsg) {
      this.errMsg = errMsg;
   }

   public String getErrCode() {
      return errCode;
   }

   public void setErrCode(String errCode) {
      this.errCode = errCode;
   }

   public ServiceStatus getStatus() {
      return status;
   }

   public void setStatus(ServiceStatus status) {
      this.status = status;
   }

   /**
    * deep clone
    * @return
    */
   @Override
   public ClusterReport clone() {
      try {
         ClusterReport report = (ClusterReport) super.clone();
         Map<String, NodeReport> oldNodeReports = report.getNodeReports();
         Map<String, NodeReport> newNodeReports = new HashMap<String, NodeReport>();
         for (String key : oldNodeReports.keySet()) {
            newNodeReports.put(key, oldNodeReports.get(key).clone());
         }
         report.setNodeReports(newNodeReports);
         return report;

      } catch (CloneNotSupportedException e) {
         return null;
      }
   }

}
