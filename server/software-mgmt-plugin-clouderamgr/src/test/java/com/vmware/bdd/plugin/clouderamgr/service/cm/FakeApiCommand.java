package com.vmware.bdd.plugin.clouderamgr.service.cm;

import com.cloudera.api.model.ApiCommand;

/**
 * Created by qjin on 8/19/14.
 */
public class FakeApiCommand extends ApiCommand {
   @Override
   public Long getId() {
      return new Long(0);
   }
}
