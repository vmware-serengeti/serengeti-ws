package com.vmware.bdd.software.mgmt.plugin;

/**
 * initialized as a bean
 */

/**
 * Author: Xiaoding Bian
 * Date: 6/5/14
 * Time: 1:17 PM
 */
public class DefaultSoftwareActionListener implements SoftwareActionListener{
   @Override
   public void beforeAction(Object cluster) {
      String action = "start"; // cluster.getAction()
      switch(action) {
         case SoftwareActionListener.CONFIGURE_ACTION:
            beforeConfigure();
            break;
         default:
            beforeOtherAction();
      }
   }

   @Override
   public void afterAction(Object cluster) {


   }

   protected void beforeConfigure() {
      // wait for disk ready

   }

   protected void beforeStart() {

   }

   protected void afterConfigure() {

   }

   protected void afterStart() {

   }

   protected void beforeOtherAction() {

   }

   protected void afterOtherAction() {

   }


}
