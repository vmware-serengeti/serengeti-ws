/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

package com.vmware.bdd.placement.interfaces;

import java.util.List;
import java.util.Map;

import com.vmware.aurora.vc.VcCluster;
import com.vmware.bdd.placement.entity.AbstractDatacenter;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.entity.VirtualGroup;
import com.vmware.bdd.placement.entity.VirtualNode;

public interface IContainer {

   /**
    * set the template node info
    *
    * all nodes clone the system disk from the template node, thus we need to
    * know the size of system disk and count it in during the disk placement
    * process
    *
    * @param template
    */
   public void SetTemplateNode(BaseNode template);

   /**
    * add resource into the container
    *
    * the container combines all available (user added) vc clusters as a pool.
    * The placement planner executes its algorithm and deduct resources (the
    * storage space) from each host by invoking the allocate method.
    *
    * VC resource pools are considered only as a vm directory in Serengeti, we
    * don't really check cpu/mem availability for them and ignore them in the
    * placement algorithm
    *
    * @param cluster
    *           vc cluster
    */
   public void addResource(VcCluster cluster);

   /**
    * filter out abstract hosts that have enough resource for a virtual node
    *
    * only consider storage space
    *
    * @param vNode
    *           virtual node
    * @param rack
    *           if rack is null, get all hosts, otherwise get specific hosts on
    *           that rack
    * @return list of abstract hosts that have enough resource
    */
   public List<AbstractHost> getValidHosts(VirtualNode vNode, String rack);

   /**
    * get hosts that are filtered out because of datastore specified for
    * a virtual group
    *
    * @param vGroup
    *           virtual node
    * @return list of abstract host names that have zero disk space
    */
   public List<String> getDsFilteredOutHosts(VirtualGroup vGroup);

   /**
    * allocate resource for the virtual node
    *
    * @param vNode
    * @param host
    */
   public void allocate(VirtualNode vNode, AbstractHost host);

   /**
    * free the resource for allocated for this virtual node
    *
    * @param vNode
    * @param host
    */
   public void free(VirtualNode vNode, AbstractHost host);

   /**
    * add rack map into the container
    *
    * @param hostToRackMap
    */
   public void addRackMap(Map<String, String> hostToRackMap);

   /**
    * get the rack name for the input host
    *
    * @param host
    * @return
    */
   public String getRack(AbstractHost host);

   /**
    * get all racks
    *
    * @return
    */
   public List<String> getRacks();

   /**
    * get all hosts
    *
    * @return
    */
   public List<AbstractHost> getAllHosts();

   /**
    * get host to rack map
    *
    * @return
    */
   public Map<String, String> getRackMap();

   /**
    * remove a host
    *
    * @param hostname
    */
   public void removeHost(String hostname);

   public AbstractDatacenter getDc();
}
