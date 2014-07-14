package com.vmware.bdd.plugin.ambari.model;

public enum ComponentStatus {

   /**
    * Initial/Clean state.
    */
   INIT,
   /**
    * In the process of installing.
    */
   INSTALLING,
   /**
    * Install failed.
    */
   INSTALL_FAILED,
   /**
    * State when install completed successfully.
    */
   INSTALLED,
   /**
    * In the process of starting.
    */
   STARTING,
   /**
    * State when start completed successfully.
    */
   STARTED,
   /**
    * In the process of stopping.
    */
   STOPPING,
   /**
    * In the process of uninstalling.
    */
   UNINSTALLING,
   /**
    * State when uninstall completed successfully.
    */
   UNINSTALLED,
   /**
    * In the process of wiping out the install.
    */
   WIPING_OUT,
   /**
    * In the process of upgrading the deployed bits.
    */
   UPGRADING,
   /**
    * Disabled master's backup state
    */
   DISABLED,
   /**
    * State could not be determined.
    */
   UNKNOWN;

}
