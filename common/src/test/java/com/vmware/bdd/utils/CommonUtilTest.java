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
package com.vmware.bdd.utils;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.vmware.bdd.spectypes.HadoopRole;
import org.testng.annotations.Test;

public class CommonUtilTest {

   @Test
   public void testValidateName() {
      assertEquals(CommonUtil.validateName("name1"), true);
      assertEquals(CommonUtil.validateName("Name2"), true);
      assertEquals(CommonUtil.validateName("name3_"), true);
      assertEquals(CommonUtil.validateName("name4 "), true);
      assertEquals(CommonUtil.validateName("name-5"), true);
      assertEquals(CommonUtil.validateName("name6-"), true);
      assertEquals(CommonUtil.validateName("-name7-"), true);
   }

   @Test
   public void validatePortGroupName() {
      assertEquals(CommonUtil.validatePortGroupName("name1"), true);
      assertEquals(CommonUtil.validatePortGroupName("Name2"), true);
      assertEquals(CommonUtil.validatePortGroupName("name3_"), true);
      assertEquals(CommonUtil.validatePortGroupName("name4 "), true);
      assertEquals(CommonUtil.validatePortGroupName("name-5"), true);
      assertEquals(CommonUtil.validatePortGroupName("name6-"), true);
      assertEquals(CommonUtil.validatePortGroupName("-name7-"), true);
      assertEquals(CommonUtil.validatePortGroupName("VM network192.168.0.1"), true);
      assertEquals(CommonUtil.validatePortGroupName("192.168.0.2VM network"), true);
   }

   @Test
   public void testValidateClusterName() {
      assertEquals(CommonUtil.validateClusterName("clusterName1"), true);
      assertEquals(CommonUtil.validateClusterName("clusterName2"), true);
      assertEquals(CommonUtil.validateClusterName("clusterName3_"), true);
      assertEquals(CommonUtil.validateClusterName("clusterName4 "), false);
      assertEquals(CommonUtil.validateClusterName("clusterName-5"), false);
      assertEquals(CommonUtil.validateClusterName("clusterName6-"), false);
      assertEquals(CommonUtil.validateClusterName("-clusterName7"), false);
      assertEquals(CommonUtil.validateClusterName("cluster-Name8"), false);
      assertEquals(CommonUtil.validateClusterName("cluster Name9"), false);
   }

   @Test
   public void testValidateNodeGroupName() {
      assertEquals(CommonUtil.validateNodeGroupName("nodeGroupName1"), true);
      assertEquals(CommonUtil.validateNodeGroupName("nodeGroupName2"), true);
      assertEquals(CommonUtil.validateNodeGroupName("nodeGroupName3_"), false);
      assertEquals(CommonUtil.validateNodeGroupName("nodeGroupName4 "), false);
      assertEquals(CommonUtil.validateNodeGroupName("nodeGroupName-5"), false);
      assertEquals(CommonUtil.validateNodeGroupName("nodeGroupName6-"), false);
      assertEquals(CommonUtil.validateNodeGroupName("-nodeGroupName7"), false);
      assertEquals(CommonUtil.validateNodeGroupName("nodeGroup Name8"), false);
      assertEquals(CommonUtil.validateNodeGroupName("nodeGroup_Name9"), false);
      assertEquals(CommonUtil.validateNodeGroupName("_nodeGroupName10"), false);
      assertEquals(
            CommonUtil
                  .validateNodeGroupName("nodeGroupName1234567890123456789012345678901234567890123456789012345678901234567890"),
            false);
   }

   @Test
   public void testValidateVcDataStoreNames() {
      List<String> vcDataStoreNames = new ArrayList<String>();
      vcDataStoreNames.add("vcDataStore_Name1");
      vcDataStoreNames.add("vcDataStore_Nam*");
      vcDataStoreNames.add("vcDataStore_Nam?");
      vcDataStoreNames.add("vcData Store_Name2");
      vcDataStoreNames.add("vcDataStoreName3-");
      vcDataStoreNames.add("vcDataStoreName192.168.0.1");
      vcDataStoreNames.add("vcDataStoreName(192.168.0.1)");
      assertEquals(CommonUtil.validateVcDataStoreNames(vcDataStoreNames), true);

      List<String> errorVcDataStoreNames1 = new ArrayList<String>();
      errorVcDataStoreNames1.add("vcDataStoreName!");
      assertEquals(CommonUtil.validateVcDataStoreNames(errorVcDataStoreNames1), false);
      List<String> errorVcDataStoreNames2 = new ArrayList<String>();
      errorVcDataStoreNames2.add("vcDataStoreName#");
      assertEquals(CommonUtil.validateVcDataStoreNames(errorVcDataStoreNames2), false);
   }

   @Test
   public void testGetDatastoreJavaPattern() {
      String datastore = "(192.168.0.1)datastore";
      String pattern = CommonUtil.getDatastoreJavaPattern("(192.168.0.1)datasto?e");
      assertTrue(datastore.matches(pattern));
      pattern = CommonUtil.getDatastoreJavaPattern("(192.168.0.1)data*");
      assertTrue(datastore.matches(pattern));
   }

   @Test
   public void testIsComputeOnly() {
      List<String> roles = new ArrayList<String>();
      roles.add(HadoopRole.HADOOP_TASKTRACKER.toString());
      String distroVendor = "apache";
      assertTrue(CommonUtil.isComputeOnly(roles, distroVendor));
      roles.add(HadoopRole.TEMPFS_CLIENT_ROLE.toString());
      assertTrue(CommonUtil.isComputeOnly(roles, distroVendor));
      roles.add(HadoopRole.HADOOP_DATANODE.toString());
      assertTrue(!CommonUtil.isComputeOnly(roles, distroVendor));

      roles.clear();
      roles.add(HadoopRole.MAPR_TASKTRACKER_ROLE.toString());
      distroVendor = Constants.MAPR_VENDOR;
      assertTrue(CommonUtil.isComputeOnly(roles, distroVendor));
      roles.add(HadoopRole.MAPR_NFS_ROLE.toString());
      assertTrue(!CommonUtil.isComputeOnly(roles, distroVendor));
   }
   
   @Test
   public void testMakeVmMemoryDivisibleBy4() {
      long max = Long.MAX_VALUE;
      max = CommonUtil.makeVmMemoryDivisibleBy4(max);
      assertTrue(max > 0);
   }
}
