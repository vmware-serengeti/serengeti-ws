package com.vmware.bdd.apitypes;

import com.cloudera.api.model.ApiClusterVersion;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 4:45 PM
 */
public class CmClusterDef implements Serializable {

   private static final long serialVersionUID = -7460690272330642250L;

   @Expose
   private String name;

   @Expose
   private String displayName;

   @Expose
   private String version; // TODO: relate to ApiClusterVersion, support CDH3, CDH3u4X, CDH4, CDH5, only CDH4/CDH5 has repos

   private ApiClusterVersion apiVersion;

   @Expose
   private String fullVersion;

   @Expose
   private String isParcel;

   @Expose
   private CmNodeDef[] nodes;

   @Expose
   private CmServiceDef[] services;


   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDisplayName() {
      return displayName;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getFullVersion() {
      return fullVersion;
   }

   public void setFullVersion(String fullVersion) {
      this.fullVersion = fullVersion;
   }

   public String getIsParcel() {
      return isParcel;
   }

   public void setIsParcel(String isParcel) {
      this.isParcel = isParcel;
   }

   public CmNodeDef[] getNodes() {
      return nodes;
   }

   public void setNodes(CmNodeDef[] nodes) {
      this.nodes = nodes;
   }

   public CmServiceDef[] getServices() {
      return services;
   }

   public void setServices(CmServiceDef[] services) {
      this.services = services;
   }

   public Set<String> allServiceNames() {
      Set<String> allServiceNames = new HashSet<String>();
      for (CmServiceDef serviceDef : this.services) {
         allServiceNames.add(serviceDef.getName());
      }
      return allServiceNames;
   }

   public Set<CmServiceType> allServiceTypes() {
      Set<CmServiceType> allServiceTypes = new HashSet<CmServiceType>();
      for (CmServiceDef serviceDef : this.services) {
         allServiceTypes.add(CmServiceType.valueOf(serviceDef.getType()));
      }
      return allServiceTypes;
   }

   public String serviceNameOfType(CmServiceType type) {
      for (CmServiceDef serviceDef : this.services) {
         if (type.equals(CmServiceType.valueOf(serviceDef.getType()))) {
            return serviceDef.getName();
         }
      }
      return null;
   }

   public boolean isEmpty() {
      return nodes == null || nodes.length == 0 || services == null || services.length == 0;
   }
}
