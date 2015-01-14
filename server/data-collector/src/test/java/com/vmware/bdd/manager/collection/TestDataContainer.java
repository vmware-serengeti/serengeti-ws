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
package com.vmware.bdd.manager.collection;

import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

@Test
public class TestDataContainer {

   @Test
   public void testPop() {
      final DataContainer dataContainer = new DataContainer();
      Runnable t = new Runnable() {
          @Override
          public void run() {
              while(true) {
                  Map<String, Map<String, Object>> data = dataContainer.pop();
                  assertEquals(data.size(), 2);
                  assertEquals(data.get("1").size(), 6);
                  assertEquals(data.get("2").size(), 6);
                  if (data.size() == 2) {
                      break;
                  }
            }
          }};
       Thread tr = new Thread(t);
       tr.start();
       dataContainer.push("1", "aa", "01");
       dataContainer.push("1", "bb", "02");
       dataContainer.push("1", "cc", "03");
       dataContainer.push("1", "dd", "04");
       dataContainer.push("1", "ee", "05");
       dataContainer.push("1", "ff", "06");

       dataContainer.push("2", "aa", "01");
       dataContainer.push("2", "bb", "02");
       dataContainer.push("2", "cc", "03");
       dataContainer.push("2", "dd", "04");
       dataContainer.push("2", "ee", "05");
       dataContainer.push("2", "ff", "06");
       try {
           tr.join();
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
   }

    @Test
    public void testSetMaxLength () {
        final DataContainer dataContainer = new DataContainer();
        Runnable t = new Runnable() {
            @Override
            public void run() {
                while(true) {
                    Map<String, Map<String, Object>> data = dataContainer.pop();
                    assertEquals(data.size(), 1);
                    assertEquals(data.get("1").size(), 7);
                    if (data.get("1").size() == 7) {
                        break;
                    }
                }
            }};
        Thread tr = new Thread(t);
        tr.start();
        dataContainer.push("1", "ff", "07");
        dataContainer.setMaxLength(7);
        dataContainer.push("1", "gg", "01");
        dataContainer.push("1", "hh", "02");
        dataContainer.push("1", "ii", "03");
        dataContainer.push("1", "jj", "04");
        dataContainer.push("1", "kk", "05");
        dataContainer.push("1", "ll", "06");
        try {
            tr.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
