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
package com.vmware.bdd.apitypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Task get output
 * 
 */
public class TaskRead {
   public enum Status {
      ABANDONED, STARTING, STARTED, STOPPED, STOPPING, COMPLETED, FAILED, UNKNOWN
   }

   public enum Type {
      INNER, VHM, DELETE
   }

   private Long id;
   private Status status;
   private Type type;
   private Double progress;
   private String errorMessage;
   private String workDir;
   private String progressMessage;
   private String target;
   private List<NodeStatus> succeedNodes = new ArrayList<NodeStatus>();
   private List<NodeStatus> failNodes = new ArrayList<NodeStatus>();

   public TaskRead() {

   }

   public TaskRead(Long id, Status status, Type type, Double progress,
         String errorMessage, String workDir, String progressMessage,
         String target) {
      this.id = id;
      this.status = status;
      this.type = type;
      this.progress = progress;
      this.errorMessage = errorMessage;
      this.workDir = workDir;
      this.progressMessage = progressMessage;
      this.target = target;
   }

   @RestIgnore
   public String getProgressMessage() {
      return progressMessage;
   }

   public void setProgressMessage(String progressMessage) {
      this.progressMessage = progressMessage;
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Status getStatus() {
      return status;
   }

   public void setStatus(Status status) {
      this.status = status;
   }

   public Type getType() {
      return type;
   }

   public void setType(Type type) {
      this.type = type;
   }

   public Double getProgress() {
      return progress;
   }

   public void setProgress(Double progress) {
      this.progress = progress;
   }

   public String getErrorMessage() {
      return errorMessage;
   }

   public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
   }

   @RestIgnore
   public String getWorkDir() {
      return workDir;
   }

   public void setWorkDir(String workDir) {
      this.workDir = workDir;
   }

   @Override
   public String toString() {
      StringBuilder strBuilder = new StringBuilder();
      strBuilder.append("TaskRead [id=" + id + ", status=" + status + ", type="
            + type + ", progress=" + progress + ", errorMessage="
            + errorMessage + ", workDir=" + workDir + ", progressMessage="
            + progressMessage + "]");
      if (this.succeedNodes.size() > 0) {
         strBuilder.append(",succeed nodes=[");
         for (TaskRead.NodeStatus status : succeedNodes) {
            strBuilder.append(status.toString());
            strBuilder.append(",");
         }
         strBuilder.append("]");
      }
      if (this.failNodes.size() > 0) {
         strBuilder.append(",fail nodes=[");
         for (TaskRead.NodeStatus status : failNodes) {
            strBuilder.append(status.toString());
            strBuilder.append(",");
         }
         strBuilder.append("]");
      }
      return strBuilder.toString();
   }

   public String getTarget() {
      return target;
   }

   public void setTarget(String target) {
      this.target = target;
   }

   /**
    * @return the succeedNodes
    */
   public List<NodeStatus> getSucceedNodes() {
      return succeedNodes;
   }

   /**
    * @param succeedNodes
    *           the succeedNodes to set
    */
   public void setSucceedNodes(List<NodeStatus> succeedNodes) {
      this.succeedNodes = succeedNodes;
   }

   /**
    * @return the failNodes
    */
   public List<NodeStatus> getFailNodes() {
      return failNodes;
   }

   /**
    * @param failNodes
    *           the failNodes to set
    */
   public void setFailNodes(List<NodeStatus> failNodes) {
      this.failNodes = failNodes;
   }

   public static class NodeStatus {
      private String nodeName;
      private String ip;
      private String status;
      private long memory;
      private int cpuNumber;
      private boolean succeed = true;
      private String errorMessage;

      public NodeStatus() {

      }

      public NodeStatus(String nodeName) {
         this.nodeName = nodeName;
      }

      public NodeStatus(String nodeName, boolean succeed, String errorMessage) {
         this(nodeName);
         this.succeed = succeed;
         this.errorMessage = errorMessage;
      }

      /**
       * @return the nodeName
       */
      public String getNodeName() {
         return nodeName;
      }

      /**
       * @param nodeName
       *           the nodeName to set
       */
      public void setNodeName(String nodeName) {
         this.nodeName = nodeName;
      }

      @RestIgnore
      public String getIp() {
         return ip;
      }

      public void setIp(String ip) {
         this.ip = ip;
      }

      @RestIgnore
      public String getStatus() {
         return status;
      }

      public void setStatus(String status) {
         this.status = status;
      }

      @RestIgnore
      public long getMemory() {
         return memory;
      }

      public void setMemory(long memory) {
         this.memory = memory;
      }

      @RestIgnore
      public int getCpuNumber() {
         return cpuNumber;
      }

      public void setCpuNumber(int cpuNumber) {
         this.cpuNumber = cpuNumber;
      }

      /**
       * @return the succeed
       */
      public boolean isSucceed() {
         return succeed;
      }

      /**
       * @param succeed
       *           the succeed to set
       */
      public void setSucceed(boolean succeed) {
         this.succeed = succeed;
      }

      /**
       * @return the errorMessage
       */
      public String getErrorMessage() {
         return errorMessage;
      }

      /**
       * @param errorMessage
       *           the errorMessage to set
       */
      public void setErrorMessage(String errorMessage) {
         this.errorMessage = errorMessage;
      }

      @Override
      public String toString() {
         StringBuilder strBuilder = new StringBuilder();
         strBuilder.append("nodeName:");
         strBuilder.append(nodeName);
         strBuilder.append(", succeed:");
         strBuilder.append(succeed);
         if (errorMessage != null) {
            strBuilder.append(", error message:");
            strBuilder.append(errorMessage);
         }
         return strBuilder.toString();
      }

   }
}
