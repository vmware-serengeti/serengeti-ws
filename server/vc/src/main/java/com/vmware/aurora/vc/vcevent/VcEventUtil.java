/* ***************************************************************************
 * Copyright (c) 2011-2012 VMware, Inc.  All rights reserved.
 * -- VMware Confidential
 * ***************************************************************************/

/**
 * <code>VcEventUtil</code> implements vc event related utilities.<p>
 *
 * @since   0.6
 * @version 0.6
 * @author Boris Weissman
 */

package com.vmware.aurora.vc.vcevent;

import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;

import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.vim.Task;
import com.vmware.vim.binding.vim.TaskInfo;
import com.vmware.vim.binding.vim.TaskManager;
import com.vmware.vim.binding.vim.event.Event;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.Change;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.FilterUpdate;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.MissingObject;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.MissingProperty;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.ObjectUpdate;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.UpdateSet;

public class VcEventUtil {
   private static Logger logger = Logger.getLogger(VcEventUtil.class);

   /**
    * Helper function that dumps all events in the passed array.
    *
    * @param events
    */
   public static void dumpEvents(Event[] events) {
      for (Event e : events) {
         dumpEvent(e);
      }
   }

   /**
    * Dumps the first level of detail about the passed Event param.
    *
    * @param Event
    */
   private static void dumpEvent(Event e) {
      StringBuilder buf = new StringBuilder();

      buf.append("id: ").append(e.getKey()).append(" : ").append(e.getChainId()).append(" ");
      buf.append(getEventName(e)).append("\n");
      if (e.getDynamicType() != null) {
         buf.append(" type: ").append(e.getDynamicType());
      }
      buf.append("\t").append(e.getFullFormattedMessage()).append("\n");
      if (e.getDatacenter() != null) {
         buf.append("\tdc:   ").append(e.getDatacenter().getName())
               .append("\n");
      }
      if (e.getComputeResource() != null) {
         buf.append("\tcr:   ").append(e.getComputeResource().getName())
               .append("\n");
      }
      if (e.getHost() != null) {
         buf.append("\thost: ").append(e.getHost().getName()).append("\n");
      }
      if (e.getDs() != null) {
         buf.append("\tds:   ").append(e.getDs().getName()).append("\n");
      }
      if (e.getNet() != null) {
         buf.append("\tnet:  ").append(e.getNet().getName()).append("\n");
      }
      if (e.getVm() != null) {
         buf.append("\tvm:   ").append(e.getVm().getName()).append("\n");
      }
      if (e.getUserName() != null && !e.getUserName().equals("")) {
         buf.append("\tuser: ").append(e.getUserName()).append("\n");
      }
      if (e.getCreatedTime() != null) {
         SimpleDateFormat dateFormat =
               new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
         String s = dateFormat.format(e.getCreatedTime().getTime());
         buf.append("created: ").append(s).append("\n");
      }
      logger.debug(buf.toString());
   }

   static String getEventName(Event e) {
      String fullName = e.getClass().getName();
      String[] tokens = fullName.split("\\.");
      String className = tokens[tokens.length - 1];
      return className;
   }

   /**
    * Helper function to get the number of updates in the update set.
    *
    * @param updateSet
    *           UpdateSet to calculate.
    * @return count number of updates in the set
    */
   public static int getUpdateSetCount(UpdateSet updateSet) {
      int updateCount = 0;
      if (updateSet != null && updateSet.getFilterSet() != null) {
         for (FilterUpdate pfu : updateSet.getFilterSet()) {
            ObjectUpdate[] objectUpdates = pfu.getObjectSet();
            if (objectUpdates != null) {
               updateCount += objectUpdates.length;
            }
         }
      }
      return updateCount;
   }

   /**
    * Helper function to dump the update set.
    *
    * @param updateSet
    *           UpdateSet to calculate.
    * @param currentVersion
    *           version (prior to this UpdateSet)
    * @return count number of updates in the set
    */
   public static int dumpUpdateSet(UpdateSet update, String currentVersion) {
      int updateCount = 0;
      StringBuilder stringBuffer = new StringBuilder();
      try {
         if (update == null) {
            stringBuffer.append("UpdateSet is null for version '");
            stringBuffer.append(currentVersion);
            stringBuffer.append("' \n");
            return updateCount;
         }
         stringBuffer.append("Dumping UpdateSet between version '");
         stringBuffer.append(currentVersion);
         stringBuffer.append("' => '");
         stringBuffer.append(update.getVersion());
         stringBuffer.append("\n");
         if (update.getFilterSet() == null) {
            stringBuffer.append("PropertyFilterUpdate is null \n");
            return updateCount;
         }
         stringBuffer.append("UpdateSet has '");
         stringBuffer.append(update.getFilterSet().length);
         stringBuffer.append("' PropertyFilterUpdate changes.\n");
         for (FilterUpdate pfu : update.getFilterSet()) {
            stringBuffer.append("\tPropertyFilterUpdate filter => '");
            stringBuffer.append(pfu.getFilter().getValue());
            stringBuffer.append("' \n");
            try {
               if ((pfu.getMissingSet() != null)
                     && (pfu.getMissingSet().length > 0)) {
                  for (MissingObject mObject : pfu.getMissingSet()) {
                     stringBuffer.append("\tMissingObject: ");
                     stringBuffer.append("PropertyFilterUpdate for");
                     stringBuffer.append("ManagedObjectReference '");
                     stringBuffer.append(mObject.getObj().getValue());
                     stringBuffer.append("' failed with fault message '");
                     stringBuffer.append(mObject.getFault()
                           .getLocalizedMessage());
                     stringBuffer.append("' \n");
                  }
               }
               ObjectUpdate[] objectUpdates = pfu.getObjectSet();
               if (objectUpdates != null) {
                  updateCount += objectUpdates.length;
                  for (ObjectUpdate oUpdate : objectUpdates) {
                     boolean taskUpdate = false;
                     stringBuffer.append("\tObjectUpdate of kind => '");
                     stringBuffer.append(oUpdate.getKind().toString());
                     stringBuffer.append("', type => '");
                     stringBuffer.append(oUpdate.getObj().getType());
                     stringBuffer.append("' for object => '");
                     stringBuffer.append(oUpdate.getObj().getValue());
                     stringBuffer.append("' \n");
                     if (oUpdate.getObj().getType().equalsIgnoreCase("Task")) {
                        taskUpdate = true;
                     }
                     if (oUpdate.getChangeSet() != null) {
                        for (Change pChange : oUpdate.getChangeSet()) {
                           stringBuffer.append("\t\t'");
                           stringBuffer.append(pChange.getName());
                           stringBuffer.append("' => '");
                           stringBuffer.append(pChange.getVal());
                           stringBuffer.append("' (");
                           stringBuffer.append(pChange.getOp().toString());
                           stringBuffer.append(") \n");
                           if (taskUpdate) {
                              if (pChange.getName().equals("info")) {
                                 TaskInfo taskInfo =
                                       (TaskInfo) pChange.getVal();
                                 stringBuffer.append("\t\t\t");
                                 stringBuffer.append("taskMoRef: ");
                                 stringBuffer.append(taskInfo.getTask()
                                       .getValue());
                                 stringBuffer.append("\n");
                                 stringBuffer.append("state: ");
                                 stringBuffer.append(taskInfo.getState()
                                       .toString());
                                 stringBuffer.append("\n");
                                 stringBuffer.append("progress: ");
                                 stringBuffer.append(taskInfo.getProgress());
                                 stringBuffer.append("\n");
                              }
                           }
                        }
                     }
                     if (oUpdate.getMissingSet() != null) {
                        for (MissingProperty mProp : oUpdate.getMissingSet()) {
                           stringBuffer.append("\t'");
                           stringBuffer.append(mProp.getPath());
                           stringBuffer.append("' failed with fault ");
                           stringBuffer.append("message '");
                           stringBuffer.append(mProp.getFault()
                                 .getLocalizedMessage());
                           stringBuffer.append("' \n");
                        }
                     }
                  }
               }
            } catch (Exception e) {
               logger.error("PropertyFilterUpdate dump exception:");
            }
         }
      } finally {
         if (stringBuffer != null) {
            logger.debug(stringBuffer.toString());
         }
      }
      return updateCount;
   }

   /**
    * Dump recent tasks: the more restrictive of 10 minutes worth of tasks or
    * 200 tasks.
    *
    * @throws Exception
    */
   public static void dumpRecentTasks() throws Exception {
      TaskManager taskManager = VcContext.getService().getTaskManager();
      ManagedObjectReference taskMoRefs[] = taskManager.getRecentTask();
      for (ManagedObjectReference moRef : taskMoRefs) {
         Task task = MoUtil.getManagedObject(moRef);
         dumpTask(task);
      }
   }

   public static void dumpTask(Task task) {
      StringBuilder buf = new StringBuilder();
      TaskInfo info = task.getInfo();
      buf.append("\ttaskMoRef: ").append(info.getTask().getValue()).append("\n");
      buf.append("\t").append(info.getName()).append(" : ").
         append(info.getDescription()).append("\n");
      buf.append("\tstate: ").append(info.getState()).append("\n");
      buf.append("\tprogress: ").append(info.getProgress());
      logger.info(buf);
   }
}