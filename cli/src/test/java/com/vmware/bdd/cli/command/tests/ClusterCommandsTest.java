/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import com.vmware.bdd.apitypes.TaskRead.Type;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.cli.commands.ClusterCommands;
import com.vmware.bdd.cli.commands.CommandsUtils;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.cli.commands.CookieCache;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopRole;
import com.vmware.bdd.software.mgmt.plugin.model.IpConfigInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NetConfigInfo.NetTrafficType;

@Test
@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })
public class ClusterCommandsTest extends MockRestServer {
    @Autowired
    private ClusterCommands clusterCommands;

   private Map<NetTrafficType, List<IpConfigInfo>> createIpConfigs(String ip) {
      Map<NetTrafficType, List<IpConfigInfo>> ipconfigs = new HashMap<NetTrafficType, List<IpConfigInfo>>();
      List<IpConfigInfo> ips = new ArrayList<IpConfigInfo>();
      ips.add(new IpConfigInfo(NetTrafficType.MGT_NETWORK, "nw1", "pg1", ip));
      ipconfigs.put(NetTrafficType.MGT_NETWORK, ips);
      return ipconfigs;
   }

    @Test
    public void testClusterResize() throws Exception {
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
        nr1.setIpConfigs(createIpConfigs("192.168.1.100"));
        nr1.setName("node1");
        nr1.setStatus("running");
        NodeRead nr2 = new NodeRead();
        nr2.setHostName("test2.vmware.com");
        nr2.setName("node2");
        nr2.setStatus("running");
        NodeRead nr3 = new NodeRead();
        nr3.setHostName("test3.vmware.com");
        nr3.setName("node3");
        nr3.setStatus("running");
        NodeRead nr4 = new NodeRead();
        nr4.setHostName("test4.vmware.com");
        nr4.setName("node4");
        nr4.setStatus("create");
        List<NodeRead> instances1 = new LinkedList<NodeRead>();
        instances1.add(nr1);
        instances1.add(nr2);
        List<NodeRead> instances2 = new LinkedList<NodeRead>();
        instances2.add(nr3);
        instances2.add(nr4);
        List<String> roles1 = new LinkedList<String>();
        roles1.add(Constants.ROLE_HADOOP_JOB_TRACKER);
        List<String> roles2 = new LinkedList<String>();
        roles2.add(HadoopRole.ZOOKEEPER_ROLE.toString());
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
        List<NodeGroupRead> nodeGroupRead1 = new LinkedList<NodeGroupRead>();
        nodeGroupRead1.add(ngr1);
        nodeGroupRead1.add(ngr2);
        cr1.setNodeGroups(nodeGroupRead1);
        this.buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
              mapper.writeValueAsString(cr1));
        this.buildReqRespWithoutRespBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/nodegroup/NodeGroup1/instancenum",
                HttpMethod.PUT, HttpStatus.NO_CONTENT, "5");

        //invalid instance num
        clusterCommands.resizeCluster("cluster1", "NodeGroup1", 0,0,0);

        //normal case
        clusterCommands.resizeCluster("cluster1", "NodeGroup1", 5,0,0);

        //zookeeper resize case
        setup();
        this.buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
              mapper.writeValueAsString(cr1));
        this.buildReqRespWithoutRespBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/nodegroup/NodeGroup1/instancenum",
                HttpMethod.PUT, HttpStatus.NO_CONTENT, "5");
        clusterCommands.resizeCluster("cluster1", "NodeGroup2", 5,0,0);

        CookieCache.clear();
    }

    @Test
    public void testClusterResizeFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("not found");
        ObjectMapper mapper = new ObjectMapper();
        StorageRead sr1 = new StorageRead();
        sr1.setType("Type1");
        sr1.setSizeGB(100);
        StorageRead sr2 = new StorageRead();
        sr2.setType("Type2");
        sr2.setSizeGB(200);
        NodeRead nr1 = new NodeRead();
        nr1.setHostName("test1.vmware.com");
        nr1.setIpConfigs(createIpConfigs("192.168.0.1"));
        nr1.setName("node1");
        nr1.setStatus("running");
        NodeRead nr2 = new NodeRead();
        nr2.setHostName("test2.vmware.com");
        nr2.setIpConfigs(createIpConfigs("192.168.0.2"));
        nr2.setName("node2");
        nr2.setStatus("running");
        NodeRead nr3 = new NodeRead();
        nr3.setHostName("test3.vmware.com");
        nr3.setIpConfigs(createIpConfigs("192.168.0.3"));
        nr3.setName("node3");
        nr3.setStatus("running");
        NodeRead nr4 = new NodeRead();
        nr4.setHostName("test4.vmware.com");
        nr4.setIpConfigs(createIpConfigs("192.168.0.4"));
        nr4.setName("node4");
        nr4.setStatus("create");
        List<NodeRead> instances1 = new LinkedList<NodeRead>();
        instances1.add(nr1);
        instances1.add(nr2);
        List<NodeRead> instances2 = new LinkedList<NodeRead>();
        instances2.add(nr3);
        instances2.add(nr4);
        List<String> roles1 = new LinkedList<String>();
        roles1.add(Constants.ROLE_HADOOP_JOB_TRACKER);
        List<String> roles2 = new LinkedList<String>();
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
        List<NodeGroupRead> nodeGroupRead1 = new LinkedList<NodeGroupRead>();
        nodeGroupRead1.add(ngr1);
        nodeGroupRead1.add(ngr2);
        cr1.setNodeGroups(nodeGroupRead1);
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
              mapper.writeValueAsString(cr1));
        this.buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/nodegroup/ng1/instancenum",
                HttpMethod.PUT, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));

        clusterCommands.resizeCluster("cluster1", "ng1", 5,0,0);
        CookieCache.clear();
    }

    @Test
    public void testClusterStart() {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        this.buildReqRespWithoutRespBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1?state=start",
                HttpMethod.PUT, HttpStatus.NO_CONTENT, "");
        clusterCommands.startCluster("cluster1");
        CookieCache.clear();
    }

    @Test
    public void testClusterStartFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("not found");
        ObjectMapper mapper = new ObjectMapper();

        this.buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1?state=start",
                HttpMethod.PUT, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));

        clusterCommands.startCluster("cluster1");
        CookieCache.clear();
    }

    @Test
    public void testClusterStop() {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        this.buildReqRespWithoutRespBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1?state=stop",
                HttpMethod.PUT, HttpStatus.NO_CONTENT, "");
        clusterCommands.stopCluster("cluster1");
        CookieCache.clear();
    }

    @Test
    public void testClusterStopFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("not found");
        ObjectMapper mapper = new ObjectMapper();

        this.buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1?state=stop",
                HttpMethod.PUT, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));
        clusterCommands.stopCluster("cluster1");
        CookieCache.clear();
    }

    @Test
    public void testCreateCluster() throws Exception {
       CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
       DistroRead[] distros = new DistroRead[1];
       DistroRead distro = new DistroRead();
       distro.setName(Constants.DEFAULT_DISTRO);
       distros[0] = distro;
       NetworkRead[] networks = new NetworkRead[1];
       NetworkRead network = new NetworkRead();
       network.setName("dhcp");
       network.setDhcp(true);
       network.setPortGroup("pg1");
       networks[0] = network;

       ObjectMapper mapper = new ObjectMapper();
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distros", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(distros));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distro/" + distro.getName(), HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(distro));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(networks));

       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/clusters", HttpMethod.POST,
             HttpStatus.NO_CONTENT, "");

       clusterCommands.createCluster("cluster1", null, "HADOOP", null, null, null, null, null, null, null, null, false, false, true, false);

       CookieCache.clear();
    }

    @Test
    public void testCreateClusterFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        DistroRead[] distros = new DistroRead[1];
        DistroRead distro = new DistroRead();
        distro.setName(Constants.DEFAULT_DISTRO);
        distros[0] = distro;
        NetworkRead[] networks = new NetworkRead[1];
        NetworkRead network = new NetworkRead();
        network.setName("dhcp");
        network.setDhcp(true);
        network.setPortGroup("pg1");
        networks[0] = network;

        ObjectMapper mapper = new ObjectMapper();
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distros", HttpMethod.GET, HttpStatus.OK,
              mapper.writeValueAsString(distros));
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distro/" + distro.getName(), HttpMethod.GET, HttpStatus.OK,
              mapper.writeValueAsString(distro));
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(networks));

        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("already exists");

        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/clusters", HttpMethod.POST,
                HttpStatus.BAD_REQUEST, mapper.writeValueAsString(errorMsg));

        clusterCommands.createCluster("cluster1", null, "HADOOP", null, null, null, null, null, null, null, null, false, false, true, false);
        CookieCache.clear();
    }

    @Test
    public void testCreateClusterBySpecFile() throws Exception {
       CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
       DistroRead[] distros = new DistroRead[1];
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
       roles.add("hbase_master");
       roles.add("hbase_regionserver");
       roles.add("hbase_client");
       roles.add("zookeeper");
       roles.add("hadoop_journalnode");
       distro.setRoles(roles);
       distros[0] = distro;
       NetworkRead[] networks = new NetworkRead[1];
       NetworkRead network = new NetworkRead();
       network.setName("dhcp");
       network.setDhcp(true);
       network.setPortGroup("pg1");
       networks[0] = network;

       ObjectMapper mapper = new ObjectMapper();
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distros", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(distros));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distro/" + distro.getName(), HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(distro));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(networks));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/clusters", HttpMethod.POST,
             HttpStatus.NO_CONTENT, "");

       clusterCommands.createCluster("cluster1WithHadoopSpec", null, null, null, "src/test/resources/hadoop_cluster.json", null, null, null, null, null, null, false, false, true, false);

       setup();
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distros", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(distros));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distro/" + distro.getName(), HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(distro));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(networks));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/clusters", HttpMethod.POST,
             HttpStatus.NO_CONTENT, "");
       clusterCommands.createCluster("cluster1WithHBaseSpec", null, null, null, "src/test/resources/hbase_cluster.json", null, null, null, null, null, null, false, false, true, false);

       setup();
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distros", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(distros));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distro/" + distro.getName(), HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(distro));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(networks));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/clusters", HttpMethod.POST,
             HttpStatus.NO_CONTENT, "");
       clusterCommands.createCluster("cluster1WithDCSeperationSpec", null, null, null, "src/test/resources/data_compute_separation_cluster.json", null, null, null, null, null, null, false, false, true, false);

       setup();
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distros", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(distros));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distro/" + distro.getName(), HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(distro));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(networks));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/clusters", HttpMethod.POST,
             HttpStatus.NO_CONTENT, "");
       clusterCommands.createCluster("cluster1WithNameNodeHASpec", null, null, null, "src/test/resources/namenode_ha_cluster.json", null, null, null, null, null, null, false, false, true, false);
       CookieCache.clear();
    }

    @Test
   public void testResumeCreateCluster() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1?state=resume",
            HttpMethod.PUT, HttpStatus.NO_CONTENT, "");
      clusterCommands.createCluster("cluster1", null, null, null, null, null, null, null, null, null, null, true, false, true, false);
      CookieCache.clear();
   }

    @Test
    public void testResumeCreateClusterFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("not found");
        ObjectMapper mapper = new ObjectMapper();
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1?state=resume",
                HttpMethod.PUT, HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));

        clusterCommands.createCluster("cluster1", null, "HADOOP", null, null, null, null, null, null, null, null, true, false, true, false);
        CookieCache.clear();
    }

    @Test
    public void testClusterCreateOutput() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        DistroRead[] distros = new DistroRead[1];
        DistroRead distro = new DistroRead();
        distro.setName(Constants.DEFAULT_DISTRO);
        distros[0] = distro;

        NetworkRead[] networks = new NetworkRead[1];
        NetworkRead network = new NetworkRead();
        network.setName("dhcp");
        network.setDhcp(true);
        network.setPortGroup("pg1");
        networks[0] = network;

        ObjectMapper mapper = new ObjectMapper();
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distros", HttpMethod.GET, HttpStatus.OK,
              mapper.writeValueAsString(distros));
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/distro/" + distro.getName(), HttpMethod.GET, HttpStatus.OK,
              mapper.writeValueAsString(distro));
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/networks", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(networks));

        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/clusters", HttpMethod.POST,
                HttpStatus.ACCEPTED, "", "https://127.0.0.1:8443/serengeti/api/task/12");

        TaskRead task = new TaskRead();
        task.setId(12l);
        task.setType(Type.INNER);
        task.setProgress(0.8);
        task.setProgressMessage("some more details here:");
        task.setStatus(Status.STARTED);
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/task/12", HttpMethod.GET, HttpStatus.OK,
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
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET,
                HttpStatus.OK, mapper.writeValueAsString(cluster));

        task.setProgress(1.0);
        task.setStatus(Status.COMPLETED);
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/task/12", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(task));

        instance1.setStatus("Service Running");
        instance1.setIpConfigs(createIpConfigs("1.2.3.4"));
        instance1.setAction(null);
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET,
                HttpStatus.OK, mapper.writeValueAsString(cluster));

        clusterCommands.createCluster("cluster1", null, "HADOOP", null, null, null, null, null, null, null, null, false, false, true, false);
        CookieCache.clear();
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
        nr1.setIpConfigs(createIpConfigs("10.1.1.99"));
        nr1.setName("node1");
        nr1.setStatus("running");
        nr1.setRack("rack1");
        NodeRead nr2 = new NodeRead();
        nr2.setHostName("test2.vmware.com");
        nr2.setIpConfigs(createIpConfigs("10.1.1.100"));
        nr2.setName("node2");
        nr2.setStatus("running");
        nr2.setRack("rack1");
        NodeRead nr3 = new NodeRead();
        nr3.setHostName("test3.vmware.com");
        nr3.setIpConfigs(createIpConfigs("10.1.1.101"));
        nr3.setName("node3");
        nr3.setStatus("running");
        nr3.setRack("rack1");
        NodeRead nr4 = new NodeRead();
        nr4.setHostName("test4.vmware.com");
        nr4.setIpConfigs(createIpConfigs("10.1.1.102"));
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
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/clusters?details=true", HttpMethod.GET, HttpStatus.OK,
                mapper.writeValueAsString(new ClusterRead[] { cr1, cr2 }));
        clusterCommands.getCluster(null, true);

        //test topology turn-on
        cr1.setTopologyPolicy(TopologyType.HVE);
        cr2.setTopologyPolicy(TopologyType.RACK_AS_RACK);
        setup();
        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/clusters?details=true", HttpMethod.GET, HttpStatus.OK,
              mapper.writeValueAsString(new ClusterRead[] { cr1, cr2 }));
        clusterCommands.getCluster(null, true);

        CookieCache.clear();
    }

    @Test
    public void testExportClusterSpec() throws Exception {
       CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
       ObjectMapper mapper = new ObjectMapper();
       ClusterCreate clusterSpec =
          CommandsUtils.getObjectByJsonString(ClusterCreate.class, CommandsUtils.dataFromFile(this.getClass().getResource("/hadoop_cluster.json").getPath()));
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/hadoop/spec", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(clusterSpec));
       clusterCommands.exportClusterSpec("hadoop", null);

       setup();
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/hadoop/spec", HttpMethod.GET, HttpStatus.OK,
             mapper.writeValueAsString(clusterSpec));
       clusterCommands.exportClusterSpec("hadoop", "exportedSpec.json");
       clusterSpec = CommandsUtils.getObjectByJsonString(ClusterCreate.class, CommandsUtils.dataFromFile("exportedSpec.json"));
       Assert.assertEquals(clusterSpec.getNodeGroups().length, 3);
       File exportedFile = new File("exportedSpec.json");
       if (exportedFile.exists()) {
          Assert.assertEquals(exportedFile.delete(), true);
       }

       CookieCache.clear();
    }

    @Test
    public void testGetClusterFailure() throws Exception {
        CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
        BddErrorMessage errorMsg = new BddErrorMessage();
        errorMsg.setMessage("not found");
        ObjectMapper mapper = new ObjectMapper();

        buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/clusters?details=true", HttpMethod.GET,
                HttpStatus.NOT_FOUND, mapper.writeValueAsString(errorMsg));
        clusterCommands.getCluster(null, true);
        CookieCache.clear();
    }

   @Test
   public void testConfigCluster() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      ObjectMapper mapper = new ObjectMapper();
      StorageRead sr1 = new StorageRead();
      sr1.setType("Type1");
      sr1.setSizeGB(100);
      NodeRead nr1 = new NodeRead();
      nr1.setHostName("test1.domain.com");
      nr1.setIpConfigs(createIpConfigs("192.1.1.99"));
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
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));

      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/config", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.configCluster("cluster1", "src/test/resources/hadoop_cluster.json", false, true);

      //test wrong fair scheduler configuration
      setup();
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/config", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.configCluster("cluster1", "src/test/resources/wrong_fair_scheduler_config.json", false, true);

      //test empty configuration
      setup();
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/config", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.configCluster("cluster1", "src/test/resources/empty_hadoop_config.json", false, true);
      CookieCache.clear();
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

   @Test
   public void testSetReSetParam() throws Exception {
      CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
      ObjectMapper mapper = new ObjectMapper();
      StorageRead sr1 = new StorageRead();
      sr1.setType("Type1");
      sr1.setSizeGB(100);
      NodeRead nr1 = new NodeRead();
      nr1.setHostName("test1.domain.com");
      nr1.setIpConfigs(createIpConfigs("192.1.1.99"));
      nr1.setName("node1");
      nr1.setStatus("running");
      List<NodeRead> instances1 = new LinkedList<NodeRead>();
      instances1.add(nr1);
      List<String> roles1 = new LinkedList<String>();
      roles1.add(Constants.ROLE_HADOOP_TASKTRACKER);
      NodeGroupRead ngr1 = new NodeGroupRead();
      ngr1.setName("NodeGroup1");
      ngr1.setCpuNum(6);
      ngr1.setMemCapacityMB(2048);
      ngr1.setStorage(sr1);
      ngr1.setInstanceNum(2);
      ngr1.setInstances(instances1);
      ngr1.setRoles(roles1);
      ClusterRead cr1 = new ClusterRead();
      cr1.setName("cluster1");
      cr1.setDistroVendor("Apache");
      cr1.setDistro("distro1");
      cr1.setInstanceNum(10);
      cr1.setVhmMinNum(-1);
      cr1.setVhmMaxNum(-1);
      cr1.setStatus(ClusterRead.ClusterStatus.RUNNING);
      List<NodeGroupRead> nodeGroupRead = new LinkedList<NodeGroupRead>();
      nodeGroupRead.add(ngr1);
      List<String> roles2 = new LinkedList<String>();
      roles2.add(Constants.ROLE_HADOOP_CLIENT);
      NodeGroupRead ngr2 = new NodeGroupRead();
      ngr2.setName("NodeGroup2");
      ngr2.setCpuNum(6);
      ngr2.setMemCapacityMB(2048);
      ngr2.setStorage(sr1);
      ngr2.setInstanceNum(1);
      ngr2.setInstances(instances1);
      ngr2.setRoles(roles2);
      nodeGroupRead.add(ngr2);
      cr1.setNodeGroups(nodeGroupRead);
      cr1.setAutomationEnable(false);


      //setParam tests
      //set elasticityMode to MANUAL
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/param", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.setParam("cluster1", "MANUAL", null, null, null, "HIGH");

      //set elasticityMode to AUTO with targetComputeNodeNum=2
      setup();
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/param", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.setParam("cluster1", "AUTO", null, null, 2, "HIGH");

      //set elasticityMode to MANUAL with targetComputeNodeNum=2
      setup();
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/param_wait_for_result", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.setParam("cluster1", "MANUAL", null, null, 2, "HIGH");

      //set minComputeNodeNum=2 and targetComputeNodeNum=2
      setup();
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/param_wait_for_result", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.setParam("cluster1", null, 2, null, 2, "HIGH");

      //set elasticityMode to AUTO with minComputeNodeNum=maxComputeNodeNum=2
      setup();
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/param", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.setParam("cluster1", "AUTO", 2, 2, null, "HIGH");

      //only set ioShares to HIGH
      setup();
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/param", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.setParam("cluster1", null, null, null, null, "HIGH");

      //reset Param tests
      //reset all
      setup();
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/param_wait_for_result", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.resetParam("cluster1", true, false, false, false, false, false);

      //set vhmTargetNum to -1
      //then reset elasticityMode
      setup();
      cr1.setVhmTargetNum(-1);
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/param_wait_for_result", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.resetParam("cluster1", false, true, false, false, false, false);

      //set automationEnable to true
      //then reset minComputeNodeNum, maxComputeNodeNum, targetComputeNodeNum and ioShares
      setup();
      cr1.setAutomationEnable(true);
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/param", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.resetParam("cluster1", false, false, true, true, true, true);

      //set automationEnable to true
      //then only reset ioShares
      setup();
      cr1.setAutomationEnable(true);
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1", HttpMethod.GET, HttpStatus.OK,
            mapper.writeValueAsString(cr1));
      buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/param", HttpMethod.PUT,
            HttpStatus.NO_CONTENT, "");
      clusterCommands.resetParam("cluster1", false, false, false, false, false, true);
      CookieCache.clear();
   }

   @Test
   public void testClusterUpgrade() throws Exception {
       CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
       StorageRead sr1 = new StorageRead();
       sr1.setType("Type1");
       sr1.setSizeGB(100);
       NodeRead nr1 = new NodeRead();
       nr1.setHostName("test1.domain.com");
       nr1.setIpConfigs(createIpConfigs("192.1.1.99"));
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
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1/upgrade",
               HttpMethod.PUT, HttpStatus.NO_CONTENT, "");
       clusterCommands.upgradeCluster("cluster1");
       CookieCache.clear();
   }

   @Test
   public void testDeleteCluster() throws Exception {
       CookieCache.put("Cookie","JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F");
       StorageRead sr1 = new StorageRead();
       sr1.setType("Type1");
       sr1.setSizeGB(100);
       NodeRead nr1 = new NodeRead();
       nr1.setHostName("test1.domain.com");
       nr1.setIpConfigs(createIpConfigs("192.1.1.99"));
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
       buildReqRespWithoutReqBody("https://127.0.0.1:8443/serengeti/api/cluster/cluster1",
               HttpMethod.DELETE, HttpStatus.NO_CONTENT, "");
       clusterCommands.deleteCluster("cluster1");
       CookieCache.clear();
   }

}
