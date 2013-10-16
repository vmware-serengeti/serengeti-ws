package com.vmware.bdd.service.sp;

import mockit.Mock;
import mockit.MockClass;

import org.mockito.Mockito;

import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.ClusterEntityManager;

@MockClass(realClass = ClusterEntityManager.class)
public class MockClusterEntityManager extends ClusterEntityManager {

   @Mock
   public NodeEntity getNodeByMobId(String vmId) {
      return Mockito.mock(NodeEntity.class);
   }

   @Mock
   public NodeEntity getNodeByVmName(String vmName) {
      return Mockito.mock(NodeEntity.class);
   }

   @Mock
   synchronized public void refreshNodeByMobId(String vmId, String action,
         boolean inSession) {
   }

   @Mock
   synchronized public void refreshNodeByVmName(String vmId, String vmName,
         String nodeAction, boolean inSession) {
   }
}
