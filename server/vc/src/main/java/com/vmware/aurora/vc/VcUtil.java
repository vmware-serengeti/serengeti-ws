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
package com.vmware.aurora.vc;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.CommonUtil;
import com.vmware.aurora.vc.VcTask.TaskType;
import com.vmware.aurora.vc.VcTaskMgr.IVcTaskBody;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.impl.vim.alarm.AlarmSettingImpl;
import com.vmware.vim.binding.impl.vim.alarm.AlarmSpecImpl;
import com.vmware.vim.binding.impl.vim.alarm.AlarmTriggeringActionImpl;
import com.vmware.vim.binding.impl.vim.alarm.EventAlarmExpressionImpl;
import com.vmware.vim.binding.impl.vim.alarm.OrAlarmExpressionImpl;
import com.vmware.vim.binding.impl.vmodl.TypeNameImpl;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.ManagedEntity;
import com.vmware.vim.binding.vim.ManagedEntity.Status;
import com.vmware.vim.binding.vim.ResourceAllocationInfo;
import com.vmware.vim.binding.vim.alarm.Alarm;
import com.vmware.vim.binding.vim.alarm.AlarmExpression;
import com.vmware.vim.binding.vim.alarm.AlarmManager;
import com.vmware.vim.binding.vim.alarm.AlarmSetting;
import com.vmware.vim.binding.vim.alarm.AlarmSpec;
import com.vmware.vim.binding.vim.alarm.AlarmTriggeringAction;
import com.vmware.vim.binding.vim.alarm.AlarmTriggeringAction.TransitionSpec;
import com.vmware.vim.binding.vim.alarm.EventAlarmExpression;
import com.vmware.vim.binding.vim.alarm.OrAlarmExpression;
import com.vmware.vim.binding.vim.fault.DuplicateName;
import com.vmware.vim.binding.vim.fault.InvalidName;
import com.vmware.vim.binding.vim.host.DateTimeSystem;
import com.vmware.vim.binding.vim.vm.ConfigSpec;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.HostCommunication;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

/**
 * VC related static functions.
 */
public class VcUtil {
   private static final Logger logger = Logger.getLogger(VcUtil.class);

   /**
    * Checks if the allocation info is valid for a resource bundle RP.
    *
    * @param cpu
    *           The cpu alloc info.
    * @param mem
    *           The mem alloc info.
    * @return reasons for incompatible.
    */
   protected static List<String> getCPUMemAllocIncompatReasons(
         ResourceAllocationInfo cpu, ResourceAllocationInfo mem) {
      List<String> reasons = new ArrayList<String>();
      if (!Configuration.getBoolean("vc.skipRpCheck", false)) {
         CommonUtil.checkCond((cpu.getLimit().equals(cpu.getReservation())),
               reasons, "CPU limit is not equal to reservation.");
         CommonUtil.checkCond((mem.getLimit().equals(mem.getReservation())),
               reasons, "Memory limit is not equal to reservation.");
         CommonUtil.checkCond((cpu.getReservation() > 0), reasons,
               "CPU reservation is equal to zero.");
         CommonUtil.checkCond((mem.getReservation() > 0), reasons,
               "Memory reservation is equal to zero.");
      } // else return empty reasons list
      return reasons;
   }

   public static List<String> getIncompatReasonsForSysRp(VcResourcePool sysRp) {
      List<String> reasons = new ArrayList<String>();
      ResourceAllocationInfo cpu = sysRp.getCpuAllocationInfo();
      ResourceAllocationInfo mem = sysRp.getMemAllocationInfo();
      if (sysRp.isRootRP()) {
         CommonUtil.checkCond((cpu.getReservation() > 0), reasons,
               "No available CPU resource.");
         CommonUtil.checkCond((mem.getReservation() > 0), reasons,
               "No available Memory resource.");
      } else {
         CommonUtil
               .checkCond((cpu.getReservation() > 0 || (cpu
                     .getExpandableReservation() && cpu.getLimit() > 0)),
                     reasons, "No available CPU resource.");
         CommonUtil
               .checkCond((mem.getReservation() > 0 || (mem
                     .getExpandableReservation() && mem.getLimit() > 0)),
                     reasons, "No available Memory resource.");
      }
      return reasons;
   }

   public static List<String> getIncompatReasonsForDatastore(
         VcDatastore datastore) {
      List<String> reasons = new ArrayList<String>();
      CommonUtil.checkCond(!datastore.isInStoragePod(), reasons,
            "The datastore can't be in storage pod.");
      CommonUtil
            .checkCond(
                  !datastore.isVmfs() || datastore.isSupportedVmfsVersion(),
                  reasons,
                  "The datastore file system is not supported. Data Director requires VMFS 5 or greater.");
      return reasons;
   }

   public static boolean isValidAllocationForResourceBundleRP(
         ResourceAllocationInfo cpu, ResourceAllocationInfo mem) {
      return getCPUMemAllocIncompatReasons(cpu, mem).isEmpty();
   }

   public static boolean isValidAllocationForResourceBundleRP(
         VcResourcePool rp, boolean forSysRb) {
      ResourceAllocationInfo cpu = rp.getCpuAllocationInfo();
      ResourceAllocationInfo mem = rp.getMemAllocationInfo();
      if (forSysRb) {
         return getIncompatReasonsForSysRp(rp).isEmpty();
      } else {
         return getCPUMemAllocIncompatReasons(cpu, mem).isEmpty();
      }
   }

   public static VcVirtualMachine createVm(final Folder parentFolder,
         final ConfigSpec spec, final VcResourcePool rp, final HostSystem host,
         final IVcTaskCallback callback) throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            ManagedObjectReference hostRef =
                  host != null ? host._getRef() : null;
            ManagedObjectReference taskRef =
                  parentFolder.createVm(spec, rp.getMoRef(), hostRef);
            return new VcTask(TaskType.CreateVm, taskRef, callback);
         }
      });
      task.waitForCompletion();
      return (VcVirtualMachine) task.getResult();
   }

   public static void processNotFoundException(ManagedObjectNotFound e,
         String moId, Logger logger) throws Exception {
      ManagedObjectReference moRef = e.getObj();
      if (MoUtil.morefToString(moRef).equals(moId)) {
         logger.error("VC object " + MoUtil.morefToString(moRef)
               + " is already deleted from VC. Purge from vc cache");
         // in case the event is lost
         VcCache.purge(moRef);
         ManagedObjectReference rpMoRef = VcCache.removeVmRpPair(moRef);
         if (rpMoRef != null) {
            VcCache.refresh(rpMoRef);
         }
      } else {
         throw e;
      }
   }

   public static void configureAlarm(Folder rootFolder) throws Exception {
      AlarmManager alarmManager = VcContext.getService().getAlarmManager();

      // Create the alarm for VHM
      String SERENGETI_UUID = rootFolder.getName(); /* should be the name of the folder clusters get deployed into */

      String ALARM_CLEARED_MSG = "all health issues previously reported by Big Data Extensions are in remission";
      EventAlarmExpression raiseExpression = new EventAlarmExpressionImpl();
      raiseExpression.setEventType(new TypeNameImpl("vim.event.EventEx"));
      raiseExpression.setEventTypeId("com.vmware.vhadoop.vhm.vc.events.warning");
      raiseExpression.setStatus(ManagedEntity.Status.yellow);
      raiseExpression.setObjectType(new TypeNameImpl("vim.VirtualMachine"));

      EventAlarmExpression clearExpression = new EventAlarmExpressionImpl();
      clearExpression.setEventType(new TypeNameImpl("vim.event.EventEx"));
      clearExpression.setEventTypeId("com.vmware.vhadoop.vhm.vc.events.info");
      clearExpression.setComparisons(new EventAlarmExpressionImpl.ComparisonImpl[] {
            new EventAlarmExpressionImpl.ComparisonImpl("message", "endsWith", ALARM_CLEARED_MSG)
            });
      clearExpression.setStatus(ManagedEntity.Status.green);
      clearExpression.setObjectType(new TypeNameImpl("vim.VirtualMachine"));

      OrAlarmExpression or = new OrAlarmExpressionImpl();
      or.setExpression(new AlarmExpression[] {raiseExpression, clearExpression});

      AlarmTriggeringAction alarmAction = new AlarmTriggeringActionImpl();
      alarmAction.setAction(null);
      TransitionSpec tSpec = new AlarmTriggeringActionImpl.TransitionSpecImpl();
      tSpec.setRepeats(false);
      tSpec.setStartState(Status.green);
      tSpec.setFinalState(Status.yellow);
      alarmAction.setTransitionSpecs(new TransitionSpec[] { tSpec });
      alarmAction.setGreen2yellow(true);

      AlarmSpec spec = new AlarmSpecImpl();
      spec.setActionFrequency(0);
      spec.setExpression(or);

      /* the name has to be unique, but we need a way to find any matching
      alarms later so we use a known prefix */
      String alarmName = "BDE Health " + SERENGETI_UUID;
      if (alarmName.length() > 80) {
         alarmName = alarmName.substring(0, 80);
      }
      spec.setName(alarmName);
      spec.setSystemName(null);
      spec.setDescription("Indicates a health issue with a compute VM managed by Big Data Extensions. The specific health issue is detailed in a warning event in the event log.");
      spec.setEnabled(true);

      AlarmSetting as = new AlarmSettingImpl();
      as.setReportingFrequency(0);
      as.setToleranceRange(0);

      spec.setSetting(as);

      ManagedObjectReference[] existingAlarms = alarmManager.getAlarm(rootFolder._getRef());
      Alarm existing = null;
      try {
         if (existingAlarms != null) {
            for (ManagedObjectReference m : existingAlarms) {
               Alarm a = MoUtil.getManagedObject(m);
               if (a.getInfo().getName().equals(alarmName)) {
                  existing = a;
                  break;
               }
            }
         }
      } catch (NullPointerException e) {
         // this just saves a lot of null checks
         logger.error("Got NullPointerException when querying alarms", e);
      }

      try {
         if (existing != null) {
            existing.reconfigure(spec);
            logger.info("Alarm " + alarmName + " exists");
         } else {
            ManagedObjectReference alarmMoref = alarmManager.create(rootFolder._getRef(), spec);
            logger.info("Create " + alarmMoref.getValue() + " " + alarmName);
         }
      } catch (InvalidName e) {
         logger.error("Invalid alarm name", e);
      } catch (DuplicateName e) {
         logger.error("Duplicate alarm name", e);
      }
   }

   public static boolean isRecoverableException(Throwable e) {
      return (e instanceof SSLPeerUnverifiedException
            || e instanceof SocketTimeoutException
            || e instanceof HostCommunication);
   }

   public static int getHostTimeDiffInSec(VcHost vcHost) throws Exception {
      HostSystem hostSystem = (HostSystem) vcHost.getManagedObject();
      ManagedObjectReference ref = hostSystem.getConfigManager().getDateTimeSystem();
      DateTimeSystem dateTimeSystem = MoUtil.getManagedObject(ref);

      return (int)(dateTimeSystem.queryDateTime().getTimeInMillis() - System.currentTimeMillis())/1000;
   }

}
