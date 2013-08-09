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
import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.aurora.exception.AuroraException;
import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.stats.Profiler;
import com.vmware.aurora.stats.StatsType;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.util.CmsWorker.Request;
import com.vmware.aurora.util.CmsWorker.WorkQueue;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.impl.vim.vApp.ProductInfoImpl;
import com.vmware.vim.binding.impl.vim.vApp.ProductSpecImpl;
import com.vmware.vim.binding.impl.vim.vApp.PropertyInfoImpl;
import com.vmware.vim.binding.impl.vim.vApp.PropertySpecImpl;
import com.vmware.vim.binding.impl.vim.vApp.VAppConfigSpecImpl;
import com.vmware.vim.binding.impl.vim.vApp.VmConfigSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.vim.option.ArrayUpdateSpec.Operation;
import com.vmware.vim.binding.vim.vApp.ProductInfo;
import com.vmware.vim.binding.vim.vApp.ProductSpec;
import com.vmware.vim.binding.vim.vApp.PropertyInfo;
import com.vmware.vim.binding.vim.vApp.PropertySpec;
import com.vmware.vim.binding.vim.vApp.VAppConfigSpec;
import com.vmware.vim.binding.vim.vApp.VmConfigSpec;
import com.vmware.vim.binding.vim.vm.ConfigSpec;

/**
 * Support VC operations on CMS & LDAP VM.
 */
@SuppressWarnings("serial")
public class CmsVApp implements Serializable {
   private static final Logger logger = Logger.getLogger(CmsVApp.class);

   VcVirtualMachine cms;
   VcVirtualMachine ldap;

   static CmsVApp instance = null;

   public static synchronized CmsVApp getInstance() throws Exception {
      if (instance == null) {
         instance = new CmsVApp();
      }
      return instance;
   }

   public CmsVApp() throws Exception {
      final String cmsRefId = Configuration.getString("vim.cms_moref");
      List<VcVirtualMachine> vms =
         VcContext.inVcSessionDo(new VcSession<List<VcVirtualMachine>>(){
            @Override
            protected List<VcVirtualMachine> body() throws Exception {
               VcVirtualMachine cms = VcCache.get(cmsRefId);
               VcResourcePool rp = cms.getParentVApp();
               List<VcVirtualMachine> vms = rp.getChildVMs();
               if (vms.size() != 2) {
                  logger.error("Unexpected number of VMs in Aurora vApp: " + vms.size());
                  throw AuroraException.INTERNAL();
               }
               // Assume CMS VM leading LDAP VM in the list. Reserve them if not.
               if (cms.equals(vms.get(1))) {
                  vms.set(1, vms.get(0));
                  vms.set(0, cms);
               } else {
                  AuAssert.check(cms.equals(vms.get(0)));
               }
               return vms;
            }
         });
      cms = vms.get(0);
      ldap = vms.get(1);
   }

   public VcVirtualMachine getLDAP() {
      return ldap;
   }

   public VcVirtualMachine getCMS() {
      return cms;
   }

   public List<VcNetwork> getSharedNetworks() throws Exception {
      AuAssert.check(VcContext.isInSession());
      VcResourcePool rp = cms.getParentVApp();
      VcCluster cluster = rp.getVcCluster();
      return cluster.getSharedNetworks();
   }
   /**
    * check and update cms property "InRecovery"
    * if cms doesn't have this property, add full definition;
    * if cms has this property but the definition is different, should edit the property;
    * if all are matched, do nothing.
    */
   private void updateInRecoveryProperty() {
      PropertyInfo[] current = cms.getVAppConfig().getProperty();
      int index = -1;
      PropertySpec propSpec = null;
      if (current != null) {
         index = InRecoveryPropertyDefinition.find(current);
      }
      if (index == -1) {
         //not found, should add this property
         propSpec = new PropertySpecImpl();
         PropertyInfo prop = InRecoveryPropertyDefinition.getProp();
         prop.setKey(current == null ? 0 : current.length);
         propSpec.setInfo(prop);
         propSpec.setOperation(Operation.add);
      } else if (!InRecoveryPropertyDefinition.match(current[index])) {
         //not match the latest definition, update
         propSpec = new PropertySpecImpl();
         PropertyInfo prop = InRecoveryPropertyDefinition.getProp();
         prop.setKey(index);
         propSpec.setInfo(prop);
         propSpec.setOperation(Operation.edit);
      }
      if (propSpec != null) {
         final ConfigSpec config = new ConfigSpecImpl();
         VmConfigSpec spec = new VmConfigSpecImpl();
         spec.setProperty(new PropertySpec[]{propSpec});
         config.setVAppConfig(spec);
         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }
            @Override
            protected Void body() throws Exception {
               cms.reconfigure(config);
               return null;
            }
         });
      }
   }

   /**
    * check and update cms product version
    * if container vApp's version and full version are different from the CMS VM version
    * update them, else do nothing
    */
   private void updateProductInfo() {
      ProductInfo[] cmsProdInfo = cms.getVAppConfig().getProduct();
      String cmsVersion = cmsProdInfo[0].getVersion();
      String cmsFullVersion = cmsProdInfo[0].getFullVersion();
      VcResourcePool vcRp = cms.getParentVApp();
      String vAppVersion = vcRp.getVersion();
      String vAppFullVersion = vcRp.getFullVersion();
      ProductSpec prodSpec = null;

      // If CMS VM version is not the same as the parent vApp version,
      // update the parent vApp version
      if (!cmsVersion.equals(vAppVersion) || !cmsFullVersion.equals(vAppFullVersion)) {
         //not found, should add this property
         prodSpec = new ProductSpecImpl();
         ProductInfo prod = new ProductInfoImpl();
         prod.setKey(0);
         prod.setVersion(cmsVersion);
         prod.setFullVersion(cmsFullVersion);
         prodSpec.setInfo(prod);
         prodSpec.setOperation(Operation.edit);
      }

      if (prodSpec != null) {
         final VAppConfigSpec spec = new VAppConfigSpecImpl();
         spec.setProduct(new ProductSpec[]{prodSpec});
         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }
            @Override
            protected Void body() throws Exception {
               cms.getParentVApp().updateVAppConfig(spec);
               return null;
            }
         });
      }
   }

   public synchronized static void startUpdateCmsProperty() {
      WorkQueue.VC_TASK_NO_DELAY.getQ().add(new UpdateCmsPropertyRequest());
   }

   /*
    * request class to check and update cms property.
    */
   static class UpdateCmsPropertyRequest extends Request {
      public UpdateCmsPropertyRequest() {
         super(Profiler.getStatsEntry(StatsType.VC_UPDATE_CONFIG));
      }

      @Override
      protected boolean execute() {
         try {
            CmsVApp.getInstance().updateInRecoveryProperty();
            CmsVApp.getInstance().updateProductInfo();
         } catch (Throwable t) {
            logger.error("Failed to update cms property, got exception " + t.getMessage());
            //return true to redo this.
            return false;
         }
         //return false to stop updating property.
         return true;
      }

      @Override
      protected void cleanup() {
      }

      @Override
      protected void abort() {
      }
   }
}

class InRecoveryPropertyDefinition {
   private final static PropertyInfo prop = new PropertyInfoImpl();
   static {
      prop.setCategory("Backup and Recovery");
      prop.setId("InRecovery");
      prop.setDescription("Set this ONLY if you are recovering the management server" +
                 " via VMware Data Recovery. After recovery unset it before powering" +
                 " on management server");
      prop.setLabel("Management Server Recovery Flag");
      prop.setType("boolean");
      prop.setDefaultValue("False");
      prop.setUserConfigurable(true);
      prop.setValue("False");
      //leave key to set at runtime.
   }
   public static PropertyInfo getProp() {
      return prop;
   }
   /*
    * find "InRecovery" property if exists, return -1 if not found.
    */
   public static int find(PropertyInfo[] current) {
      AuAssert.check(current != null);
      for (int i = 0; i < current.length; i++) {
         if (current[i].getId().equals(prop.getId())) {
            return i;
         }
      }
      return -1;
   }
   public static boolean match(PropertyInfo current) {
      return (prop.getCategory().equals(current.getCategory()) &&
            prop.getDescription().equals(current.getDescription()) &&
            prop.getLabel().equals(current.getLabel()) &&
            prop.getType().equals(current.getType()) &&
            prop.getUserConfigurable().equals(current.getUserConfigurable()));
   }
}