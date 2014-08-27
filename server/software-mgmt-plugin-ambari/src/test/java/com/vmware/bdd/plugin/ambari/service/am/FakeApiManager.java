package com.vmware.bdd.plugin.ambari.service.am;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.exception.AmbariApiException;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequest;

import java.net.URL;

/**
 * Created by qjin on 8/27/14.
 */
public class FakeApiManager extends ApiManager {

   public FakeApiManager(AmbariManagerClientbuilder clientbuilder) {
      super(clientbuilder);
   }

   @Override
   public ApiRequest startAllServicesInCluster(String clusterName) throws AmbariApiException {
      return null;
   }

   @Override
   public ApiRequest stopAllServicesInCluster(String clusterName) throws AmbariApiException {
      return null;
   }

   @Override
   public ApiClusterList getClusterList() {
      return new ApiClusterList();
   }

   @Override
   public ApiCluster getCluster(String clusterName) {
      return null;
   }
}
