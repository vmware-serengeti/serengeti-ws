/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;

import com.vmware.bdd.utils.AppConfigValidationFactory;
import com.vmware.bdd.utils.ValidateResult;

public class AppConfigValidationFactoryTest {
   private ClusterCreate cluster;
   @BeforeMethod
   public void setup(){
      cluster=new ClusterCreate();
      Map<String,Object> hadoopMap=new HashMap<String,Object>();
      Map<String,Object> fileMap=new HashMap<String,Object>();
      Map<String,Object> corePopertysMap=new HashMap<String,Object>();
      Map<String,Object> hdfsPopertysMap=new HashMap<String,Object>();
      Map<String,Object> mapredPopertysMap=new HashMap<String,Object>();
      corePopertysMap.put("hadoop.tmp.dir", "/tmp");
      hdfsPopertysMap.put("dfs.http.address", "localhost");
      mapredPopertysMap.put("mapred.job.tracker","127.0.1.2");
      fileMap.put("core-site.xml", corePopertysMap);
      fileMap.put("hdfs-site.xml", hdfsPopertysMap);
      fileMap.put("mapred-site.xml", mapredPopertysMap);
      hadoopMap.put("hadoop", fileMap);
      cluster.setConfiguration(hadoopMap);
      NodeGroupCreate nodeGroup=new NodeGroupCreate();
      hadoopMap=new HashMap<String,Object>();
      fileMap=new HashMap<String,Object>();
      corePopertysMap=new HashMap<String,Object>();
      hdfsPopertysMap=new HashMap<String,Object>();
      mapredPopertysMap=new HashMap<String,Object>();
      corePopertysMap.put("hadoop.tmp.dir", "/tmp");
      hdfsPopertysMap.put("dfs.namenode.test.level", 4);
      hdfsPopertysMap.put("dfs.namenode.logger.level", 5);
      mapredPopertysMap.put("mapred.cluster.map.memory.mb",200);
      fileMap.put("core-site.xml", corePopertysMap);
      fileMap.put("hdfs-site.xml", hdfsPopertysMap);
      fileMap.put("mapred-site.xml", mapredPopertysMap);
      hadoopMap.put("hadoop", fileMap);
      nodeGroup.setConfiguration(hadoopMap);
      cluster.setNodeGroups(new NodeGroupCreate[]{nodeGroup});
   }
   
   @Test
   public void testWhiteListHandle() {
      ValidateResult validateResult=AppConfigValidationFactory.whiteListHandle(cluster.getNodeGroups()[0].getConfiguration());
      assertEquals(validateResult.getType(),ValidateResult.Type.WHITE_LIST_INVALID_NAME);
      assertEquals(validateResult.getFailureNames().get(0), "dfs.namenode.test.level");
      assertEquals(validateResult.getFailureNames().get(1), "dfs.namenode.logger.level");
   }
   
   @Test
   public void testBlackListHandle() {
      ValidateResult validateResult=AppConfigValidationFactory.blackListHandle(cluster.getConfiguration());
      assertEquals(validateResult.getType(),ValidateResult.Type.NAME_IN_BLACK_LIST);
      assertEquals(validateResult.getFailureNames().get(0), "dfs.http.address");
      assertEquals(validateResult.getFailureNames().get(1), "mapred.job.tracker");
   }
   
}
