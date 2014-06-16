package com.vmware.bdd.apitypes;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 1:44 PM
 */
public enum SoftwareMgtProvider {
   IRONFAN("Ironfan", false, 9090),
   CLOUDERA_MANAGER("ClouderaManager", true, 7180),
   AMBARI("Ambrari", true, 8080);

   private String name;
   private boolean pluggable;
   private int defaultPort;

   SoftwareMgtProvider(String name, boolean pluggable, int defaultPort) {
      this.name = name;
      this.pluggable = pluggable;
      this.defaultPort = defaultPort;
   }

   public String getName() {
      return name;
   }

   public boolean isPluggable() {
      return pluggable;
   }

   public int getDefaultPort() {
      return defaultPort;
   }

   public static SoftwareMgtProvider fromName(String name) {
      for (SoftwareMgtProvider vendor : values()) {
         if (vendor.getName().equalsIgnoreCase(name)) {
            return vendor;
         }
      }
      throw new IllegalArgumentException("Unknown" + SoftwareMgtProvider.class.getName() + " name: " + name);
   }

}
