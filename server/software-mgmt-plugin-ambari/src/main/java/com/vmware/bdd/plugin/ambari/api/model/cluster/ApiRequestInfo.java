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

import java.util.HashMap;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiRequestInfo {

   @Expose
   @SerializedName("id")
   private Long requestId;

   @Expose
   @SerializedName("cluster_name")
   private String clusterName;

   @Expose
   @SerializedName("aborted_task_count")
   private long abortedTaskCount;

   @Expose
   @SerializedName("completed_task_count")
   private long completedTaskCount;

   @Expose
   @SerializedName("create_time")
   private long createTime;

   @Expose
   @SerializedName("end_time")
   private long endTime;

   @Expose
   @SerializedName("failed_task_count")
   private long failedTaskCount;

   @Expose
   @SerializedName("inputs")
   private String inputs;

   @Expose
   @SerializedName("progress_percent")
   private double progressPercent;

   @Expose
   @SerializedName("queued_task_count")
   private long queuedTaskCount;

   @Expose
   @SerializedName("request_context")
   private String requestContext;

   @Expose
   @SerializedName("request_schedule")
   private String requestSchedule;

   @Expose
   @SerializedName("request_status")
   private String requestStatus;

   @Expose
   private String status;

   @Expose
   @SerializedName("resource_filters")
   private List<String> resourceFilters;

   @Expose
   @SerializedName("start_time")
   private long startTime;

   @Expose
   @SerializedName("task_count")
   private long taskCount;

   @Expose
   @SerializedName("timed_out_task_count")
   private long timedOutTaskCount;

   @Expose
   @SerializedName("type")
   private String type;

   @Expose
   @SerializedName("context")
   private String context;

   @Expose
   @SerializedName("command")
   private String command;

   @Expose
   @SerializedName("parameters")
   private HashMap<String, String> parameters;
   @SerializedName("operation_level")
   private ApiOperationLevel operationLevel;

   public Long getRequestId() {
      return requestId;
   }

   public void setRequestId(Long requestId) {
      this.requestId = requestId;
   }

   public String getClusterName() {
      return clusterName;
   }

   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   public long getAbortedTaskCount() {
      return abortedTaskCount;
   }

   public void setAbortedTaskCount(long abortedTaskCount) {
      this.abortedTaskCount = abortedTaskCount;
   }

   public long getCompletedTaskCount() {
      return completedTaskCount;
   }

   public void setCompletedTaskCount(long completedTaskCount) {
      this.completedTaskCount = completedTaskCount;
   }

   public long getCreateTime() {
      return createTime;
   }

   public void setCreateTime(long createTime) {
      this.createTime = createTime;
   }

   public long getEndTime() {
      return endTime;
   }

   public void setEndTime(long endTime) {
      this.endTime = endTime;
   }

   public long getFailedTaskCount() {
      return failedTaskCount;
   }

   public void setFailedTaskCount(long failedTaskCount) {
      this.failedTaskCount = failedTaskCount;
   }

   public String getInputs() {
      return inputs;
   }

   public void setInputs(String inputs) {
      this.inputs = inputs;
   }

   public double getProgressPercent() {
      return progressPercent;
   }

   public void setProgressPercent(double progressPercent) {
      this.progressPercent = progressPercent;
   }

   public long getQueuedTaskCount() {
      return queuedTaskCount;
   }

   public void setQueuedTaskCount(long queuedTaskCount) {
      this.queuedTaskCount = queuedTaskCount;
   }

   public String getRequestContext() {
      return requestContext;
   }

   public void setRequestContext(String requestContext) {
      this.requestContext = requestContext;
   }

   public String getRequestSchedule() {
      return requestSchedule;
   }

   public void setRequestSchedule(String requestSchedule) {
      this.requestSchedule = requestSchedule;
   }

   public String getRequestStatus() {
      return requestStatus;
   }

   public void setRequestStatus(String requestStatus) {
      this.requestStatus = requestStatus;
   }

   public List<String> getResourceFilters() {
      return resourceFilters;
   }

   public void setResource_filters(List<String> resourceFilters) {
      this.resourceFilters = resourceFilters;
   }

   public long getStartTime() {
      return startTime;
   }

   public void setStartTime(long startTime) {
      this.startTime = startTime;
   }

   public long getTaskCount() {
      return taskCount;
   }

   public void setTaskCount(long taskCount) {
      this.taskCount = taskCount;
   }

   public long getTimedOutTaskCount() {
      return timedOutTaskCount;
   }

   public void setTimedOutTaskCount(long timedOutTaskCount) {
      this.timedOutTaskCount = timedOutTaskCount;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getContext() {
      return context;
   }

   public void setContext(String context) {
      this.context = context;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getCommand() {
      return command;
   }

   public void setCommand(String command) {
      this.command = command;
   }

   public HashMap<String, String> getParameters() {
      return parameters;
   }

   public void setParameters(HashMap<String, String> parameters) {
      this.parameters = parameters;
   }
   public ApiOperationLevel getOperationLevel() {
      return operationLevel;
   }

   public void setOperationLevel(ApiOperationLevel operationLevel) {
      this.operationLevel = operationLevel;
   }
}
