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

import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

/**
 * Test for conversion between java api types and json formats
 * 
 */
public class BeanToJsonTest {
   private Gson gson;

   @BeforeMethod
   public void setup() {
      gson = new Gson();
   }

   @Test
   public void testResourcePoolAdd() {
      ResourcePoolAdd rpAdd = new ResourcePoolAdd();
      rpAdd.setName("myRp");
      rpAdd.setVcClusterName("vc1");
      rpAdd.setResourcePoolName("rp1");


      //convert from bean to json
      String jsonString = gson.toJson(rpAdd);
      //		System.out.println("ResourcePool Add:");
      //		System.out.println(jsonString);

      //convert from json to bean
      rpAdd = gson.fromJson(jsonString, ResourcePoolAdd.class);
      assertEquals(rpAdd.getResourcePoolName(), "rp1");
      //System.out.println(rpAdd.getNames());
   }

   @Test
   public void testDataStoreAdd() {
      DatastoreAdd dsAdd = new DatastoreAdd();
      List<String> names = new ArrayList<String>();
      names.add("ds1");
      names.add("ds2");
      names.add("jun*");
      dsAdd.setSpec(names);


      //convert from bean to json
      String jsonString = gson.toJson(dsAdd);
      //                System.out.println("ResourcePool Add:");
      //                System.out.println(jsonString);

      //convert from json to bean
      dsAdd = gson.fromJson(jsonString, DatastoreAdd.class);
      assertEquals(dsAdd.getSpec().get(1), "ds2");
      //System.out.println(dsAdd.getNames());
   }

   @Test
   public void testClusterCreate() {
      ClusterCreate clusterCreate = new ClusterCreate();
      clusterCreate.setName("beijing");

      //convert from bean to json
      String jsonString = gson.toJson(clusterCreate);
      //      System.out.println("Cluster Create:");
      //      System.out.println(jsonString);

      //convert from json to bean
      clusterCreate = gson.fromJson(jsonString, ClusterCreate.class);
      assertEquals(clusterCreate.getName(), "beijing");
   }

   @Test
   public void testClusterRead() {
      ClusterRead clusterRead = new ClusterRead();
      clusterRead.setName("beijing");
      clusterRead.setDistro("CDH3");
      clusterRead.setInstanceNum(5);


      List<NodeGroupRead> nodeGroups = new ArrayList<NodeGroupRead>();

      //add namenode info
      NodeGroupRead nameNodeGroup = new NodeGroupRead();
      nameNodeGroup.setName("nnGroup");
      List<String> nameNodeRoles = new ArrayList<String>();
      nameNodeRoles.add("nn");
      nameNodeGroup.setRoles(nameNodeRoles);

      List<NodeRead> nameNodes = new ArrayList<NodeRead>();
      NodeRead nameNodeRead = new NodeRead();
      nameNodeRead.setHostName("bjNN");
      nameNodeRead.setIp("10.112.113.121");

      nameNodes.add(nameNodeRead);
      nameNodeGroup.setInstances(nameNodes);
      nodeGroups.add(nameNodeGroup);

      //add datanode/compute nodes info
      NodeGroupRead mixNodeGroup = new NodeGroupRead();
      mixNodeGroup.setName("mixedNodeGroup");
      List<String> roles = new ArrayList<String>();
      roles.add("dn");
      roles.add("cn");
      mixNodeGroup.setRoles(roles);

      List<NodeRead> mixNodes = new ArrayList<NodeRead>();
      NodeRead mixNodeRead1 = new NodeRead();
      mixNodeRead1.setHostName("bjDataComputeNode1");
      mixNodeRead1.setIp("10.112.113.122");

      NodeRead mixNodeRead2 = new NodeRead();
      mixNodeRead2.setHostName("bjDataComputeNode2");
      mixNodeRead2.setIp("10.112.113.123");

      mixNodes.add(mixNodeRead1);
      mixNodes.add(mixNodeRead2);

      //TODO set storage and network
      mixNodeGroup.setInstances(mixNodes);


      nodeGroups.add(mixNodeGroup);

      clusterRead.setNodeGroups(nodeGroups);

      //convert from bean to json
      String jsonString = gson.toJson(clusterRead);

      //      System.out.println("Cluster Read:");
      //      System.out.println(jsonString);

      //convert from json to bean
      clusterRead = gson.fromJson(jsonString, ClusterRead.class);
      //System.out.println(clusterRead.getName());
      assertEquals(clusterRead.getDistro(), "CDH3");
   }

   @Test
   public void testNodeRead() {
      NodeRead nodeRead = new NodeRead();

      nodeRead.setHostName("hadoop-bj-adsf");
      nodeRead.setIp("10.1.1.1");

      //convert from bean to json
      String jsonString = gson.toJson(nodeRead);
      //      System.out.println("Node Read:");
      //      System.out.println(jsonString);

      //convert from json to bean
      nodeRead = gson.fromJson(jsonString, NodeRead.class);
      assertEquals(nodeRead.getIp(), "10.1.1.1");
   }
}
