/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.manager.mocks;

import com.vmware.bdd.plugin.ironfan.impl.DefaultSoftwareManagerImpl;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManagerFactory;

/**
 * Created By xiaoliangl on 9/3/14.
 */
public class FooSWMgrFactory implements SoftwareManagerFactory {
   @Override
   public SoftwareManager getSoftwareManager(final String URL, String username, char[] password, String certificate) {
      return new DefaultSoftwareManagerImpl() {

         public String getName() {
            return "fooAppMgr";
         }

         public String getType() {
            return "fooAppMgr";
         }
      };
   }
}
