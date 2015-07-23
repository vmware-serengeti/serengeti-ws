package com.vmware.bdd.apitypes;

import java.util.List;

public class DatacenterMap {
   private String src;

   private String tgt;

   private List<VcClusterMap> clusters;

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

   public List<VcClusterMap> getClusters() {
      return clusters;
   }

   public void setClusters(List<VcClusterMap> clusters) {
      this.clusters = clusters;
   }
}