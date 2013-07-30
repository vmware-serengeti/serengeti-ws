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

package com.vmware.aurora.composition;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import com.vmware.aurora.vc.VcTask;
import com.vmware.aurora.vc.VcTask.TaskType;
import com.vmware.aurora.vc.VcTaskMgr.IVcTaskBody;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

/**
 * Stored procedure to delete given VM folders.
 *
 * @author Xin Li (xinli)
 */

public class DeleteVMFolderSP implements Callable<Void> {
   private List<Folder> folders;
   private boolean force;
   private boolean abortOnError;

   /**
    * Delete given folders. If <tt>force</tt> is true, the folders will be deleted together with its
    * children (subfolder, VM, resource pool, etc), otherwise it won't be deleted.
    * Please note if there's VM powered on under the folder, it cannot be deleted.
    *
    * @param folders Folders to delete
    * @param force Whether to delete the folder together with its children.
    * @param abortOnError Folders given will be deleted in order, if error is met and <tt>abortOnError</tt>
    * is set to true, exception will be thrown immediately.
    */
   public DeleteVMFolderSP(List<Folder> folders, boolean force, boolean abortOnError) {
      this.folders = folders;
      this.force = force;
      this.abortOnError = abortOnError;
   }

   /**
    * Delete a single folder.
    * @param folder Folder to delete
    * @param force Whether to delete the folder together with its children.
    */
   public DeleteVMFolderSP(Folder folder, boolean force) {
      this(Arrays.asList(folder), force, true);
   }

   @Override
   public Void call() throws Exception {
      for(final Folder folder : folders) {
         if (!force) {
            ManagedObjectReference[] children = folder.getChildEntity();
            if (children != null && children.length > 0) {
               if (abortOnError) {
                  throw new Exception("Folder not empty");
               } else {
                  continue;
               }
            }
         }

         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }

            @Override
            protected Void body() throws Exception {
               VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
                  public VcTask body() throws Exception {
                     return new VcTask(TaskType.DeleteFolder, folder.destroy(), null);
                  }
               });
               task.waitForCompletion();
               return null;
            }
         });
      }
      return null;
   }
}
