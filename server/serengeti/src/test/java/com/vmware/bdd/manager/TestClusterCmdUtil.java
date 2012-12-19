package com.vmware.bdd.manager;

import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import com.vmware.bdd.utils.ClusterCmdUtil;

public class TestClusterCmdUtil {
   @Test
   public void testGetQueryClusterCmdArray() {
      String[] cmd = ClusterCmdUtil.getQueryClusterCmdArray("abc", "spec");
      assertEquals(cmd.length, 5);
      assertTrue(cmd[2].equals("abc"));
      assertTrue(cmd[3].equals("spec"));
      assertTrue(cmd[4].equals("-V"));
   }

   @Test
   public void testGetCreateClusterCmdArray() {
      String[] cmd = ClusterCmdUtil.getCreateClusterCmdArray("abc", "spec");
      assertEquals(cmd.length, 5);
   }

   @Test
   public void testDeleteClusterCmdArray() {
      String[] cmd = ClusterCmdUtil.getDeleteClusterCmdArray("abc", "spec");
      assertEquals(cmd.length, 5);
   }

   @Test
   public void testStartClusterNodesCmdArray() {
      String[] cmd = ClusterCmdUtil.getStartClusterNodesCmdArray("abc", "spec");
      assertEquals(cmd.length, 5);
   }

   @Test
   public void testStopClusterNodesCmdArray() {
      String[] cmd = ClusterCmdUtil.getStopClusterNodesCmdArray("abc", "spec");
      assertEquals(cmd.length, 5);
   }

   @Test
   public void testUpdatetClusterCmdArray() {
      String[] cmd = ClusterCmdUtil.getUpdatetClusterCmdArray("abc", "spec");
      assertEquals(cmd.length, 5);
   }

   @Test
   public void testConfigureClusterCmdArray() {
      String[] cmd = ClusterCmdUtil.getConfigureClusterCmdArray("abc", "spec");
      assertEquals(cmd.length, 5);
   }

   @Test
   public void testNodeFullName() {
      String nodeName = ClusterCmdUtil.getFullNodeName("cluster", "group", null);
      assertTrue(nodeName.equals("cluster-group"));
   }
}