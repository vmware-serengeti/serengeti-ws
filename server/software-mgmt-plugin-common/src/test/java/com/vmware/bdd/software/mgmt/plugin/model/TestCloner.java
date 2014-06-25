package com.vmware.bdd.software.mgmt.plugin.model;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import org.testng.annotations.Test;

import javax.xml.soap.Node;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 6/26/14
 * Time: 11:07 AM
 */
public class TestCloner {
   @Test
   public void testClone() {
      ClusterReport report = new ClusterReport();
      report.setName("cluster01");
      Map<String, NodeReport> nodes = new HashMap<String, NodeReport>();
      NodeReport node1 = new NodeReport();
      node1.setName("node1");
      nodes.put(node1.getName(), node1);
      report.setNodeReports(nodes);

      System.out.println((new Gson()).toJson(report));

      ClusterReport report2 = report.clone();
      report2.setName("cluster02");
      report2.getNodeReports().get("node1").setName("node1111");


      System.out.println((new Gson()).toJson(report2));
      System.out.println((new Gson()).toJson(report));

      System.out.println((new SimpleDateFormat("yyyyMMddHHmm")).format(new Date()));
   }

}
