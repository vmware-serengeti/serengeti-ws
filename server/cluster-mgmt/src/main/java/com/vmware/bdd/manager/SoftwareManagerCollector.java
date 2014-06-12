package com.vmware.bdd.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;

public class SoftwareManagerCollector {
   private Map<String, SoftwareManager> cache = new HashMap<String, SoftwareManager>();

   /**
    * Software manager name will be unique inside of BDE. Otherwise,
    * creation will fail. The plugin information should be persisted in meta-db
    * 
    * @param name
    */
   public void createSoftwareManager(String name, String type, String URL, String username,
         char[] password, String certificate) throws BddException {
      //TODO: insert information to meta-db, and load class instance with registered factory class
      //the factory class should be registered in a separate xml file
   }

   /**
    * Get software manager instance
    * 
    * @param name
    * @return null if the name does not exist
    */
   public synchronized SoftwareManager getSoftwareManager(String name) {
      if (cache.containsKey(name)) {
         return cache.get(name);
      }

      //TODO:
      return null;
   }

   public void loadSoftwareManagers() {
      // TODO: load all software manager instances into memory while the Tomcat service is started
   }
}
