package com.vmware.bdd.model;

import com.vmware.bdd.utils.Constants;

/**
 * Author: Xiaoding Bian
 * Date: 5/21/14
 * Time: 2:45 PM
 */

// Cloudera Manager Service is only available in the Enterprise Edition, the Management Service
// provides monitoring, diagnostic and reporting features for your Hadoop clusters. The operation
// of this service is similar to other Hadoop services, except that the Management Service does not belong to a cluster.
public enum CmMgmtServiceType {

     // Management
  CM(null, "CM", false),

  // Management
  MANAGEMENT(null, "MGMT", false),

  // Host Monitor
  HOSTMONITOR(MANAGEMENT, "HOSTMONITOR", false),

  // Service Monitor
  SERVICEMONITOR(MANAGEMENT, "SERVICEMONITOR", false),

  // Activity Monitor
  ACTIVITYMONITOR(MANAGEMENT, "ACTIVITYMONITOR", false),

  // Alerts Publisher
  ALERTPUBLISHER(MANAGEMENT, "ALERTPUBLISHER", false),

  // Event Server
  EVENTSERVER(MANAGEMENT, "EVENTSERVER", false),

  // Reports Manager
  REPORTSMANAGER(MANAGEMENT, "REPORTSMANAGER", true),

  // Navigator
  NAVIGATOR(MANAGEMENT, "NAVIGATOR", true),

  // Navigator Meta Server
  NAVIGATORMETASERVER(MANAGEMENT, "NAVIGATORMETASERVER", true);

  private CmMgmtServiceType parent;
  private String id;
  private boolean requireEnterprise;

  private CmMgmtServiceType(CmMgmtServiceType parent, String id, boolean requireEnterprise) {
    this.parent = parent;
    this.id = id;
    this.requireEnterprise = requireEnterprise;
  }

  public CmMgmtServiceType getParent() {
    return parent;
  }

  public String getId() {
    return id;
  }

  public boolean requireEnterprise() {
    return requireEnterprise;
  }

  public String getName() {
    return MANAGEMENT.getId().toLowerCase()
        + (getParent() != null ? Constants.CMS_NAME_TOKEN_DELIM + getId().toLowerCase() : "");
  }
}
