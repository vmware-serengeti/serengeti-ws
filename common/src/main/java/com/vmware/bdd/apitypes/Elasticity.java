package com.vmware.bdd.apitypes;

/**
 * This class is a DTO of the cluster setElasticity command.
 */
public class Elasticity {
   private String clusterName;
   private boolean enableAutoElasticity;
   private int minComputeNodeNum; // min compute nodes that stay powered on

   public boolean isEnableAutoElasticity() {
      return enableAutoElasticity;
   }
   public void setEnable(boolean enableAutoElasticity) {
      this.enableAutoElasticity = enableAutoElasticity;
   }
   public String getClusterName() {
      return clusterName;
   }
   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }
   public int getMinComputeNodeNum() {
      return minComputeNodeNum;
   }
   public void setMinComputeNodeNum(int minComputeNodeNum) {
      this.minComputeNodeNum = minComputeNodeNum;
   }
}
