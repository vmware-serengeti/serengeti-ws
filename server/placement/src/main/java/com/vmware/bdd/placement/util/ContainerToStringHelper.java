/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.placement.util;

import com.vmware.bdd.placement.Container;
import com.vmware.bdd.placement.entity.AbstractDatacenter;

/**
 * Created by xiaoliangl on 8/18/15.
 */
public class ContainerToStringHelper {
   public static String convertToString(Container container) {
      AbstractDatacenter dc = container.getDc();
      StringBuilder sb = new StringBuilder(dc.getName()).append('\n');

      int clusterIndent = 2;
      for(AbstractDatacenter.AbstractCluster cluster : dc.getClusters()) {
         for(int i = 0; i < clusterIndent; i ++) {
            sb.append('>');
         }
         sb.append(cluster.getName());
         sb.append('\n');
         int hostIndent = 4;
         for(AbstractDatacenter.AbstractHost host : cluster.getHosts()) {
            for(int i = 0; i < hostIndent; i ++) {
               sb.append('>');
            }
            sb.append(host.getName());
            sb.append('\n');
            int datastoreIndent = 6;
            for(AbstractDatacenter.AbstractDatastore datastore : host.getDatastores()) {
               for(int i = 0; i < datastoreIndent; i ++) {
                  sb.append('>');
               }
               sb.append(datastore.getName()).append(':').append(datastore.getFreeSpace()).append("GB");
               sb.append('\n');
            }
         }
      }

      return sb.toString();
   }
}
