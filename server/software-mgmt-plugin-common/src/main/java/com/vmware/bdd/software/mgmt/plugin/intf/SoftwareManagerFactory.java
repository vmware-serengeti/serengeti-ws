package com.vmware.bdd.software.mgmt.plugin.intf;


public interface SoftwareManagerFactory {
   public SoftwareManager getSoftwareManager(String URL, String username,
         char[] password, String certificate, String type);
}
