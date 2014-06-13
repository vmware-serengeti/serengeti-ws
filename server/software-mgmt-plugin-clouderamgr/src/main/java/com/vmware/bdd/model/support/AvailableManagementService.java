/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.model.support;

import com.vmware.bdd.utils.Constants;

/**
 * Author: Xiaoding Bian
 * Date: 5/21/14
 * Time: 2:45 PM
 */

// Cloudera Manager Service is only available in the Enterprise Edition, the Management Service
// provides monitoring, diagnostic and reporting features for your Hadoop clusters. The operation
// of this service is similar to other Hadoop services, except that the Management Service does not belong to a cluster.
public enum AvailableManagementService {

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

  private AvailableManagementService parent;
  private String id;
  private boolean requireEnterprise;

  private AvailableManagementService(AvailableManagementService parent, String id, boolean requireEnterprise) {
    this.parent = parent;
    this.id = id;
    this.requireEnterprise = requireEnterprise;
  }

  public AvailableManagementService getParent() {
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
