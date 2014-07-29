package com.vmware.bdd.software.mgmt.plugin.intf;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;

/**
 * Author: Xiaoding Bian
 * Date: 7/29/14
 * Time: 10:35 AM
 */
public interface PreStartServices {

   void preStartServices(String clusterName, int maxWaitingSeconds) throws SoftwareManagementPluginException;
}
