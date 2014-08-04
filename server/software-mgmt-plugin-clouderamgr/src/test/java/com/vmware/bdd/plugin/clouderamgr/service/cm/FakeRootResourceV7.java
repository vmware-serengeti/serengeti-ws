package com.vmware.bdd.plugin.clouderamgr.service.cm;

import com.cloudera.api.v1.CommandsResource;
import com.cloudera.api.v1.EventsResource;
import com.cloudera.api.v1.ToolsResource;
import com.cloudera.api.v1.UsersResource;
import com.cloudera.api.v2.HostsResourceV2;
import com.cloudera.api.v4.AuditsResource;
import com.cloudera.api.v6.BatchResource;
import com.cloudera.api.v6.TimeSeriesResourceV6;
import com.cloudera.api.v7.ClouderaManagerResourceV7;
import com.cloudera.api.v7.ClustersResourceV7;
import com.cloudera.api.v7.RootResourceV7;

/**
 * Author: Xiaoding Bian
 * Date: 8/4/14
 * Time: 11:19 AM
 */
public class FakeRootResourceV7 implements RootResourceV7 {
   @Override
   public ClustersResourceV7 getClustersResource() {
      return null;
   }

   @Override
   public CommandsResource getCommandsResource() {
      return null;
   }

   @Override
   public AuditsResource getAuditsResource() {
      return null;
   }

   @Override
   public BatchResource getBatchResource() {
      return null;
   }

   @Override
   public ClouderaManagerResourceV7 getClouderaManagerResource() {
      return null;
   }

   @Override
   public HostsResourceV2 getHostsResource() {
      return null;
   }

   @Override
   public ToolsResource getToolsResource() {
      return null;
   }

   @Override
   public UsersResource getUsersResource() {
      return null;
   }

   @Override
   public EventsResource getEventsResource() {
      return null;
   }

   @Override
   public TimeSeriesResourceV6 getTimeSeriesResource() {
      return null;
   }
}
