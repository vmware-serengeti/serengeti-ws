/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.TaskRead.Status;
import com.vmware.bdd.apitypes.TaskRead.Type;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.task.TaskListener;
import com.vmware.bdd.manager.task.VHMReceiveListener;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Configuration;

@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "task_seq", allocationSize = 1)
@Table(name = "task")
public class TaskEntity extends EntityBase {
   private static final Logger logger = Logger.getLogger(TaskEntity.class);
   
   private static final String TASK_ID_STR = "${task_id}";
   private static String routeKeyFormat = "task." + TASK_ID_STR;
   private static File taskRootDir;
   private static String vhmLog = "vhm.log";
   private static String taskRootDirStr = "";
   static {
      taskRootDirStr = System.getProperty("serengeti.home.dir");
      if (taskRootDirStr != null) {
         taskRootDir = new File(taskRootDirStr, "logs/task");
      } else {
         taskRootDir = new File("/tmp/serengeti");
      }

      logger.info("setting task work dir to: " + taskRootDir.getAbsolutePath());
      if (!taskRootDir.exists()) {
         logger.info("task root directory does not exist, try to create one");
         taskRootDir.mkdirs();
      }

      routeKeyFormat = Configuration.getString("task.rabbitmq.routekey_fmt",
            routeKeyFormat);
      AuAssert.check(routeKeyFormat.contains(TASK_ID_STR));
   }

   public TaskEntity() {
   }

   public TaskEntity(String[] cmdArray, TaskListener taskListener, String cookie) {
      setCmdArray(cmdArray);
      setProgress(0.0);
      setStatus(Status.CREATED);
      setTaskListener(taskListener);
      setCreationTime(new Date());
      setCookie(cookie);
   }

   @Column(name = "cmd_array_json", nullable = false)
   private String cmdArrayJson;

   @Column(name = "status", nullable = false)
   @Enumerated(EnumType.STRING)
   private Status status;

   @Column(name = "progress", nullable = false)
   private Double progress;

   @Column(name = "listener")
   private byte[] byteListener;

   @Transient
   private TaskListener listener;

   @Column(name = "ctime")
   private Date creationTime;

   @Column(name = "ftime")
   private Date finishTime;

   @Column(name = "error")
   private String errorMessage;

   @Column(name = "progress_msg")
   private String progressMessage;

   // identifier of task scheduler (which will always change after restart)
   @Column(name = "cookie")
   private String cookie;

   @Column(name = "retry")
   private boolean retry = true;

   @Column(name = "target")
   private String target;

   public TaskListener getTaskListener() {
      if (listener == null) {
         depersistListener();
      }
      return listener;
   }

   public void setTaskListener(TaskListener taskListener) {
      this.listener = taskListener;
      persistListener();
   }

   private void persistListener() throws BddException {
      try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(listener);
         this.byteListener = baos.toByteArray();
      } catch (IOException e) {
         throw BddException.INTERNAL(e, "failed to persist listener");
      }
   }

   private void depersistListener() throws BddException {
      try {
         InputStream is = new ByteArrayInputStream(byteListener);
         ObjectInputStream ois = new ObjectInputStream(is);
         listener = (TaskListener) ois.readObject();
      } catch (Exception e) {
         throw BddException.INTERNAL(e, "failed to depersist listener");
      }
   }

   @SuppressWarnings("static-access")
   public File getWorkDir() {
      File path = null;
      Type type = getType();
      if (type.equals(type.INNER)) {
         if (getId() == null) {
            return null;
         }

         path = new File(taskRootDir, getId().toString());
         if (!path.exists()) {
            path.mkdirs();
         }
      } else if (type.equals(type.VHM)) {
         String serengetiLogPath = "";
         if (!CommonUtil.isBlank(taskRootDirStr)) {
            serengetiLogPath = taskRootDirStr + "/logs/" + vhmLog;
         } else {
            serengetiLogPath = "/opt/serengeti/logs" + vhmLog;
         }
         logger.info("serengeti.log path is  " + serengetiLogPath);
         path = new File(serengetiLogPath);
         AuAssert.check(path.exists(),"The " + vhmLog + " must exist !");
      }

      return path;
   }

   public String getMessageRouteKey() {
      if (getId() == null) {
         return null;
      }
      String routeKey = routeKeyFormat.replace("${task_id}", getId().toString());
      AuAssert.check(!routeKey.equals(routeKeyFormat), "routeKey is not generated");
      return routeKey;
   }

   public String[] getCmdArray() {
      Gson gson = new Gson();
      return gson.fromJson(cmdArrayJson, String[].class);
   }

   public void setCmdArray(String[] cmdArray) {
      Gson gson = new Gson();
      cmdArrayJson = gson.toJson(cmdArray);
   }

   public String getCmdArrayJson() {
      return cmdArrayJson;
   }

   public void setCmdArrayJson(String cmdArrayJson) {
      this.cmdArrayJson = cmdArrayJson;
   }

   public Status getStatus() {
      return status;
   }

   public void setStatus(Status status) {
      this.status = status;
      if (status == Status.FAILED || status == Status.SUCCESS) {
         setFinishTime(new Date());
      }
   }

   public Double getProgress() {
      return progress;
   }

   public void setProgress(Double progress) {
      this.progress = progress;
   }

   public Date getCreationTime() {
      return creationTime;
   }

   public void setCreationTime(Date creationTime) {
      this.creationTime = creationTime;
   }

   public Date getFinishTime() {
      return finishTime;
   }

   public void setFinishTime(Date finishTime) {
      this.finishTime = finishTime;
   }

   public String getCookie() {
      return cookie;
   }

   public void setCookie(String cookie) {
      this.cookie = cookie;
   }

   public String getErrorMessage() {
      return errorMessage;
   }

   public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
   } 

   public String getProgressMessage() {
      return progressMessage;
   }

   public void setProgressMessage(String progressMessage) {
      this.progressMessage = progressMessage;
   }

   public boolean isRetry() {
      return retry;
   }

   public void setRetry(boolean retry) {
      this.retry = retry;
   }

   @Override
   public String toString() {
      return "TaskEntity [getWorkDir()=" + getWorkDir() + ", getCmdArrayJson()="
            + getCmdArrayJson() + ", getStatus()=" + getStatus() + ", getProgress()="
            + getProgress() + ", getId()=" + getId() + "]";
   }

   public static TaskEntity findById(final Long taskId) {
      return DAL.autoTransactionDo(new Saveable<TaskEntity>() {
         @Override
         public TaskEntity body() throws Exception {
            return DAL.findById(TaskEntity.class, taskId);
         }
      });
   }

   public static List<TaskEntity> findAllByStatus(final Status[] anyStatus,
         final String exceptCookie) {
      return DAL.autoTransactionDo(new Saveable<List<TaskEntity>>() {
         @Override
         public List<TaskEntity> body() throws Exception {
            Criterion crit = null;
            for (Status s : anyStatus) {
               if (crit == null) {
                  crit = Restrictions.eq("status", s);
               } else {
                  crit = Restrictions.or(crit, Restrictions.eq("status", s));
               }
            }

            AuAssert.check(crit != null);

            if (exceptCookie != null) {
               crit = Restrictions.and(crit, Restrictions.ne("cookie", exceptCookie));
            }

            return DAL.findByCriteria(TaskEntity.class, crit);
         }
      });
   }

   private static TaskEntity updateState(final Long taskId, final Status status,
         final Double progress, final String errorMessage, final String progressMessage) {
      return DAL.autoTransactionDo(new Saveable<TaskEntity>() {
         @Override
         public TaskEntity body() throws Exception {
            TaskEntity task = DAL.findById(TaskEntity.class, taskId);
            if (task == null) {
               throw BddException.NOT_FOUND("task", "" + taskId);
            }
            if (status != null) {
               task.setStatus(status);
            }
            if (progress != null) {
               task.setProgress(progress);
            }
            if (errorMessage != null) {
               task.setErrorMessage(errorMessage);
            }
            if (progressMessage != null) {
               task.setProgressMessage(progressMessage);
            }
            return task;
         }
      });
   }

   public static TaskEntity updateProgress(long taskId, double progress) {
      return updateState(taskId, null, progress, null, null);
   }

   public static TaskEntity updateProgress(long taskId, double progress, String progressMessage) {
      return updateState(taskId, null, progress, null, progressMessage);
   }

   public static TaskEntity updateStatus(long taskId, Status status, String errorMessage) {
      if (status == Status.SUCCESS) {
         return updateState(taskId, status, 1.0, null, null);
      } else {
         return updateState(taskId, status, null, errorMessage, null);
      }
   }

   public Type getType() {
      TaskListener taskListener = getTaskListener();
      if (taskListener instanceof VHMReceiveListener) {
         return Type.VHM;
      } else {
         return Type.INNER;
      }
   }

   public String getTarget() {
      return target;
   }

   public void setTarget(String target) {
      this.target = target;
   }
}
