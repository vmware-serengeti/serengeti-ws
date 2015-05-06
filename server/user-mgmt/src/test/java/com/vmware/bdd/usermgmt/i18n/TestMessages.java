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
package com.vmware.bdd.usermgmt.i18n;


import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class TestMessages {

    @Test
    public void testGetString() {
        String str1Msg = "no configuration items found in database.";
        String str2Msg = "sssd conf template file read err: Network connect failed.";
        String str1 = Messages.getString("MGMT_VM_CFG.CFG_NOT_FOUND");
        String str2 = Messages.getString("SSSD_CONF_TEMPLATE_READ_ERR", "Network connect failed.");
        assertEquals(str1, str1Msg);
        assertEquals(str2, str2Msg);
    }
}
