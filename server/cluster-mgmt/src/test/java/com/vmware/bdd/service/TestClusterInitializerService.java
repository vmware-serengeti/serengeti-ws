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
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.service.impl.ClusterInitializerService;

/**
 * Author: Xiaoding Bian
 * Date: 8/12/13
 * Time: 6:29 PM
 */
public class TestClusterInitializerService {

   private static final Logger logger = Logger.getLogger(TestClusterInitializerService.class);
   private static ClusterInitializerService adjustDbService;

   @BeforeClass(groups = { "TestClusterInitializerService" })
   public static void setup() {
      adjustDbService = new ClusterInitializerService();

      List<ClusterEntity> clusters = new ArrayList<ClusterEntity>();
      ClusterEntity cluster01 = new ClusterEntity("cluster01");
      cluster01.setStatus(ClusterStatus.PROVISION_ERROR);
      clusters.add(cluster01);

      ClusterEntityManager clusterEntityManager = Mockito.mock(ClusterEntityManager.class);
      Mockito.when(clusterEntityManager.findAllClusters()).thenReturn(clusters);
      Mockito.doNothing().when(clusterEntityManager).update(cluster01);
      adjustDbService.setClusterEntityManager(clusterEntityManager);
   }

   @Test(groups = { "TestClusterInitializerService" })
   public void testTransformClusterStatus() {
      adjustDbService.transformClusterStatus(ClusterStatus.PROVISIONING, ClusterStatus.PROVISION_ERROR);
      ClusterEntity cluster = adjustDbService.getClusterEntityManager().findAllClusters().get(0);
      assertTrue(cluster.getStatus().equals(ClusterStatus.PROVISION_ERROR));
   }

}
