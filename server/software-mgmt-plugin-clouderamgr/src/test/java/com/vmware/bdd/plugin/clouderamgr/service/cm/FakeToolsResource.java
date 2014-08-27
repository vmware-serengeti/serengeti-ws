package com.vmware.bdd.plugin.clouderamgr.service.cm;

import com.cloudera.api.model.ApiEcho;
import com.cloudera.api.v1.ToolsResource;

import javax.ws.rs.DefaultValue;

/**
 * Author: Xiaoding Bian
 * Date: 8/27/14
 * Time: 3:59 PM
 */
public class FakeToolsResource implements ToolsResource {
   @Override
   public ApiEcho echo(@DefaultValue("Hello, World!") String s) {
      ApiEcho echo = new ApiEcho();
      echo.setMessage(s);
      return echo;
   }

   @Override
   public ApiEcho echoError(@DefaultValue("Default error message") String s) {
      return null;
   }
}
