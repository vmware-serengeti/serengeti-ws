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
        assertEquals(SensitiveDataObfuscator.getSensitiveDataFromFile().size(), 54);
        assertEquals(value, "784EBC7877711442A5271F5CF3E39005");
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
    public void testParseStrToMd5U32() {
        String mask = SensitiveDataObfuscator.parseStrToMd5U32("255.255.255.0");
        assertEquals(mask, "454A105061438B142842C0EF875CFBFE");
        String ip = SensitiveDataObfuscator.parseStrToMd5U32("192.168.0.1");
        assertEquals(ip, "F0FDB4C3F58E3E3F8E77162D893D3055");
    }
}