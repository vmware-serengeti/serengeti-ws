package com.vmware.bdd.software.mgmt.plugin.model;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.List;

/**
 * Hadoop distribution information, for instance CDH5 5.0.1
 * @author line
 *
 */public class HadoopStack implements Serializable{

   private static final long serialVersionUID = -4032296997440707531L;

   @Expose
   private String distro; // for CM, CDH4/CDH5

   @Expose
   private String fullVersion;
   private boolean hveSupported;
   private List<String> roles;

   public String getDistro() {
      return distro;
   }

   public void setDistro(String distro) {
      this.distro = distro;
   }

   public String getFullVersion() {
      return fullVersion;
   }

   public void setFullVersion(String fullVersion) {
      this.fullVersion = fullVersion;
   }

   public boolean isHveSupported() {
      return hveSupported;
   }

   public void setHveSupported(boolean hveSupported) {
      this.hveSupported = hveSupported;
   }

   public List<String> getRoles() {
      return roles;
   }

   public void setRoles(List<String> roles) {
      this.roles = roles;
   }
}
