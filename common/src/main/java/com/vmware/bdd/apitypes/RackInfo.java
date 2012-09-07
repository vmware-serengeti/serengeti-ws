package com.vmware.bdd.apitypes;

import java.util.List;

/**
 * Keep <rack_name, hosts> pair
 *
 */
public class RackInfo {
   private String name;
   private List<String> hosts;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public List<String> getHosts() {
      return hosts;
   }

   public void setHosts(List<String> hosts) {
      this.hosts = hosts;
   }
}
