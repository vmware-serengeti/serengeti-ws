package com.vmware.bdd.apitypes;

public class ClusterPriority {
   private Priority diskIOPriority;
   private String nodeGroupName;

   public Priority getDiskIOPriority() {
      return diskIOPriority;
   }
   public void setDiskIOPriority(Priority diskIOPriority) {
      this.diskIOPriority = diskIOPriority;
   }

   public String getNodeGroupName() {
      return nodeGroupName;
   }
   public void setNodeGroupName(String nodeGroupName) {
      this.nodeGroupName = nodeGroupName;
   }

}
