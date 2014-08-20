package com.vmware.bdd.plugin.ambari.service.am;

import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.ClustersResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.ConfigGroupsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.HostComponentsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.HostsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.RequestsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.ServicesResource;

public class FakeClustersResource implements ClustersResource {

   @Override
   public Response readClusters() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Response readCluster(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Response createCluster(String clusterName, String clusterBlueprint) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Response deleteCluster(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RequestsResource getRequestsResource(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ServicesResource getServicesResource(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public HostsResource getHostsResource(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public HostComponentsResource getHostComponentsResource(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ConfigGroupsResource getConfigGroupsResource(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

}
