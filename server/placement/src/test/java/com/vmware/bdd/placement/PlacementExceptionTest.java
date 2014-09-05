/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.exception.PlacementException;
import com.vmware.bdd.placement.util.PlacementUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test Case for placement exception
 */
public class PlacementExceptionTest {
   @Test
   public void testPLACEMENT_ERROR() {
      BaseNode placement1 = new BaseNode("hbase5-worker-0");
      placement1.setTargetHost("host1");
      BaseNode placement2 = new BaseNode("hbase5-worker-1");
      placement2.setTargetHost("host2");
      List<BaseNode> placedNodes = Arrays.asList(placement2, placement1);

      Map<String, List<String>> filteredHosts = new HashMap<>();
      filteredHosts.put(PlacementUtil.NETWORK_NAMES, Arrays.asList("defaultNetwork", "dhcp"));
      filteredHosts.put(PlacementUtil.NO_NETWORKS_HOSTS, Arrays.asList("hbase5-worker-01", "hbase5-worker-02"));

      filteredHosts.put(PlacementUtil.NO_DATASTORE_HOSTS, Arrays.asList("host5"));
      filteredHosts.put(PlacementUtil.NO_DATASTORE_HOSTS_NODE_GROUP, Arrays.asList("worker"));

      filteredHosts.put(PlacementUtil.OUT_OF_SYNC_HOSTS, Arrays.asList("host3", "host4"));

      PlacementException pe = PlacementException.PLACEMENT_ERROR(
            PlacementException.OUT_OF_STORAGE_ON_HOST("host1"), placedNodes, filteredHosts);
      System.out.println(pe.getMessage());

      Assert.assertEquals(pe.getMessage(),
            "Cannot find enough storage on host host1. Possible fixes:\n" +
            "You must synchronize the time of the following hosts [host3, host4] with the Serengeti Management Server to use them.\n" +
                  "You must add these hosts [hbase5-worker-01, hbase5-worker-02] to the network [defaultNetwork, dhcp] to use them.\n" +
                  "You must add datastores on these hosts [host5] to use them with the node group [worker].\n" +
                  "Current node placement plan:\n" +
                  "Node hbase5-worker-1 placed on host host2. Node hbase5-worker-0 placed on host host1. ");
   }
}
