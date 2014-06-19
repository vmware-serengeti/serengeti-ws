package com.vmware.bdd.software.mgmt.plugin.impl;

import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.ServiceType;
import junit.framework.TestCase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class DefaultSoftwareManagerImplTest extends TestCase {
    private static DefaultSoftwareManagerImpl defaultSoftwareManager = new DefaultSoftwareManagerImpl();

    @BeforeClass
    public void setup() throws Exception {

    }

    public void testValidateBlueprint() throws Exception {

    }

    public void testValidateRoles() throws Exception {
    }

    public void testValidateCliConfigurations() throws Exception {

    }

    @Test
    public void testValidateRoleDependency() {
        ClusterBlueprint blueprint = new ClusterBlueprint();
        List<String> failedMsgList = new ArrayList<String>();
        assertEquals(false, defaultSoftwareManager.validateRoleDependency(failedMsgList, blueprint));

        NodeGroupInfo compute = new NodeGroupInfo();
        NodeGroupInfo data = new NodeGroupInfo();
        List<NodeGroupInfo> nodeGroupInfos = new ArrayList<NodeGroupInfo>();
        nodeGroupInfos.add(compute);
        nodeGroupInfos.add(data);
        blueprint.setNodeGroups(nodeGroupInfos);
        assertEquals(false, defaultSoftwareManager.validateRoleDependency(failedMsgList, blueprint));
        assertEquals(2, failedMsgList.size());
        failedMsgList.clear();
        blueprint.setExternalHDFS("hdfs://192.168.0.2:9000");
        compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString()));
        data.setRoles(Arrays.asList(HadoopRole.HADOOP_DATANODE.toString()));
        assertEquals(false, defaultSoftwareManager.validateRoleDependency(failedMsgList, blueprint));
        assertEquals(2, failedMsgList.size());
        assertEquals("Duplicate NameNode or DataNode role.", failedMsgList.get(0));
        assertEquals("Missing JobTracker or ResourceManager role.",
                failedMsgList.get(1));
        failedMsgList.clear();
        blueprint.setExternalHDFS("");
        nodeGroupInfos = new ArrayList<NodeGroupInfo>();
        nodeGroupInfos.add(compute);
        blueprint.setNodeGroups(nodeGroupInfos);
        assertEquals(false, defaultSoftwareManager.validateRoleDependency(failedMsgList, blueprint));
        assertEquals(1, failedMsgList.size());
        assertEquals("Cannot find one or more roles in " + ServiceType.MAPRED + " "
                + ServiceType.MAPRED.getRoles()
                + " in the cluster specification file.", failedMsgList.get(0));
        failedMsgList.clear();
        NodeGroupInfo master = new NodeGroupInfo();
        master.setRoles(Arrays.asList(HadoopRole.HADOOP_JOBTRACKER_ROLE
                .toString()));
        nodeGroupInfos = new ArrayList<NodeGroupInfo>();
        nodeGroupInfos.add(master);
        nodeGroupInfos.add(compute);
        blueprint.setNodeGroups(nodeGroupInfos);
        assertEquals(false, defaultSoftwareManager.validateRoleDependency(failedMsgList, blueprint));
        assertEquals(1, failedMsgList.size());
        assertEquals("Some dependent services " + EnumSet.of(ServiceType.HDFS)
                        + " " + ServiceType.MAPRED
                        + " relies on cannot be found in the spec file.",
                failedMsgList.get(0));
    }

    public void testValidateHDFSUrl() throws Exception {

    }

    public void testValidateScaling() throws Exception {

    }
}