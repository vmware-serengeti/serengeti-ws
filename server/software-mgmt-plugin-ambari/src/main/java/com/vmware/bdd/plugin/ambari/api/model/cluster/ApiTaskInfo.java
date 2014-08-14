/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.api.model.cluster;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiTaskInfo {

   @Expose
   @SerializedName("attempt_cnt")
   private int attemptCnt;

   @Expose
   @SerializedName("cluster_name")
   private String clusterName;

   @Expose
   private String command;

   @Expose
   @SerializedName("command_detail")
   private String commandDetail;

   @Expose
   @SerializedName("start_time")
   private Long startTime;

   @Expose
   @SerializedName("end_time")
   private Long endTime;

   @Expose
   @SerializedName("exit_code")
   private int exitCode;

   @Expose
   @SerializedName("host_name")
   private String hostName;

   @Expose
   private Long id;

   @Expose
   @SerializedName("request_id")
   private Long requestId;

   @Expose
   private String role;

   @Expose
   @SerializedName("stage_id")
   private int stageId;

   @Expose
   private String status;

   @Expose
   private String stderr;

   public int getAttemptCnt() {
      return attemptCnt;
   }

   public void setAttemptCnt(int attemptCnt) {
      this.attemptCnt = attemptCnt;
   }

   public String getClusterName() {
      return clusterName;
   }

   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   public String getCommand() {
      return command;
   }

   public void setCommand(String command) {
      this.command = command;
   }

   public String getCommandDetail() {
      return commandDetail;
   }

   public void setCommandDetail(String commandDetail) {
      this.commandDetail = commandDetail;
   }

   public Long getStartTime() {
      return startTime;
   }

   public void setStartTime(Long startTime) {
      this.startTime = startTime;
   }

   public Long getEndTime() {
      return endTime;
   }

   public void setEndTime(Long endTime) {
      this.endTime = endTime;
   }

   public int getExitCode() {
      return exitCode;
   }

   public void setExitCode(int exitCode) {
      this.exitCode = exitCode;
   }

   public String getHostName() {
      return hostName;
   }

   public void setHostName(String hostName) {
      this.hostName = hostName;
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Long getRequestId() {
      return requestId;
   }

   public void setRequestId(Long requestId) {
      this.requestId = requestId;
   }

   public String getRole() {
      return role;
   }

   public void setRole(String role) {
      this.role = role;
   }

   public int getStageId() {
      return stageId;
   }

   public void setStageId(int stageId) {
      this.stageId = stageId;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getStderr() {
      return stderr;
   }

   public void setStderr(String stderr) {
      this.stderr = stderr;
   }

}
