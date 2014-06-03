package com.vmware.bdd.software.mgmt.plugin.intf;

import java.util.Set;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;

public interface SoftwareManagerCollector {
   /**
    * Software manager name will be unique inside of BDE. Otherwise,
    * registration will fail. Plugin implementation should register itself as a
    * spring bean and in the initialization method, register itself into
    * collector
    * 
    * @param name
    * @param mgr
    */
   void registerSoftwareManager(SoftwareManager mgr)
         throws SoftwareManagementPluginException;

   /**
    * Get software manager instance
    * 
    * @param name
    * @return null if the name does not exist
    */
   SoftwareManager getSoftwareManager(String name);

   /**
    * Get all registered software manager names
    * @return
    */
   Set<String> getSoftwareManagerNames();
}
