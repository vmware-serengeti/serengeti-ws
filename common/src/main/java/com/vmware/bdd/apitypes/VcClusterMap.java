package com.vmware.bdd.apitypes;

import java.util.Map;

public class VcClusterMap {
   private String src;

   private String tgt;

   private Map<String, String> hosts;

   public String getSrc() {
      return src;
   }

   public void setSrc(String src) {
      this.src = src;
   }

   public String getTgt() {
      return tgt;
   }

   public void setTgt(String tgt) {
      this.tgt = tgt;
   }

   public Map<String, String> getHosts() {
      return hosts;
   }

   public void setHosts(Map<String, String> hosts) {
      this.hosts = hosts;
   }
}