package com.vmware.bdd.service;

import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.impl.ClusterInitializerService;

/**
 * Author: Xiaoding Bian
 * Date: 8/12/13
 * Time: 6:29 PM
 */
public class TestClusterInitializerService {

   private static final Logger logger = Logger.getLogger(TestClusterInitializerService.class);
   private static ClusterInitializerService clusterInitializerService;

   @BeforeClass(groups = { "TestClusterInitializerService" })
   public static void setup() {
      clusterInitializerService = new ClusterInitializerService();

      List<ClusterEntity> clusters = new ArrayList<ClusterEntity>();

      ClusterEntity cluster01 = new ClusterEntity("cluster01");
      cluster01.setStatus(ClusterStatus.PROVISION_ERROR);
      clusters.add(cluster01);

      ClusterEntity cluster02 = new ClusterEntity("cluster02");
      cluster02.setStatus(ClusterStatus.DELETING);
      clusters.add(cluster02);

      IClusterEntityManager clusterEntityManager = Mockito.mock(IClusterEntityManager.class);
      Mockito.when(clusterEntityManager.findAllClusters()).thenReturn(clusters);
      Mockito.doNothing().when(clusterEntityManager).update(cluster01);
      clusterInitializerService.setClusterEntityManager(clusterEntityManager);
   }

   @Test(groups = { "TestClusterInitializerService" })
   public void testTransformClusterStatus() {
      clusterInitializerService.transformClusterStatus();
      List<ClusterEntity> clusters = clusterInitializerService.getClusterEntityManager().findAllClusters();
      for (ClusterEntity clusterEntity : clusters) {
         if (clusterEntity.getName().equals("cluster01")) {
            assertTrue(clusterEntity.getStatus().equals(ClusterStatus.PROVISION_ERROR));
         }
         if (clusterEntity.getName().equals("cluster02")) {
            assertTrue(clusterEntity.getStatus().equals(ClusterStatus.ERROR));
         }
      }
   }
}
