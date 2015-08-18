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
