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
package com.vmware.bdd.service.event;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Author: Xiaoding Bian
 * Date: 1/2/14
 * Time: 1:46 PM
 */
public interface IEventProcessor {

   /**
    * a dedicated thread to produce events and add to produceQueue
    * @param produceQueue
    */
   public void produceEvent(BlockingQueue<IEventWrapper> produceQueue);

   /**
    *
    * @param toProcessEvents
    */
   public void consumeEvent(List<IEventWrapper> toProcessEvents);
}
