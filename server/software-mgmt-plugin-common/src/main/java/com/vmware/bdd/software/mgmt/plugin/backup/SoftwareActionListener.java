package com.vmware.bdd.software.mgmt.plugin.backup;

/**
 * Author: Xiaoding Bian
 * Date: 6/5/14
 * Time: 12:47 PM
 */
public interface SoftwareActionListener {

   public static final String CONFIGURE_ACTION = "configure";
   public static final String START_ACTION = "start";
   public static final String STOP_ACTION = "stop";
   public static final String DESTROY_ACTION = "destroy";

   void beforeAction(Object cluster);

   void afterAction(Object cluster);

}
