/******************************************************************************
 *   Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.vmware.bdd.cli.command.tests;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.StorageRead;
import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.apitypes.TaskRead.Status;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.cli.commands.ClusterCommands;
import com.vmware.bdd.cli.commands.CommandsUtils;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.cli.commands.CookieCache;

@Test
@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class ClusterCommandsTest extends MockRestServer {
    @Autowired
    private ClusterCommands clusterCommands;

    @Test
    public void testClusterResize() {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        this.buildReqRespWithoutRespBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1/nodegroup/ng1/instancenum",
                HttpMethod.PUT, HttpStatus.NO_CONTENT, "5");
        //invalid instance num
        clusterCommands.resizeCluster("cluster1", "ng1", 0);

        //normal case
        clusterCommands.resizeCluster("cluster1", "ng1", 5);
        CookieCache.put("Cookie","");
    }

    @Test
    public void testClusterResizeFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("not found");
        ObjectMapper mapper = new ObjectMapper();
        this.buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1/nodegroup/ng1/instancenum",
                HttpMethod.PUT, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));

        clusterCommands.resizeCluster("cluster1", "ng1", 5);
        CookieCache.put("Cookie","");
    }

    @Test
    public void testClusterStart() {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        this.buildReqRespWithoutRespBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1?state=start",
                HttpMethod.PUT, HttpStatus.NO_CONTENT, "");
        clusterCommands.startCluster("cluster1", null, null);
        CookieCache.put("Cookie","");
    }

    @Test
    public void testClusterStartFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("not found");
        ObjectMapper mapper = new ObjectMapper();

        this.buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1?state=start",
                HttpMethod.PUT, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));

        clusterCommands.startCluster("cluster1", null, null);
        CookieCache.put("Cookie","");
    }

    @Test
    public void testClusterStop() {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        this.buildReqRespWithoutRespBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1?state=stop",
                HttpMethod.PUT, HttpStatus.NO_CONTENT, "");
        clusterCommands.stopCluster("cluster1", null, null);
        CookieCache.put("Cookie","");
    }

    @Test
    public void testClusterStopFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("not found");
        ObjectMapper mapper = new ObjectMapper();

        this.buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1?state=stop",
                HttpMethod.PUT, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));
        clusterCommands.stopCluster("cluster1", null, null);
        CookieCache.put("Cookie","");
    }

    @Test
    public void testCreateCluster() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        NetworkRead[] networks = new NetworkRead[1];
        NetworkRead network = new NetworkRead();
        network.setName("dhcp");
        network.setDhcp(true);
        network.setPortGroup("pg1");
        networks[0] = network;

        ObjectMapper mapper = new ObjectMapper();
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(networks));

        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/clusters", HttpMethod.POST,
                HttpStatus.NO_CONTENT, "");

        clusterCommands.createCluster("cluster1", null, null, null, null, null, null, false, false, false);
        CookieCache.put("Cookie","");
    }

    @Test
    public void testCreateClusterFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        NetworkRead[] networks = new NetworkRead[1];
        NetworkRead network = new NetworkRead();
        network.setName("dhcp");
        network.setDhcp(true);
        network.setPortGroup("pg1");
        networks[0] = network;

        ObjectMapper mapper = new ObjectMapper();
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(networks));

        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("already exists");

        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/clusters", HttpMethod.POST,
                HttpStatus.BAD_REQUEST, mapper.writeValueAsString(errorMsg));

        clusterCommands.createCluster("cluster1", null, null, null, null, null, null, false, false, false);
        CookieCache.put("Cookie","");
    }

    @Test
    public void testCreateClusterBySpecFile() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        NetworkRead[] networks = new NetworkRead[1];
        NetworkRead network = new NetworkRead();
        network.setName("dhcp");
        network.setDhcp(true);
        network.setPortGroup("pg1");
        networks[0] = network;

        ObjectMapper mapper = new ObjectMapper();
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(networks));

        DistroRead distro = new DistroRead();
        distro.setName(Constants.DEFAULT_DISTRO);
        List<String> roles = new ArrayList<String>();
        roles.add("hadoop");
        roles.add("hadoop_namenode");
        roles.add("hadoop_jobtracker");
        roles.add("hadoop_worker");
        roles.add("hadoop_datanode");
        roles.add("hadoop_tasktracker");
        roles.add("hadoop_client");
        roles.add("hive");
        roles.add("hive_server");
        roles.add("pig");
        distro.setRoles(roles);

        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/distro/" + Constants.DEFAULT_DISTRO,
                HttpMethod.GET, HttpStatus.OK, mapper.writeValueAsString(distro));

        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/clusters", HttpMethod.POST,
                HttpStatus.NO_CONTENT, "");

        clusterCommands.createCluster("cluster1", null, "hadoop_cluster.json", null, null, null, null, false, false, false);
        CookieCache.put("Cookie","");
    }

    @Test
   public void testResumeCreateCluster() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1?state=resume",
            HttpMethod.PUT, HttpStatus.NO_CONTENT, "");
      clusterCommands.createCluster("cluster1", null, null, null, null, null, null, true, false, false);
      CookieCache.put("Cookie","");
   }

    @Test
    public void testResumeCreateClusterFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("not found");
        ObjectMapper mapper = new ObjectMapper();
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1?state=resume",
                HttpMethod.PUT, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));

        clusterCommands.createCluster("cluster1", "HADOOP", null, null, null, null, null, true, false, false);
        CookieCache.put("Cookie","");
    }

    @Test
    public void testClusterCreateOutput() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        NetworkRead[] networks = new NetworkRead[1];
        NetworkRead network = new NetworkRead();
        network.setName("dhcp");
        network.setDhcp(true);
        network.setPortGroup("pg1");
        networks[0] = network;

        ObjectMapper mapper = new ObjectMapper();
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(networks));

        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/clusters", HttpMethod.POST,
                HttpStatus.ACCEPTED, "", "http://127.0.0.1:8080/serengeti/api/task/12");

        TaskRead task = new TaskRead();
        task.setId(12l);
        task.setProgress(0.8);
        task.setStatus(Status.RUNNING);
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/task/12", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(task));

        ClusterRead cluster = new ClusterRead();
        List<NodeGroupRead> nodeGroups = new ArrayList<NodeGroupRead>();
        NodeGroupRead workerGroup = new NodeGroupRead();
        workerGroup.setName("worker");
        workerGroup.setInstanceNum(1);
        List<NodeRead> instances = new ArrayList<NodeRead>();
        NodeRead instance1 = new NodeRead();
        instance1.setName("worker1");
        instance1.setStatus("PoweredOn");
        instance1.setAction("Getting IP...");
        instances.add(instance1);
        workerGroup.setInstances(instances);
        nodeGroups.add(workerGroup);
        cluster.setNodeGroups(nodeGroups);
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1", HttpMethod.GET,
                HttpStatus.OK, mapper.writeValueAsString(cluster));

        task.setProgress(1.0);
        task.setStatus(Status.SUCCESS);
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/task/12", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(task));

        instance1.setStatus("Service Running");
        instance1.setIp("1.2.3.4");
        instance1.setAction(null);
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1", HttpMethod.GET,
                HttpStatus.OK, mapper.writeValueAsString(cluster));

        clusterCommands.createCluster("cluster1", null, null, null, null, null, null, false, false, false);
        CookieCache.put("Cookie","");
    }

    @Test
    public void testGetCluster() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        ObjectMapper mapper = new ObjectMapper();
        StorageRead sr1 = new StorageRead();
        sr1.setType("Type1");
        sr1.setSizeGB(100);
        StorageRead sr2 = new StorageRead();
        sr2.setType("Type2");
        sr2.setSizeGB(200);
        NodeRead nr1 = new NodeRead();
        nr1.setHostName("test1.vmware.com");
        nr1.setIp("10.1.1.99");
        nr1.setName("node1");
        nr1.setStatus("running");
        nr1.setRack("rack1");
        NodeRead nr2 = new NodeRead();
        nr2.setHostName("test2.vmware.com");
        nr2.setIp("10.1.1.100");
        nr2.setName("node2");
        nr2.setStatus("running");
        nr2.setRack("rack1");
        NodeRead nr3 = new NodeRead();
        nr3.setHostName("test3.vmware.com");
        nr3.setIp("10.1.1.101");
        nr3.setName("node3");
        nr3.setStatus("running");
        nr3.setRack("rack1");
        NodeRead nr4 = new NodeRead();
        nr4.setHostName("test4.vmware.com");
        nr4.setIp("10.1.1.102");
        nr4.setName("node4");
        nr4.setStatus("create");
        nr4.setRack("rack1");
        List<NodeRead> instances1 = new LinkedList<NodeRead>();
        instances1.add(nr1);
        instances1.add(nr2);
        List<NodeRead> instances2 = new LinkedList<NodeRead>();
        instances2.add(nr3);
        instances2.add(nr4);
        List<String> roles1 = new LinkedList<String>();
        roles1.add(Constants.ROLE_HADOOP_NAME_NODE);
        roles1.add(Constants.ROLE_HADOOP_JOB_TRACKER);
        List<String> roles2 = new LinkedList<String>();
        roles2.add(Constants.ROLE_HADOOP_DATANODE);
        roles2.add(Constants.ROLE_HADOOP_TASKTRACKER);
        NodeGroupRead ngr1 = new NodeGroupRead();
        ngr1.setName("NodeGroup1");
        ngr1.setCpuNum(6);
        ngr1.setMemCapacityMB(2048);
        ngr1.setStorage(sr1);
        ngr1.setInstanceNum(1);
        ngr1.setInstances(instances1);
        ngr1.setRoles(roles1);
        NodeGroupRead ngr2 = new NodeGroupRead();
        ngr2.setName("NodeGroup2");
        ngr2.setCpuNum(12);
        ngr2.setMemCapacityMB(2048);
        ngr2.setStorage(sr2);
        ngr2.setInstanceNum(20);
        ngr2.setInstances(instances2);
        ngr2.setRoles(roles2);
        ClusterRead cr1 = new ClusterRead();
        cr1.setName("cluster1");
        cr1.setDistro("distro1");
        cr1.setInstanceNum(10);
        cr1.setStatus(ClusterRead.ClusterStatus.RUNNING);
        ClusterRead cr2 = new ClusterRead();
        cr2.setName("cluster2");
        cr2.setDistro("distro2");
        cr2.setInstanceNum(20);
        cr2.setStatus(ClusterRead.ClusterStatus.STOPPED);
        List<NodeGroupRead> nodeGroupRead1 = new LinkedList<NodeGroupRead>();
        nodeGroupRead1.add(ngr1);
        nodeGroupRead1.add(ngr2);
        cr1.setNodeGroups(nodeGroupRead1);
        cr2.setNodeGroups(nodeGroupRead1);
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/clusters", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(new ClusterRead[] { cr1, cr2 }));
        clusterCommands.getCluster(null, true);

        //test topology turn-on
        cr1.setTopologyPolicy(TopologyType.HVE);
        cr2.setTopologyPolicy(TopologyType.RACK_AS_RACK);
        setup();
        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/clusters", HttpMethod.GET, HttpStatus.OK,
              mapper.writeValueAsString(new ClusterRead[] { cr1, cr2 }));
        clusterCommands.getCluster(null, true);

        CookieCache.put("Cookie","");
    }

    @Test
    public void testExportClusterSpec() throws Exception {
       CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
       ObjectMapper mapper = new ObjectMapper();
       ClusterCreate clusterSpec =
          CommandsUtils.getObjectByJsonString(ClusterCreate.class, CommandsUtils.dataFromFile(this.getClass().getResource("/hadoop_cluster.json").getPath()));
       buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/cluster/hadoop/spec", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(clusterSpec));
     clusterCommands.exportClusterSpec("hadoop", null);
     CookieCache.put("Cookie","");
    }

    @Test
    public void testGetClusterFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("not found");
        ObjectMapper mapper = new ObjectMapper();

        buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/clusters", HttpMethod.GET,
                HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));
        clusterCommands.getCluster(null, true);
        CookieCache.put("Cookie","");
    }

   @Test(enabled=false)
   public void testConfigCluster() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      ObjectMapper mapper = new ObjectMapper();
      StorageRead sr1 = new StorageRead();
      sr1.setType("Type1");
      sr1.setSizeGB(100);
      NodeRead nr1 = new NodeRead();
      nr1.setHostName("test1.domain.com");
      nr1.setIp("192.1.1.99");
      nr1.setName("node1");
      nr1.setStatus("running");
      List<NodeRead> instances1 = new LinkedList<NodeRead>();
      instances1.add(nr1);
      List<String> roles1 = new LinkedList<String>();
      roles1.add(Constants.ROLE_HADOOP_NAME_NODE);
      roles1.add(Constants.ROLE_HADOOP_JOB_TRACKER);
      NodeGroupRead ngr1 = new NodeGroupRead();
      ngr1.setName("NodeGroup1");
      ngr1.setCpuNum(6);
      ngr1.setMemCapacityMB(2048);
      ngr1.setStorage(sr1);
      ngr1.setInstanceNum(1);
      ngr1.setInstances(instances1);
      ngr1.setRoles(roles1);
      ClusterRead cr1 = new ClusterRead();
      cr1.setName("cluster1");
      cr1.setDistro("distro1");
      cr1.setInstanceNum(10);
      cr1.setStatus(ClusterRead.ClusterStatus.RUNNING);
      List<NodeGroupRead> nodeGroupRead1 = new LinkedList<NodeGroupRead>();
      nodeGroupRead1.add(ngr1);
      cr1.setNodeGroups(nodeGroupRead1);
      buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));

      buildReqRespWithoutReqBody("http://127.0.0.1:8080/serengeti/api/cluster/cluster1/config", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.configCluster("cluster1", "hadoop_cluster.json", false, false);
      CookieCache.put("Cookie","");
   }

   @Test
   public void testParseClusterSpec() {
     try {
        String[] specFiles = { "hadoop_cluster.json", "hbase_cluster.json" };
        for (String specFile : specFiles) {
           ClusterCreate clusterSpec = CommandsUtils.getObjectByJsonString(
                 ClusterCreate.class, CommandsUtils.dataFromFile(this.getClass().getResource("/" + specFile).getPath()));
           List<String> errors = new ArrayList<String>();
           List<String> warnings = new ArrayList<String>();
           boolean valid = clusterSpec.validateNodeGroupPlacementPolicies(errors, warnings);
           Assert.assertTrue(valid, errors.toString());
        }
     } catch (Exception e) {
        Assert.fail("failed to parse cluster spec", e);
     }
   }

}
