/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.usermgmt;

/**
 * Created By xiaoliangl on 12/15/14.
 */
public interface UserMgmtConstants {
   String VMCONFIG_MGMTVM_CUM_MODE = "vmconfig.mgmtvm.cum.mode";
   String VMCONFIG_MGMTVM_CUM_SERVERNAME = "vmconfig.mgmtvm.cum.servername";
   String DEFAULT_USERMGMT_SERVER_NAME = "default";

   String ADMIN_GROUP_NAME = "admin_group_name";
   String USER_GROUP_NAME = "user_group_name";
   String USERMGMT_SERVER_NAME = "user_management_server_name";

   String LDAP_USER_MANAGEMENT = "ldap_user_management";
   String DISABLE_LOCAL_USER_FLAG = "disable_local_user";

   String SERVICE_USER_TYPE = "user_type";
   String SERVICE_USER_CONFIG_IN_SPEC_FILE = "service_user";
   String SERVICE_USER_NAME = "user_name";
   String SERVICE_USER_GROUP = "user_group";
}
