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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.fault.DuplicateName;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.types.VmodlTypeMap;

/**
 * Stored procedure to create VM folder(s) in a VC data centre.
 *
 * This stored procedure can create VM folders, under the top VM folder of a datacenter, down to any level.
 * If some intermediate folders already exist, it will ignore exception when trying to create them, and continue.
 * If any other exception is met, it will abort and rethrow the exception,
 * folders created already won't get deleted.
 *
 * If executed successfully, all folders along the path will be recorded.
 *
 * @author Xin Li (xinli)
 *
 */

public class CreateVMFolderSP implements Callable<Void> {
   private VcDatacenter dc;
   private List<String> folderNames;
   private List<Folder> folders;
   private Folder startPoint;

   /**
    * Create VM folder(s) inside a datacenter. Intermediate folders will be created if needed.
    * @param dc
    * @param startPoint Optional, if null, the start point is the top VM folder of the datacenter.
    * @param folderNames List of folder names.
    */
   public CreateVMFolderSP(VcDatacenter dc, Folder startPoint, List<String> folderNames) {
      this.dc = dc;
      this.startPoint = startPoint;
      this.folderNames = folderNames;
   }

   private Folder getChildFolder(Folder folder, String name) throws Exception {
      ManagedObjectReference[] children = folder.getChildEntity();
      for (ManagedObjectReference ref : children) {
         if (VmodlTypeMap.Factory.getTypeMap().getVmodlType(Folder.class).getWsdlName().equals(ref.getType())) {
            Folder child = MoUtil.getManagedObject(ref);
            if (child.getName().equals(name)) {
               return child;
            }
         }
      }
      return null;
   }

   @Override
   public Void call() throws Exception {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected boolean isTaskSession() {
            return false;
         }

         @Override
         protected Void body() throws Exception {
            folders = new ArrayList<Folder>(folderNames.size());
            Folder folder = startPoint == null ? dc.getVmFolder() : startPoint;
            for (String folderName : folderNames) {
               try {
                  Folder child = getChildFolder(folder, folderName);
                  if (child == null) {
                     folder = MoUtil.getManagedObject(folder.createFolder(folderName));
                  } else {
                     folder = child;
                  }
                  folders.add(folder);
               } catch (DuplicateName ex) {
                  Folder child = getChildFolder(folder, folderName);
                  if (child == null) {
                     throw new Exception("Folder " + folderName + " should exist, but cannot be found");
                  } else {
                     folder = child;
                     folders.add(folder);
                  }
               }
            }
            return null;
         }
      });
      return null;
   }

   public List<Folder> getResult() {
      return folders;
   }
}
