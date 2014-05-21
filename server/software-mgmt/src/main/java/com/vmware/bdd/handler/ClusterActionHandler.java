package com.vmware.bdd.handler;

/**
 * Author: Xiaoding Bian
 * Date: 5/22/14
 * Time: 5:11 PM
 */
public interface ClusterActionHandler {
   String BOOTSTRAP_ACTION = "bootstrap";
   String CONFIGURE_ACTION = "configure";
   String START_ACTION = "start";
   String STOP_ACTION = "stop";
   String CLEANUP_ACTION = "cleanup";
   String DESTROY_ACTION = "destroy";

   String getRole();

   //void beforeAction(org.apache.whirr.service.ClusterActionEvent clusterActionEvent) throws java.io.IOException, java.lang.InterruptedException;

   //void afterAction(org.apache.whirr.service.ClusterActionEvent clusterActionEvent) throws java.io.IOException, java.lang.InterruptedException;
}
