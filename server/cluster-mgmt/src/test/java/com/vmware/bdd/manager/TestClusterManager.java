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
package com.vmware.bdd.manager;

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mockit.Mock;
import mockit.MockUp;

import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.utils.CommonUtil;

public class TestClusterManager {

   @Test
   public void testWriteClusterSpecFileWithUTF8() {
      final String dirPath = "src/test/resources/";
      final String filePath = dirPath + "클러스터.json";
      new MockUp<ClusterManager>() {
         @Mock
         public Map<String, Object> getClusterConfigManifest(
               String clusterName, List<String> targets, boolean needAllocIp) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            ClusterCreate cluster = new ClusterCreate();
            NodeGroupCreate master = new NodeGroupCreate();
            master.setName("마스터 노드");
            NodeGroupCreate worker = new NodeGroupCreate();
            worker.setName("协作节点");
            NodeGroupCreate client = new NodeGroupCreate();
            client.setName("クライアント");
            cluster.setNodeGroups(new NodeGroupCreate[] { master, worker,
                  client });
            attrs.put("cluster_definition", cluster);
            return attrs;
         }
      }.getMockInstance();
      ClusterManager clusterManager = new ClusterManager();
      File dir = new File(dirPath);
      clusterManager.writeClusterSpecFile("클러스터-主节点-01", dir, false);
      File file = new File(filePath);
      assertTrue(file.exists());
      String clusterJson = CommonUtil.readJsonFile(filePath);
      assertTrue(clusterJson.indexOf("마스터 노드") != 0);
      assertTrue(clusterJson.indexOf("协作节点") != 0);
      assertTrue(clusterJson.indexOf("クライアント") != 0);
      file.delete();
   }

}
