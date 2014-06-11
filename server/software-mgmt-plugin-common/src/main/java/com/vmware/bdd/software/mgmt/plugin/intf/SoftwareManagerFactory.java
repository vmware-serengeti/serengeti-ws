package com.vmware.bdd.software.mgmt.plugin.intf;

/**
 * Factory class to instantiate SoftwareManager
 * @author line
 *
 */
public interface SoftwareManagerFactory {
   public SoftwareManager getSoftwareManager(String URL, String username,
         char[] password, String certificate);
   public String getType();
}
