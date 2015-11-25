/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.manager;

import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.spectypes.VcCluster;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by xiaoliangl on 11/25/15.
 */
@Component
public class VcResourceManager {

   @Autowired
   private ClusterConfigManager clusterConfigMgr;

   /**
    * Get the dsNames to be used by the cluster
    */
   public List<String> getDsNamesToBeUsed(final List<String> specifiedDsNames) {
      if (specifiedDsNames == null || specifiedDsNames.isEmpty()) {
         List<String> allDsNames = new ArrayList<>();
         allDsNames.addAll(clusterConfigMgr.getDatastoreMgr().getAllDatastoreNames());
         return allDsNames;
      } else {
         return validateGivenDS(specifiedDsNames);
      }
   }

   public List<String> getRpNames(List<String> rpNames) {
      if(CollectionUtils.isEmpty(rpNames)) {
         List<String> newRpNameList = new ArrayList<>();
         newRpNameList.addAll(clusterConfigMgr.getRpMgr().getAllRPNames());
         return newRpNameList;
      }

      return rpNames;
   }

   private List<String> validateGivenDS(List<String> specifiedDsNames) {
      List<String> exitsDs = new ArrayList<String>();
      Set<String> allDs =
            clusterConfigMgr.getDatastoreMgr().getAllDatastoreNames();
      StringBuffer nonexistentDsNames = new StringBuffer();

      for (String dsName : specifiedDsNames) {
         if (!allDs.contains(dsName))
            nonexistentDsNames.append(dsName).append(",");
         else
            exitsDs.add(dsName);
      }

      if (nonexistentDsNames.length() > 0) {
         nonexistentDsNames.delete(nonexistentDsNames.length() - 1,
               nonexistentDsNames.length());
         throw VcProviderException
               .DATASTORE_NOT_FOUND(nonexistentDsNames.toString());
      }

      return exitsDs;
   }
}
