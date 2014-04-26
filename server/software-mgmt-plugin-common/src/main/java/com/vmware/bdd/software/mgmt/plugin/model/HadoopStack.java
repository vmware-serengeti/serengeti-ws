package com.vmware.bdd.software.mgmt.plugin.model;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

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
}
