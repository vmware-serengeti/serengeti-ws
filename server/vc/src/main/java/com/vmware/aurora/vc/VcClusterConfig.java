/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

package com.vmware.aurora.vc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.CommonUtil;
import com.vmware.vim.binding.vim.cluster.ConfigInfoEx;
import com.vmware.vim.binding.vim.cluster.DasAdmissionControlPolicy;
import com.vmware.vim.binding.vim.cluster.DasConfigInfo;
import com.vmware.vim.binding.vim.cluster.DasVmConfigInfo;
import com.vmware.vim.binding.vim.cluster.DasVmSettings;
import com.vmware.vim.binding.vim.cluster.DrsConfigInfo;
import com.vmware.vim.binding.vim.cluster.FailoverHostAdmissionControlPolicy;
import com.vmware.vim.binding.vim.cluster.FailoverLevelAdmissionControlPolicy;
import com.vmware.vim.binding.vim.cluster.FailoverResourcesAdmissionControlPolicy;
import com.vmware.vim.binding.vim.cluster.VmToolsMonitoringSettings;

@SuppressWarnings("serial")
public class VcClusterConfig implements Serializable {
   public static enum AdmCtlPolType {
      FailoverHostPolicy(FailoverHostAdmissionControlPolicy.class), FailoverResourcePolicy(
            FailoverResourcesAdmissionControlPolicy.class), FailoverLevelPolicy(
            FailoverLevelAdmissionControlPolicy.class), UnknownPolicy(null);

      private Class<? extends DasAdmissionControlPolicy> polClass;

      AdmCtlPolType(Class<? extends DasAdmissionControlPolicy> polClass) {
         this.polClass = polClass;
      }

      private static AdmCtlPolType getInstance(DasAdmissionControlPolicy polObj) {
         for (AdmCtlPolType polType : AdmCtlPolType.values()) {
            if (polType.polClass != null && polType.polClass.isInstance(polObj)) {
               return polType;
            }
         }
         return UnknownPolicy;
      }
   }

   public static boolean skipHADRSCheck() {
      return Configuration.getBoolean("vc.skipClusterHACheck", false);
   }

   public static class VmHAConfig implements Serializable {
      private String id;
      private DasConfigInfo.VmMonitoringState vmMonState; // strict
      private int vmMonFailInterval; // lenient
      private int vmMonMaxFailures; // lenient
      private int vmMonMaxFailuresWindow; // lenient
      private int vmMonMinUptime; // lenient

      VmHAConfig(String id, DasConfigInfo.VmMonitoringState vmMonState, int vmMonFailInterval,
            int vmMonMaxFailures, int vmMonMaxFailuresWindow, int vmMonMinUptime) {
         this.id = id;
         this.vmMonState = vmMonState;
         this.vmMonFailInterval = vmMonFailInterval;
         this.vmMonMaxFailures = vmMonMaxFailures;
         this.vmMonMaxFailuresWindow = vmMonMaxFailuresWindow;
         this.vmMonMinUptime = vmMonMinUptime;
      }

      VmHAConfig(DasVmConfigInfo vmConfig, VmHAConfig defaultVmHAConfig) {
         DasVmSettings vmHASettings = vmConfig.getDasSettings();
         VmToolsMonitoringSettings toolsSettings = vmHASettings.getVmToolsMonitoringSettings();

         vmMonState = (toolsSettings.getVmMonitoring() != null
               ? DasConfigInfo.VmMonitoringState.valueOf(toolsSettings.getVmMonitoring())
               : defaultVmHAConfig.vmMonState);
         vmMonFailInterval = (toolsSettings.getFailureInterval() != null
               ? toolsSettings.getFailureInterval()
               : defaultVmHAConfig.vmMonFailInterval);
         vmMonMaxFailures = (toolsSettings.getMaxFailures() != null
               ? toolsSettings.getMaxFailures()
               : defaultVmHAConfig.vmMonMaxFailures);
         vmMonMaxFailuresWindow = (toolsSettings.getMaxFailureWindow() != null
               ? toolsSettings.getMaxFailureWindow()
               : defaultVmHAConfig.vmMonMaxFailuresWindow);
         vmMonMinUptime = (toolsSettings.getMinUpTime() != null
               ? toolsSettings.getMinUpTime()
               : defaultVmHAConfig.vmMonMinUptime);
      }

      public List<String> getHADRSIncompatReasons(VmHAConfig reference, boolean strict) {
         List<String> reasons = new ArrayList<String>();
         CommonUtil.checkCond((vmMonState == reference.vmMonState), reasons,
               id + " monitoring state is not " + reference.vmMonState.toString() + ".");
         if (!strict) {
            CommonUtil.checkCond((vmMonFailInterval >= reference.vmMonFailInterval), reasons,
                  id + " failure interval is less than " + reference.vmMonFailInterval + ".");
            CommonUtil.checkCond((vmMonMaxFailures >= reference.vmMonMaxFailures), reasons,
                  id + " maximum per-VM resets are less than " + reference.vmMonMaxFailures + ".");
            CommonUtil.checkCond((vmMonMaxFailuresWindow >= reference.vmMonMaxFailuresWindow), reasons,
                  id + " maximum resets time window is less than " + reference.vmMonMaxFailuresWindow + " seconds.");
            CommonUtil.checkCond((vmMonMinUptime >= reference.vmMonMinUptime), reasons,
                  id + " minumum uptime is less than " + reference.vmMonMinUptime + " seconds.");
         }
         return reasons;
      }

      public List<String> getHADRSIncompatReasons(boolean strict) {
         return getHADRSIncompatReasons(GoldenVmHAConfig, strict);
      }

      public boolean isHADRSCompatible(VmHAConfig reference, boolean strict) {
         return getHADRSIncompatReasons(reference, strict).isEmpty();
      }

      public boolean isHADRSCompatible(boolean strict) {
         return isHADRSCompatible(GoldenVmHAConfig, strict);
      }

      public String toString() {
         StringBuffer buf = new StringBuffer(id);
         return buf.append("vmMonState").append("=").append(vmMonState)
               .append(";").append("vmMonFailInterval")
               .append("=").append(vmMonFailInterval).append(";")
               .append("vmMonMaxFailures").append("=").append(vmMonMaxFailures)
               .append(";").append("vmMonMaxFailuresWindow").append("=")
               .append(vmMonMaxFailuresWindow).append(";")
               .append("vmMonMinUptime").append("=").append(vmMonMinUptime)
               .append(";").toString();
      }
   }

   private boolean haEnabled; // strict
   private boolean admCtlEnabled; // strict
   private VmHAConfig defaultVmHAConfig; // strict, lenient (see VmConfig)
   private DasConfigInfo.ServiceState hostMonEnabled; // lenient
   private AdmCtlPolType admCtlPol; // lenient
   private int resAdmPolCpuPerc; // lenient
   private int resAdmPolMemPerc; // lenient
   private boolean drsEnabled; // lenient

   public static final VmHAConfig GoldenVmHAConfig = new VmHAConfig("GoldenVM",
         DasConfigInfo.VmMonitoringState.vmAndAppMonitoring,
         30, // vmMonFailInterval
         3, // vmMonMaxFailures
         3600, // vmMonMaxFailuresWindow
         120 // vmMonMinUptime
         );
   public static final VcClusterConfig GoldenConfig = new VcClusterConfig(true, // haEnabled
         true, // admCtlEnabled
         GoldenVmHAConfig, // defaultVmHAConfig
         DasConfigInfo.ServiceState.enabled, // hostMonEnabled
         AdmCtlPolType.FailoverResourcePolicy, // admCtlPol
         5, // resAdmPolCpuPerc
         5, // resAdmPolMemPerc
         true // drsEnabled
         );

   private VcClusterConfig(boolean haEnabled, boolean admCtlEnabled,
         VmHAConfig defaultVmHAConfig,
         DasConfigInfo.ServiceState hostMonEnabled, AdmCtlPolType admCtlPol,
         int resAdmPolCpuPerc, int resAdmPolMemPerc, boolean drsEnabled) {
      this.haEnabled = haEnabled;
      this.admCtlEnabled = admCtlEnabled;
      this.hostMonEnabled = hostMonEnabled;
      this.defaultVmHAConfig = defaultVmHAConfig;
      this.admCtlPol = admCtlPol;
      this.resAdmPolCpuPerc = resAdmPolCpuPerc;
      this.resAdmPolMemPerc = resAdmPolMemPerc;
      this.drsEnabled = drsEnabled;
   }

   public static VcClusterConfig create(ConfigInfoEx config) {
      DasConfigInfo dasConfig = config.getDasConfig();
      DrsConfigInfo drsConfig = config.getDrsConfig();
      DasVmSettings vmSettings = dasConfig.getDefaultVmSettings();
      VmToolsMonitoringSettings toolsSettings =
            vmSettings.getVmToolsMonitoringSettings();
      DasAdmissionControlPolicy pol = dasConfig.getAdmissionControlPolicy();
      AdmCtlPolType polType = AdmCtlPolType.getInstance(pol);
      FailoverResourcesAdmissionControlPolicy resAdmCtlPol =
            (pol instanceof FailoverResourcesAdmissionControlPolicy
                  ? (FailoverResourcesAdmissionControlPolicy) pol
                  : null);
      VmHAConfig defaultVmHAConfig = new VmHAConfig("Cluster VM default setting",
            DasConfigInfo.VmMonitoringState.valueOf(dasConfig.getVmMonitoring()),
            toolsSettings.getFailureInterval(),
            toolsSettings.getMaxFailures(),
            toolsSettings.getMaxFailureWindow(),
            toolsSettings.getMinUpTime());
      return new VcClusterConfig(dasConfig.getEnabled(),
            dasConfig.getAdmissionControlEnabled(),
            defaultVmHAConfig,
            DasConfigInfo.ServiceState.valueOf(dasConfig.getHostMonitoring()),
            polType,
            (resAdmCtlPol != null ? resAdmCtlPol.getCpuFailoverResourcesPercent() : 0),
            (resAdmCtlPol != null ? resAdmCtlPol.getMemoryFailoverResourcesPercent() : 0),
            drsConfig.getEnabled());
   }

   public List<String> getHADRSIncompatReasons(VcClusterConfig reference, boolean strict) {
      List<String> reasons = new ArrayList<String>();
      CommonUtil.checkCond((haEnabled == reference.haEnabled), reasons,
            "Parent cluster HA is " + (haEnabled ? "" : "not ") + "enabled.");
      CommonUtil.checkCond((drsEnabled == reference.drsEnabled), reasons,
            "Parent cluster DRS is " + (drsEnabled ? "" : "not ") + "enabled.");
      CommonUtil.checkCond((hostMonEnabled.equals(reference.hostMonEnabled)), reasons,
            "Parent cluster host monitoring is not " + reference.hostMonEnabled.toString() + ".");
      defaultVmHAConfig.getHADRSIncompatReasons(reference.defaultVmHAConfig, strict);

      if (!strict) {
         CommonUtil.checkCond((admCtlEnabled == reference.admCtlEnabled), reasons,
               "Parent cluster admission control policy is " + (admCtlEnabled ? "" : "not ") + "enabled.");
      }
      return reasons;
   }

   public List<String> getHADRSIncompatReasons(boolean strict) {
      return getHADRSIncompatReasons(GoldenConfig, strict);
   }

   public String toString() {
      StringBuffer buf = new StringBuffer();
      return buf.append("haEnabled").append("=").append(haEnabled).append(";")
            .append("admCtlEnabled").append("=").append(admCtlEnabled)
            .append(";").append(defaultVmHAConfig).append("hostMonEnabled")
            .append("=").append(hostMonEnabled).append(";").append("admCtlPol")
            .append("=").append(admCtlPol).append(";")
            .append("resAdmPolCpuPerc").append("=").append(resAdmPolCpuPerc)
            .append(";").append("resAdmPolMemPerc").append("=")
            .append(resAdmPolMemPerc).append(";").append("drsEnabled")
            .append("=").append(drsEnabled).append(";").toString();
   }

   public VmHAConfig getDefaultVmHAConfig() {
      return defaultVmHAConfig;
   }

   public boolean getHAEnabled() {
      return haEnabled;
   }

   public boolean getDRSEnabled() {
      return drsEnabled;
   }
}
