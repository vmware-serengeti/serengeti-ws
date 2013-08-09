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
package com.vmware.aurora.vc.test;

import org.testng.annotations.Test;

import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcService;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

public class VcConnectionTest extends AbstractVcTest {
   
   
   //---------------------------------------------------------------------------
   // Private methods

   /**
    * Returns the first datacenter in the root folder.
    */
   private static Datacenter getDatacenter(VcService service) throws Exception {
      ManagedObjectReference rootFolderRef = service.getServiceInstanceContent().getRootFolder();
      
      Folder rootFolder = service.getManagedObject(rootFolderRef);
      ManagedObjectReference[] childEntities = rootFolder.getChildEntity();
      for (ManagedObjectReference child : childEntities) {
         ManagedObject mo = service.getManagedObject(child);
         if (mo instanceof Datacenter) {
            return (Datacenter) mo;
         }
      }
      return null;
   }
   
   /** 
    * Tests login into vc server
    * @throws Exception
    */ 
   @Test
   public void testLoginVc() throws Exception {
      System.out.println("Service url is: " + vcService.getServiceUrl());
      Datacenter dc = getDatacenter(vcService);
      AuAssert.check(vcService.isConnected());
      System.out.println("VC server guid is: " + VcContext.getServerGuid());
      System.out.println("datacenter: " + dc);
   }  
   
   /** 
    * Tests logout and then auto login into server
    * @throws Exception
    */ 
   @Test
   public void testLogout() throws Exception {
      AuAssert.check(vcService.isConnected());
      vcService.logout();
      AuAssert.check(!vcService.isConnected());
      Datacenter dc = getDatacenter(vcService);
      System.out.println("datacenter: " + dc);
      AuAssert.check(vcService.isConnected());
   }

}
