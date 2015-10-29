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
package com.vmware.bdd.aop.rest;

import com.vmware.bdd.manager.collection.CollectOperationManager;
import mockit.Mocked;
import mockit.Verifications;
import org.aspectj.lang.JoinPoint;
import org.mockito.Mockito;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.testng.annotations.Test;

public class TestDataCollectorAdvice {

    private @Mocked CollectOperationManager collectOperationManager;
    final JoinPoint joinPoint = Mockito.mock(MethodInvocationProceedingJoinPoint.class);

    @Test(groups = { "TestDataCollectorAdvice" })
    public void testAfterRestCallMethod() {
        DataCollectorAdvice dataCollectorAdvice = new DataCollectorAdvice();
        try {
            dataCollectorAdvice.afterRestCallMethod(joinPoint);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        new Verifications() {{ CollectOperationManager.storeOperationParameters((MethodInvocationProceedingJoinPoint)joinPoint); }};
    }

    @Test(groups = { "TestDataCollectorAdvice" })
    public void testAfterClusterManagerMethod() {
        DataCollectorAdvice dataCollectorAdvice = new DataCollectorAdvice();
        try {
            dataCollectorAdvice.afterClusterManagerMethod(joinPoint, 10L);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        new Verifications() {{ CollectOperationManager.storeOperationParameters((MethodInvocationProceedingJoinPoint)joinPoint, 10L); }};
    }
}
