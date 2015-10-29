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
package com.vmware.bdd.manager.collection;

import com.vmware.bdd.util.collection.CollectionConstants;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestTimelyDataProducer {

    private DataContainer dataContainer;
    private CollectionDriver collectionDriver;

    @BeforeMethod(groups = { "TestTimelyDataProducer" })
    public void setMockup() {
        dataContainer =  Mockito.mock(DataContainer.class);
        collectionDriver = Mockito.mock(FakeCollectionDriver.class);
    }

    @Test
    public void testCollect() {
        Map<String, Object> paramData = new HashMap<>();
        long taskId = 100L;
        String resourceId = "1000";
        paramData.put(CollectionConstants.OPERATION_NAME, CollectionConstants.METHOD_CREATE_CLUSTER);
        paramData.put(CollectionConstants.TASK_ID, taskId);
        TimelyDataProducer timelyDataProducer = new TimelyDataProducer(dataContainer, collectionDriver);
        timelyDataProducer.collect(paramData, true);
        String id = CollectionConstants.ASYNCHRONIZATION_PREFIX + taskId;
        verify(dataContainer).push(id ,CollectionConstants.OBJECT_ID, id);
        verify(dataContainer).push(id ,CollectionConstants.OPERATION_NAME, CollectionConstants.METHOD_CREATE_CLUSTER);
        verify(dataContainer).push(id ,CollectionConstants.TASK_ID, taskId);
        paramData.remove(CollectionConstants.TASK_ID);
        paramData.put(CollectionConstants.OBJECT_ID, resourceId);
        timelyDataProducer.collect(paramData, false);
        id = CollectionConstants.SYNCHRONIZATION_PREFIX + resourceId;
        verify(dataContainer).push(id ,CollectionConstants.OBJECT_ID, id);
        verify(dataContainer).push(id ,CollectionConstants.OPERATION_NAME, CollectionConstants.METHOD_CREATE_CLUSTER);
    }
}
