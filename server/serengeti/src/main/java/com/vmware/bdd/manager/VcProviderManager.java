/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
package com.vmware.bdd.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.vmware.bdd.entity.CloudProviderConfigEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.spectypes.VcCluster;
import com.vmware.bdd.utils.Configuration;

public class VcProviderManager implements CloudProviderManager {

   enum VcProviderAttribute {
      PROVIDER_TYPE("type"), VC_ADDR_ATTR("vc_addr"), VC_USER_ATTR("vc_user"), VC_PASSWORD_ATTR(
            "vc_pwd"), VC_DATACENTER_ATTR("vc_datacenter"), VC_CLUSTER_ATTR(
            "vc_clusters"), VC_RESOURCE_POOL_ATTR("vc_rps"), VC_SHARED_STORAGE_ATTR(
            "vc_shared_datastore_pattern"), VC_LOCAL_STORAGE_ATTR(
            "vc_local_datastore_pattern"), PROVIDER_NAME("name");

      private String description;

      private VcProviderAttribute(String description) {
         this.description = description;
      }

      public String toString() {
         return this.description;
      }

      public static VcProviderAttribute fromString(String desc) {
         if (desc != null) {
            for (VcProviderAttribute b : VcProviderAttribute.values()) {
               if (desc.equalsIgnoreCase(b.toString())) {
                  return b;
               }
            }
         }
         return null;
      }

   }

   private static final String VSPHERE_PROVIDER_NAME = "vsphere";
   private static final String VC_PROVIDER_TYPE = "VC";
   private VcResourcePoolManager rpMgr;
   private VcDataStoreManager datastoreMgr;
   public Map<String, Object> attributes = new HashMap<String, Object>();

   public VcProviderManager() {
      loadAttributes();
      addDefaultValue();
   }

   public VcDataStoreManager getDatastoreMgr() {
      return datastoreMgr;
   }

   public void setDatastoreMgr(VcDataStoreManager datastoreMgr) {
      this.datastoreMgr = datastoreMgr;
   }

   public VcResourcePoolManager getRpMgr() {
      return rpMgr;
   }

   public void setRpMgr(VcResourcePoolManager rpMgr) {
      this.rpMgr = rpMgr;
   }

   public String getType() {
      return VC_PROVIDER_TYPE;
   }

   public Map<String, Object> getAttributes() {
      Map<String, Object> allAttrs = new HashMap<String, Object>();
      allAttrs.putAll(attributes);
      List<VcCluster> vcClusters = rpMgr.getAllVcResourcePool();
      allAttrs.put(VcProviderAttribute.VC_CLUSTER_ATTR.toString(), vcClusters);
      Set<String> sharedPattern = datastoreMgr.getAllSharedDatastores();
      Set<String> localPattern = datastoreMgr.getAllLocalDatastores();
      if (!localPattern.isEmpty()) {
         allAttrs.put(VcProviderAttribute.VC_LOCAL_STORAGE_ATTR.toString(), localPattern);
      }
      if (!sharedPattern.isEmpty()) {
         allAttrs.put(VcProviderAttribute.VC_SHARED_STORAGE_ATTR.toString(), sharedPattern);
      }
      return allAttrs;
   }

   private void loadAttributes() {
      List<CloudProviderConfigEntity> entities =
            CloudProviderConfigEntity.findAllByType(VC_PROVIDER_TYPE);
      for (CloudProviderConfigEntity entity : entities) {
         String attributeName = entity.getAttribute();
         VcProviderAttribute enumAttr;
         try {
            enumAttr = VcProviderAttribute.fromString(attributeName);
         } catch (IllegalArgumentException e) {
            throw BddException.INTERNAL(e, "invalid attribute name "
                  + attributeName);
         }
         switch (enumAttr) {
         case VC_ADDR_ATTR:
         case VC_USER_ATTR:
         case VC_PASSWORD_ATTR:
         case VC_DATACENTER_ATTR:
            attributes.put(entity.getAttribute(), entity.getValue());
            break;
         default:
            break;
         }
      }
   }

   private void addDefaultValue() {
      for (VcProviderAttribute attr : VcProviderAttribute.values()) {
         if (attributes.get(attr.toString()) != null) {
            continue;
         }

         switch (attr) {
         case PROVIDER_TYPE:
            attributes.put(VcProviderAttribute.PROVIDER_TYPE.toString(),
                  VC_PROVIDER_TYPE);
            break;
         case VC_ADDR_ATTR:
            String vcAddr =
                  Configuration.getString(VcProviderAttribute.VC_ADDR_ATTR
                        .toString());
            attributes.put(VcProviderAttribute.VC_ADDR_ATTR.toString(), vcAddr);
            break;
         case VC_USER_ATTR:
            String vcUser =
                  Configuration.getString(VcProviderAttribute.VC_USER_ATTR
                        .toString());
            attributes.put(VcProviderAttribute.VC_USER_ATTR.toString(), vcUser);
            break;
         case VC_PASSWORD_ATTR:
            String vcPassword =
                  Configuration.getString(VcProviderAttribute.VC_PASSWORD_ATTR
                        .toString());
            attributes.put(VcProviderAttribute.VC_PASSWORD_ATTR.toString(),
                  vcPassword);
            break;
         case VC_DATACENTER_ATTR:
            String vcDatacenter =
                  Configuration
                        .getString(VcProviderAttribute.VC_DATACENTER_ATTR
                              .toString());
            attributes.put(VcProviderAttribute.VC_DATACENTER_ATTR.toString(),
                  vcDatacenter);
            break;
         case PROVIDER_NAME:
            attributes.put(VcProviderAttribute.PROVIDER_NAME.toString(),
                  VSPHERE_PROVIDER_NAME);
            break;
         default:
            break;
         }
      }
   }

   public String getVcAddress() {
      return (String) attributes.get(VcProviderAttribute.VC_ADDR_ATTR
            .toString());
   }

   public String getAdminUser() {
      return (String) attributes.get(VcProviderAttribute.VC_USER_ATTR
            .toString());
   }

   public String getAdminPassword() {
      return (String) attributes.get(VcProviderAttribute.VC_PASSWORD_ATTR
            .toString());
   }

   public String getDataCenter() {
      return (String) attributes.get(VcProviderAttribute.VC_DATACENTER_ATTR
            .toString());
   }

   public String getManifest() {
      Gson gson = new Gson();
      Map<String, Object> attrs = getAttributes();
      return gson.toJson(attrs);
   }
}
