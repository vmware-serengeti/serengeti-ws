package com.vmware.bdd.software.mgmt.plugin;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 6/5/14
 * Time: 11:19 AM
 */
public abstract class SoftwareProvider{
   public static final Logger logger = Logger.getLogger(SoftwareProvider.class);
   private static final Cache CACHE = new Cache();
   private Cache.PluginConf conf;
   private SoftwareActionListener listener;

   public static SoftwareProvider get(Cache.PluginConf conf) {
      return CACHE.get(conf);
   }

   /**
    *
    * @param conf
    * @return
    */
   private static SoftwareProvider createConnector(Cache.PluginConf conf) {
      // new a provider connection instance.
      // inject actionListener
      return null;
   }

   // methods to be exposed to caller
   public void configServices(Object cluster) throws Exception {
      listener.beforeAction(cluster);
      configServicesInternal(cluster);
      listener.afterAction(cluster);
   }

   // methods to be extended
   protected abstract void configServicesInternal(Object cluster);

   static class Cache {
      private final Map<PluginConf, SoftwareProvider> connectors = new HashMap<PluginConf, SoftwareProvider>();

      SoftwareProvider get(PluginConf conf) {
         SoftwareProvider connector = null;
         synchronized (this) {
            connector = connectors.get(conf);
         }
         if (connector != null) {
            return connector;
         }

         connector = createConnector(conf);

         synchronized (this) {
            SoftwareProvider oldConnector = connectors.get(conf); // refetch
            if (oldConnector != null) {
               return oldConnector;
            }
            connector.conf = conf;
            connectors.put(conf, connector);
            return connector;
         }
      }

      synchronized void remove(PluginConf conf, SoftwareProvider connector) {
         if (connectors.containsKey(conf) && connectors.get(conf) == connector) {
            connectors.remove(conf);
         }
      }

      int size() {
         return connectors.size();
      }

      static class PluginConf {
         private String name;
         private String host;
         private int port;
         private String username;

         @Override
         public int hashCode() {
            return 0;
         }

         @Override
         public boolean equals(Object obj) {
            return false;
         }

         @Override
         public String toString() {
            return null;
         }
      }
   }


   public SoftwareActionListener getListener() {
      return listener;
   }

   public void setListener(SoftwareActionListener listener) {
      this.listener = listener;
   }

}
