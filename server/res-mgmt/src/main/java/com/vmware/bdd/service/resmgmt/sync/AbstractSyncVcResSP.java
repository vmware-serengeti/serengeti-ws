/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.resmgmt.sync;

import com.vmware.aurora.vc.*;
import com.vmware.bdd.service.resmgmt.sync.filter.VcResourceFilters;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by xiaoliangl on 7/16/15.
 */
public abstract class AbstractSyncVcResSP implements Callable<List<AbstractSyncVcResSP>> {
   private final static Logger LOGGER = Logger.getLogger(AbstractSyncVcResSP.class);

   private VcResourceFilters vcResourceFilters = null;

   private boolean skipped = false;

   protected AbstractSyncVcResSP(boolean skippRefresh) {
      skipped = skippRefresh;
   }

   protected List<AbstractSyncVcResSP> getSyncResourceSpList(ManagedObjectReference[] moRefs) {
      List<AbstractSyncVcResSP> syncChildSpList = new ArrayList<>();
      for (ManagedObjectReference moRef : moRefs) {
         AbstractSyncVcResSP syncVcResSP = getSyncResourceSp(moRef);
         if(syncVcResSP != null) {
            syncChildSpList.add(syncVcResSP);
         }
      }

      return syncChildSpList;
   }

   protected List<AbstractSyncVcResSP> getSyncResourceSpList(Iterable<ManagedObjectReference> moRefs) {
      List<AbstractSyncVcResSP> syncChildSpList = new ArrayList<>();
      for (ManagedObjectReference moRef : moRefs) {

         AbstractSyncVcResSP syncVcResSP = getSyncResourceSp(moRef);
         if(syncVcResSP != null) {
            syncChildSpList.add(syncVcResSP);
         }
      }

      return syncChildSpList;
   }

   protected AbstractSyncVcResSP getSyncResourceSp(ManagedObjectReference moRef) {
      VcObject vcObject = VcCache.lookup(moRef);
      //Vc Object exists meaning the object has been loaded before.
      // otherwise it's a newly added vc resource.

      AbstractSyncVcResSP newSyncVcResSp = null;
      if (vcObject != null) {
         boolean isFiltered = vcResourceFilters != null && vcResourceFilters.isFiltered(vcObject);
         newSyncVcResSp = new SyncResourceSp(vcObject, isFiltered);
      } else {
         newSyncVcResSp = new SyncVcResourceSp(moRef);
      }

      if(newSyncVcResSp != null && vcResourceFilters != null) {
         newSyncVcResSp.setVcResourceFilters(vcResourceFilters);
      }

      return newSyncVcResSp;
   }

   /**
    *
    * @param vcResourceFilters filters to exclude vc resources from refresh.
    */
   public void setVcResourceFilters(VcResourceFilters vcResourceFilters) {
      this.vcResourceFilters = vcResourceFilters;
   }

   @Override
   public List<AbstractSyncVcResSP> call() throws Exception {
      //sync the current vc resource object
      VcObject vcObject = syncThis();

      if(!skipped && LOGGER.isDebugEnabled()) {
         if(vcObject instanceof VcDatastore) {
            LOGGER.debug(String.format("%1s freesize:%2sGB", vcObject.getName(), ((VcDatastore) vcObject).getFreeSpace()/1024/1024/1024));
         }
      }

      //build a list of task
      List<AbstractSyncVcResSP> syncChildSpList = new ArrayList<>();
      if (vcObject instanceof VcDatacenter) {
         if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("vc resource[%1s] is a datacenter, start to create sync children tasks.", vcObject.getId()));
         }
         //if it's a datacenter, refresh the clusters
         //resource pool is currently refreshed by event
         syncChildSpList.addAll(getSyncResourceSpList(((VcDatacenter) vcObject).getClusterMoRefs()));
      } else if (vcObject instanceof VcCluster) {
         VcCluster vcCluster = (VcCluster) vcObject;
         if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("vc resource[%1s] is a cluster, start to create sync children tasks.", vcObject.getId()));
         }

         // if it's a cluster, refresh the datastores, networks, hosts
         syncChildSpList.addAll(getSyncResourceSpList(vcCluster.getDataStoreMoRefs()));
         syncChildSpList.addAll(getSyncResourceSpList(vcCluster.getNetworkMoRefs()));
         syncChildSpList.addAll(getSyncResourceSpList(vcCluster.getHostMoRefs()));
      } else {
         if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("vc resource[%1s] has no child, no need to create sync children tasks.", vcObject.getId()));
         }
      }

      return syncChildSpList;
   }

   protected final boolean isSkipped() {
      return skipped;
   }

   protected abstract VcObject syncThis();

}
