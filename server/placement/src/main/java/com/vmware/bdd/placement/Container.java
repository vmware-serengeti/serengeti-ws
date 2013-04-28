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

package com.vmware.bdd.placement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.aurora.composition.DiskSchema.Disk;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.placement.entity.AbstractDatacenter;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractCluster;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractDatastore;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.entity.VirtualGroup;
import com.vmware.bdd.placement.entity.VirtualNode;
import com.vmware.bdd.placement.exception.PlacementException;
import com.vmware.bdd.placement.interfaces.IContainer;
import com.vmware.bdd.utils.AuAssert;

public class Container implements IContainer {
   static final Logger logger = Logger.getLogger(Container.class);

   private AbstractDatacenter dc;
   private BaseNode templateNode;
   private Map<String, String> hostToRackMap;

   public Container() {
      this.dc = new AbstractDatacenter("abstractDC");
   }

   public Container(AbstractDatacenter dc) {
      this.dc = dc;
   }

   @Override
   public void SetTemplateNode(BaseNode template) {
      AuAssert.check(template.getDisks() != null
            && template.getDisks().size() == 1);
      this.templateNode = template;
   }

   public BaseNode getTemplateNode() {
      return templateNode;
   }

   @Override
   public void addResource(VcCluster cluster) {
      AuAssert.check(this.dc.findAbstractCluster(cluster.getName()) == null);
      logger.info("add vc cluster " + cluster.getName() + " to container");

      // translate datastores
      for (VcDatastore datastore : cluster.getAllDatastores()) {
         if (!datastore.isAccessible() 
               || !datastore.isInNormalMode()) {
            logger.info("datastore " + datastore.getName() 
                  + " is inaccessible or in maintanence mode. Ignore it.");
            continue;
         }
         if (this.dc.findAbstractDatastore(datastore.getName()) == null) {
            AbstractDatastore ds = new AbstractDatastore(datastore.getName());
            ds.setFreeSpace((int) (datastore.getFreeSpace() / (1024 * 1024 * 1024)));
            this.dc.addDatastore(ds);

            logger.info("added datastore " + ds.getName() + " with space "
                  + ds.getFreeSpace() + " to datacenter");
         }
      }

      // translate cluster and hosts
      AbstractCluster abstractCluster = new AbstractCluster(cluster.getName());

      try {
         // add hosts
         for (VcHost host : cluster.getHosts()) {
            if (host.isConnected() && !host.isInMaintenanceMode() 
                  && host.getDatastores() != null && host.getDatastores().size() > 0) {
               AbstractHost abstractHost = new AbstractHost(host.getName());
               for (VcDatastore datastore : host.getDatastores()) {
                  AbstractDatastore ds =
                        this.dc.findAbstractDatastore(datastore.getName());
                  if (ds != null) {
                     abstractHost.addDatastore(ds);
                     logger.info("added datastore " + ds.getName() + " to host "
                           + host.getName());
                  }
               }
               abstractCluster.addHost(abstractHost);
               logger.info("added host " + host.getName() + " to container");
            }
         }

         // add datastores
         for (VcDatastore datastore : cluster.getAllDatastores()) {
            AbstractDatastore ds =
                  this.dc.findAbstractDatastore(datastore.getName());
            if (ds != null) {
               abstractCluster.addDatastore(ds);
               logger.info("added datastore " + ds.getName() + " with space "
                     + ds.getFreeSpace() + " to cluster " + cluster.getName());
            }
         }

         this.dc.addCluster(abstractCluster);
      } catch (Exception e) {
         logger.error("Internal Error " + e.getMessage());
         throw BddException.INTERNAL(e, "failed to list VC Hosts");
      }
   }

   @Override
   public List<AbstractHost> getValidHosts(VirtualNode vNode, String rack) {
      logger.info("get valid vc hosts on rack " + rack);
      VirtualGroup vGroup = vNode.getParent();
      List<AbstractCluster> sharedClusters = vGroup.getJointAbstractClusters();

      if (sharedClusters == null || sharedClusters.size() == 0)
         throw PlacementException.DO_NOT_HAVE_SHARED_VC_CLUSTER(vNode
               .getBaseNodeNames());
      AuAssert.check(sharedClusters != null);

      List<AbstractHost> candidates = new ArrayList<AbstractHost>();

      for (AbstractCluster cluster : sharedClusters) {
         /*
          *  retrieve the cluster object, as the AbstractCluster object from getJointAbstractClusters
          *  is made up.
          */
         String vcClusterName = cluster.getName();
         cluster = this.dc.findAbstractCluster(cluster.getName());
         if (cluster == null) {
            logger.warn("VC Cluster " + vcClusterName
                  + " specified in the cluster spec is not found in VC");
            continue;
         }

         for (AbstractHost host : cluster.getHosts()) {
            if (rack != null && !rack.equals(getRack(host))) {
               logger.info("pass host " + host.getName()
                     + " as it's not on the target rack " + rack);
               continue;
            }
            if (vNode.hasEnoughCpu(host) && vNode.hasEnoughMemory(host)
                  && vNode.hasEnoughStorage(host)) {

               logger.info("host "
                     + host.getName()
                     + " is estimated to have enough resource to place virtual node "
                     + vNode.getBaseNodeNames());
               candidates.add(host);
            }
         }
      }

      return candidates;
   }

   @Override
   public void allocate(VirtualNode vNode, AbstractHost host) {
      for (BaseNode node : vNode.getBaseNodes()) {
         // system disk
         this.dc.getDatastore(node.getTargetDs()).allocate(
               node.getSystemDiskSize());
         // other disks
         for (Disk disk : node.getVmSchema().diskSchema.getDisks()) {
            this.dc.getDatastore(disk.datastore).allocate(
                  disk.initialSizeMB / 1024);
         }
      }
   }

   @Override
   public void free(VirtualNode vNode, AbstractHost host) {
      // TODO Auto-generated method stub

   }

   @Override
   public void addRackMap(Map<String, String> hostToRackMap) {
      AuAssert.check(hostToRackMap != null && hostToRackMap.size() != 0);
      this.hostToRackMap = hostToRackMap;
   }

   @Override
   public String getRack(AbstractHost host) {
      if (hostToRackMap == null || !hostToRackMap.containsKey(host.getName()))
         return null;

      return hostToRackMap.get(host.getName());
   }

   @Override
   public List<String> getRacks() {
      Set<String> racks = new HashSet<String>();

      for (AbstractHost host : getAllHosts()) {
         if (getRack(host) != null) {
            racks.add(getRack(host));
         }
      }

      return new ArrayList<String>(racks);
   }

   @Override
   public List<AbstractHost> getAllHosts() {
      return this.dc.getAllHosts();
   }

   @Override
   public Map<String, String> getRackMap() {
      return this.hostToRackMap;
   }
}