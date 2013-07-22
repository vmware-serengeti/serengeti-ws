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
package com.vmware.bdd.apitypes;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.spectypes.HadoopRole;

public class NodeGroupCreateTest {

   @Test
   public void testCalculateHostNum() {
      NodeGroupCreate group = new NodeGroupCreate();
      group.setInstanceNum(10);
      PlacementPolicy placementPolicy = new PlacementPolicy();
      placementPolicy.setInstancePerHost(3);
      group.setPlacementPolicies(placementPolicy);
      assertEquals(-1, group.calculateHostNum().intValue());
      group.setInstanceNum(9);
      assertEquals(3, group.calculateHostNum().intValue());
   }

   @Test
   public void testGetReferredGroup() {
      NodeGroupCreate compute = new NodeGroupCreate();
      PlacementPolicy computePlacementPolicy = new PlacementPolicy();
      GroupAssociation computeGroupAssociation = new GroupAssociation();
      computeGroupAssociation.setReference("data");
      computePlacementPolicy.setGroupAssociations(Arrays
            .asList(computeGroupAssociation));
      compute.setPlacementPolicies(computePlacementPolicy);
      assertEquals("data", compute.getReferredGroup());
   }

   @Test
   public void testIsStrictReferred() {
      NodeGroupCreate compute = new NodeGroupCreate();
      PlacementPolicy computePlacementPolicy = new PlacementPolicy();
      GroupAssociation computeGroupAssociation = new GroupAssociation();
      computeGroupAssociation.setReference("data");
      computePlacementPolicy.setGroupAssociations(Arrays
            .asList(computeGroupAssociation));
      compute.setPlacementPolicies(computePlacementPolicy);
      assertEquals(false, compute.isStrictReferred());
      computeGroupAssociation.setType(GroupAssociationType.STRICT);
      computePlacementPolicy.setGroupAssociations(Arrays
            .asList(computeGroupAssociation));
      compute.setPlacementPolicies(computePlacementPolicy);
      assertEquals(true, compute.isStrictReferred());
   }

   @Test
   public void testInstancePerHost() {
      NodeGroupCreate group = new NodeGroupCreate();
      PlacementPolicy placementPolicy = new PlacementPolicy();
      placementPolicy.setInstancePerHost(3);
      group.setPlacementPolicies(placementPolicy);
      assertEquals(placementPolicy.getInstancePerHost(),
            group.instancePerHost());
   }

   @Test
   public void testIsComputeOnlyGroup() {
      NodeGroupCreate worker = new NodeGroupCreate();
      worker.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString()));
      assertEquals(true, worker.isComputeOnlyGroup());
      worker.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString(),
            HadoopRole.TEMPFS_CLIENT_ROLE.toString()));
      assertEquals(true, worker.isComputeOnlyGroup());
      worker.setRoles(Arrays.asList(HadoopRole.MAPR_TASKTRACKER_ROLE.toString()));
      assertEquals(true, worker.isComputeOnlyGroup());
      worker.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString(),
            HadoopRole.HADOOP_DATANODE.toString()));
      assertEquals(false, worker.isComputeOnlyGroup());
   }

   @SuppressWarnings("static-access")
   @Test
   public void testGetImagestoreNamePattern() {
      ClusterCreate cluster = new ClusterCreate();
      NodeGroupCreate worker = new NodeGroupCreate();
      StorageRead storage = new StorageRead();
      storage.setType("shared");
      storage.setImagestoreNamePattern(Arrays.asList("st_imagestore"));
      worker.setStorage(storage);
      Set<String> sharedDatastorePattern = new HashSet<String>();
      sharedDatastorePattern.add("cluster_shared*");
      Set<String> localDatastorePattern = new HashSet<String>();
      localDatastorePattern.add("cluster_local*");
      cluster.setSharedDatastorePattern(sharedDatastorePattern);
      cluster.setLocalDatastorePattern(localDatastorePattern);
      cluster.setNodeGroups(new NodeGroupCreate[] { worker });
      assertEquals("st_imagestore",
            worker.getImagestoreNamePattern(cluster, worker)[0]);
      storage.setImagestoreNamePattern(null);
      storage.setDiskstoreNamePattern(Arrays.asList("st_diskstore"));
      worker.setStorage(storage);
      assertEquals("st_diskstore",
            worker.getImagestoreNamePattern(cluster, worker)[0]);
      storage.setDiskstoreNamePattern(null);
      worker.setStorage(storage);
      assertEquals("cluster_shared.*",
            worker.getImagestoreNamePattern(cluster, worker)[0]);
      storage.setType("");
      assertEquals("cluster_local.*",
            worker.getImagestoreNamePattern(cluster, worker)[0]);
      localDatastorePattern.clear();
      cluster.setLocalDatastorePattern(localDatastorePattern);
      assertEquals("cluster_shared.*",
            worker.getImagestoreNamePattern(cluster, worker)[0]);
   }

}
