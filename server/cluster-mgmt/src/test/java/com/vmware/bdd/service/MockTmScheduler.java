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
package com.vmware.bdd.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import mockit.Mock;
import mockit.MockClass;

import org.apache.log4j.Logger;
import org.mockito.Mockito;

import com.google.gson.internal.Pair;
import com.vmware.aurora.composition.CreateVMFolderSP;
import com.vmware.aurora.composition.CreateVmSP;
import com.vmware.aurora.composition.DeleteVMFolderSP;
import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Priority;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.composition.concurrent.Scheduler.ProgressCallback;
import com.vmware.bdd.clone.spec.VmCreateSpec;
import com.vmware.bdd.service.sp.ConfigIOShareSP;
import com.vmware.bdd.service.sp.CreateResourcePoolSP;
import com.vmware.bdd.service.sp.DeleteVmByIdSP;
import com.vmware.bdd.service.sp.StartVmSP;
import com.vmware.bdd.service.sp.StopVmSP;
import com.vmware.vim.binding.vim.Folder;

@MockClass(realClass = Scheduler.class)
public class MockTmScheduler {
   private static final Logger logger = Logger.getLogger(MockTmScheduler.class);
   public enum VmOperation {
      CREATE_VM,
      CREATE_FOLDER,
      START_VM,
      STOP_VM,
      DELETE_VM,
      DELETE_FOLDER,
      RECONFIGURE_VM,
      CREATE_RP
   }

   private static Map<VmOperation, Boolean> flag = new HashMap<VmOperation, Boolean>();
   private static boolean resultIsNull = false;

   public static void cleanFlag() {
      flag.clear();
      resultIsNull = false;
   }

   public static boolean isResultIsNull() {
      return resultIsNull;
   }

   public static void setResultIsNull(boolean resultIsNull) {
      MockTmScheduler.resultIsNull = resultIsNull;
   }

   public synchronized static boolean getFlag(VmOperation operation) {
      if (operation == null) {
         return true;
      }
      if (flag.get(operation) == null) {
         return true;
      }
      return flag.get(operation);
   }

   public synchronized static void setFlag(VmOperation operation, boolean success) {
      flag.put(operation, success);
   }

   private static VmOperation getOperation(Callable<Void> sp) {
      if (sp instanceof CreateVMFolderSP) {
         return VmOperation.CREATE_FOLDER;
      } else if (sp instanceof StartVmSP) {
         return VmOperation.START_VM;
      } else if (sp instanceof StopVmSP) {
         return VmOperation.STOP_VM;
      } else if (sp instanceof DeleteVMFolderSP) {
         return VmOperation.DELETE_FOLDER;
      } else if (sp instanceof DeleteVmByIdSP) {
         return VmOperation.DELETE_VM;
      } else if (sp instanceof CreateVmSP) {
         return VmOperation.CREATE_VM;
      } else if (sp instanceof ConfigIOShareSP) {
         return VmOperation.RECONFIGURE_VM;
      } else if (sp instanceof CreateResourcePoolSP) {
         return VmOperation.CREATE_RP;
      } else {
         logger.error("unsupported vm opration: " + sp);
         return null;
      }
   }

   @Mock
   public static ExecutionResult[] executeStoredProcedures(Priority priority,
         Callable<Void>[] storedProcedures, ProgressCallback callback) throws InterruptedException {
      logger.info("mock method is invoked.");
      if (resultIsNull) {
         return null;
      }

      ExecutionResult[] result = new ExecutionResult[storedProcedures.length];
      for (int i = 0; i < result.length; i++) {
         VmOperation operation = getOperation(storedProcedures[i]);
         boolean flag = getFlag(operation);
         ExecutionResult r = null;
         if (flag) {
            r = new ExecutionResult(true, null);
            if (operation == VmOperation.CREATE_FOLDER) {
               CreateVMFolderSP sp = (CreateVMFolderSP)storedProcedures[i];
               setReturnFolder(sp);
            } else if (operation == VmOperation.CREATE_VM) {
               CreateVmSP sp = (CreateVmSP)storedProcedures[i];
               setReturnVM(sp);
            }
            r = new ExecutionResult(true, null);
         } else {
            r = new ExecutionResult(true, new Throwable("test failure"));
         }
         result[i] = r;
      }
      return result;
   }

   private static void setReturnFolder(CreateVMFolderSP sp) {
      try {
         Folder folder = Mockito.mock(Folder.class);
         List<Folder> folders = new ArrayList<Folder>();
         folders.add(folder);
         Field field = sp.getClass().getDeclaredField("folders");
         field.setAccessible(true);
         field.set(sp, folders);
      } catch (Exception e) {
         logger.error("set return value failed.", e);
      }
   }

   private static void setReturnVM(CreateVmSP sp) {
      try {
         VmCreateSpec spec = Mockito.mock(VmCreateSpec.class);
         Mockito.when(spec.getVmName()).thenReturn("cluster-worker-0");
         Mockito.when(spec.getVmId()).thenReturn("create-vm-succ");
         Field field = sp.getClass().getDeclaredField("targetVmSpec");
         field.setAccessible(true);
         field.set(sp, spec);
      } catch (Exception e) {
         logger.error("set return value failed.", e);
      }
   }

   @SuppressWarnings("unchecked")
   @Mock
   public static Pair<ExecutionResult, ExecutionResult>[] executeStoredProcedures(Priority priority,
         Pair<? extends Callable<Void>, ? extends Callable<Void>>[] storedProcedures,
         int numberOfFailuresAllowed, ProgressCallback callback) throws InterruptedException {
      logger.info("mock method is invoked.");
      if (resultIsNull) {
         return null;
      }
      VmOperation operation = getOperation(storedProcedures[0].first);
      boolean flag = getFlag(operation);
      Pair<ExecutionResult, ExecutionResult>[] result = new Pair[storedProcedures.length];
      for (int i = 0; i < result.length; i++) {
         ExecutionResult f = null;
         ExecutionResult s = null;
         if (flag) {
            if (operation == VmOperation.CREATE_VM) {
               CreateVmSP sp = (CreateVmSP)storedProcedures[i].first;
               setReturnVM(sp);
            }
            f = new ExecutionResult(true, null);
            s = new ExecutionResult(false, null);
         } else {
            f = new ExecutionResult(true, new Throwable("Mock failure"));
            s = new ExecutionResult(true, null);
         }
         result[i] = new Pair<ExecutionResult, ExecutionResult>(f, s);
      }
      return result;

   }
}
