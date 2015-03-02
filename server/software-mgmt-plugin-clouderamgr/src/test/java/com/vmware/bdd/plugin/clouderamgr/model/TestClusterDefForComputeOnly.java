package com.vmware.bdd.plugin.clouderamgr.model;

import java.io.IOException;

import junit.framework.Assert;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.utils.SerialUtils;
import com.vmware.bdd.utils.CommonUtil;

public class TestClusterDefForComputeOnly {

   private static ClusterBlueprint blueprint = null;

   @BeforeClass(groups = { "TestClusterDefForComputeOnly" }, dependsOnGroups = {"TestAvailableServiceRoleContainer"})
   public static void setup() throws IOException {

      String content = CommonUtil.readJsonFile("compute_only_blueprint.json");

      blueprint = SerialUtils.getObjectByJsonString(ClusterBlueprint.class, content);
   }

   @Test(groups = { "TestClusterDefForComputeOnly" })
   public void testBluePrintToCmCluster() throws IOException {
      blueprint.getHadoopStack().setDistro("CDH-5.2.0");

      CmClusterDef clusterDef = new CmClusterDef(blueprint);
      Assert.assertTrue(clusterDef.getVersion().equals("CDH5"));
      Assert.assertTrue(clusterDef.getFullVersion().equals("5.2.0"));

      Assert.assertTrue(clusterDef.allServiceNames().size() == 2);
      Assert.assertTrue(clusterDef.allServiceNames().contains("cluster01_ISILON"));
      Assert.assertTrue(clusterDef.allServiceTypes().size() == 2);
      Assert.assertTrue(clusterDef.allServiceTypes().contains("ISILON"));

      for (CmServiceDef serviceDef : clusterDef.getServices()) {
         if (serviceDef.getType().getDisplayName().equals("YARN")) {
            Assert.assertNotNull(serviceDef.getConfiguration().get("hdfs_service"));
            Assert.assertNotNull(serviceDef.getConfiguration().get("admin_application_list_settings"));
         }
      }
   }

   @Test(groups = { "TestClusterDefForComputeOnly" })
   public void testIsComputeOnly() throws IOException {
      CmClusterDef clusterDef = new CmClusterDef(blueprint);
      Assert.assertTrue(clusterDef.isComputeOnly());
   }
}
