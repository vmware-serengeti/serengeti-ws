package com.vmware.bdd.placement;

import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.exception.PlacementException;
import com.vmware.bdd.placement.util.PlacementUtil;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 8/19/14.
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

      System.out.println(
      PlacementException.PLACEMENT_ERROR(PlacementException.OUT_OF_STORAGE_ON_HOST("host1"), placedNodes, filteredHosts).getMessage());

      System.out.println();
   }
}
