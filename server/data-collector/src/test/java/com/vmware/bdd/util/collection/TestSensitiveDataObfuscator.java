/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.util.collection;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

public class TestSensitiveDataObfuscator {

    @Test(groups = { "SensitiveDataObfuscatorTest" })
    public void testHashSensitiveData() {
        String value = SensitiveDataObfuscator.hashSensitiveData(
                CollectionConstants.OPERATION_NAME, CollectionConstants.METHOD_CONFIG_CLUSTER, null);
        assertEquals(value, CollectionConstants.METHOD_CONFIG_CLUSTER);
        value = SensitiveDataObfuscator.hashSensitiveData(
                "password","1234567abc", null);
        assertEquals(SensitiveDataObfuscator.getSensitiveDataFromFile().size(), 55);
        assertEquals(value, "4D74CDB3F355C750B8FE2E4C86CC062F41CA1C7373A8CD19D2C7C2D6974C3002");
    }

    @Test(groups = { "SensitiveDataObfuscatorTest" })
    public void testGetSensitiveDataFromFile() {
        List<String> sensitiveDataContent = SensitiveDataObfuscator.getSensitiveDataFromFile();
        assertTrue(sensitiveDataContent != null && sensitiveDataContent.size() > 0);
        assertTrue(sensitiveDataContent.contains("ip"));
        assertTrue(sensitiveDataContent.contains("dns1"));
        assertTrue(sensitiveDataContent.contains("dns2"));
        assertTrue(sensitiveDataContent.contains("gateway"));
        assertTrue(sensitiveDataContent.contains("username"));
        assertTrue(sensitiveDataContent.contains("password"));
        assertTrue(sensitiveDataContent.contains("sslCertificate"));
    }

    @Test(groups = { "SensitiveDataObfuscatorTest" })
    public void testParseStrToSHA256() {
        String mask = SensitiveDataObfuscator.parseStrToSHA256("255.255.255.0");
        assertEquals(mask, "F9E2C70401F315FEEBFC5B2A2B7493C10578E8E5CC3D7C7354FCF5F34FEC0DB5");
        String ip = SensitiveDataObfuscator.parseStrToSHA256("192.168.0.1");
        assertEquals(ip, "37D7A80604871E579850A658C7ADD2AE7557D0C6ABCC9B31ECDDC4424207EBA3");
    }
}
