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
package com.vmware.bdd.manager;

import java.util.Map;

import org.testng.annotations.Test;

import com.vmware.bdd.manager.task.TaskListener;
import com.vmware.bdd.utils.ClusterCmdUtil;

@Test(enabled=false)
class TestEntity {
   private String name;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }
}

@Test(enabled=false)
public class TestListener implements TaskListener {
   private static final long serialVersionUID = 1L;
   private static TestEntity entity;

   public String getEntityName() {
      return entity.getName();
   }

   public void onSuccess() {
      System.out.println("task success!");
   }

   public void onFailure() {
      System.out.println("task failed!");
   }

   public void onMessage(Map<String, Object> mMap) {
      System.out.println("get message");
   }

   public String[] getTaskCommand(String clusterName, String fileName) {
      return ClusterCmdUtil.getCreateClusterCmdArray(clusterName, fileName);
   }
}
