/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.usermgmt.mocks;

import java.util.HashMap;
import java.util.Map;

import com.vmware.bdd.usermgmt.persist.MgmtVmCfgEao;

/**
 * Created By xiaoliangl on 12/19/14.
 */
public class MgmtVmCfgEaoMock extends MgmtVmCfgEao {
   private HashMap<String, String> map = new HashMap<>();

   public MgmtVmCfgEaoMock(Map<String, String> initMap) {
      map.putAll(initMap);
   }

   @Override
   public Map<String, String> findAll() {
      return map;
   }

   @Override
   public void update(Map<String, String> newConfig) {
      map.putAll(newConfig);
   }
}
