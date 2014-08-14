/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.clouderamgr.service.cm;

import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiCollectDiagnosticDataArguments;
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiCommandList;
import com.cloudera.api.model.ApiConfigList;
import com.cloudera.api.model.ApiDeployment;
import com.cloudera.api.model.ApiHealthSummary;
import com.cloudera.api.model.ApiHost;
import com.cloudera.api.model.ApiHostInstallArguments;
import com.cloudera.api.model.ApiHostList;
import com.cloudera.api.model.ApiHostNameList;
import com.cloudera.api.model.ApiLicense;
import com.cloudera.api.model.ApiLicensedFeatureUsage;
import com.cloudera.api.model.ApiVersionInfo;
import com.cloudera.api.v2.HostsResourceV2;
import com.cloudera.api.v3.AllHostsResource;
import com.cloudera.api.v3.CmPeersResource;
import com.cloudera.api.v6.ClouderaManagerResourceV6;
import com.cloudera.api.v6.MgmtServiceResourceV6;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import javax.ws.rs.DefaultValue;
import java.io.InputStream;
import java.util.UUID;

/**
 * Author: Xiaoding Bian
 * Date: 7/7/14
 * Time: 10:37 AM
 */
public class FakeClouderaManagerResource implements ClouderaManagerResourceV6 {

   private HostsResourceV2 hostsResourceV2;
   private MgmtServiceResourceV6 mgmtServiceResource;

   public FakeClouderaManagerResource(HostsResourceV2 hostsResourceV2) {
      this.hostsResourceV2 = hostsResourceV2;
      this.mgmtServiceResource = new FakeMgmtServiceResource();
   }

   @Override
   public void beginTrial() {

   }

   @Override
   public void endTrial() {

   }

   @Override
   public ApiLicensedFeatureUsage getLicensedFeatureUsage() {
      return null;
   }

   @Override
   public ApiCommand hostInstallCommand(ApiHostInstallArguments apiHostInstallArguments) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiHostList hosts = new ApiHostList();
      for (String ip : apiHostInstallArguments.getHostNames()) {
         ApiHost host = new ApiHost();
         host.setHostId(UUID.randomUUID().toString());
         host.setIpAddress(ip);
         host.setHostname(ip);
         hosts.add(host);
      }
      hostsResourceV2.createHosts(hosts);

      ApiCommand command = new ApiCommand();
      command.setName("installHosts");
      command.setId(1L);
      return command;
   }

   @Override
   public AllHostsResource getAllHostsResource() {
      return null;
   }

   @Override
   public CmPeersResource getCmPeersResource() {
      return null;
   }

   @Override
   public ApiDeployment getDeployment(@DefaultValue("export") DataView dataView) {
      return null;
   }

   @Override
   public ApiDeployment updateDeployment(ApiDeployment apiDeployment, @DefaultValue("false") Boolean aBoolean) {
      return null;
   }

   @Override
   public ApiCommand hostsDecommissionCommand(ApiHostNameList strings) {
      return null;
   }

   @Override
   public ApiCommand hostsRecommissionCommand(ApiHostNameList strings) {
      return null;
   }

   @Override
   public ApiCommand hostsStartRolesCommand(ApiHostNameList strings) {
      return null;
   }

   @Override
   public ApiConfigList getConfig(@DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiConfigList updateConfig(ApiConfigList apiConfigs) {
      return null;
   }

   @Override
   public ApiLicense readLicense() {
      return null;
   }

   @Override
   public ApiLicense updateLicense(@Multipart("license") byte[] bytes) {
      return null;
   }

   @Override
   public ApiCommandList listActiveCommands(@DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiCommand generateCredentialsCommand() {
      return null;
   }

   @Override
   public ApiCommand inspectHostsCommand() {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiCommand command = new ApiCommand();
      command.setName("inspectHosts");
      command.setId(1L);
      return command;
   }

   @Override
   public MgmtServiceResourceV6 getMgmtServiceResource() {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return mgmtServiceResource;
   }

   @Override
   public ApiCommand collectDiagnosticDataCommand(ApiCollectDiagnosticDataArguments apiCollectDiagnosticDataArguments) {
      return null;
   }

   @Override
   public ApiVersionInfo getVersion() {
      ApiVersionInfo versionInfo = new ApiVersionInfo();
      versionInfo.setVersion("5.0.1");
      return versionInfo;
   }

   @Override
   public InputStream getLog() {
      return null;
   }
}
