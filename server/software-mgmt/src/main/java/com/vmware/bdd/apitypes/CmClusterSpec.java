package com.vmware.bdd.apitypes;

import com.vmware.bdd.exception.CmException;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Author: Xiaoding Bian
 * Date: 5/22/14
 * Time: 3:18 PM
 */
public class CmClusterSpec implements Serializable {

   private String name;

   private String displayName;

   private String version;

   private String fullVersion;

   private boolean isParcel = true;

   private CmServiceSpec server;

   private Set<CmServiceSpec> agents = new HashSet<CmServiceSpec>();
   private Set<CmServiceSpec> nodes = new HashSet<CmServiceSpec>();

   private Map<String, Map<String, Map<String, String>>> configuration = new HashMap<String, Map<String, Map<String, String>>>();

   private Map<CmServiceType, Set<CmServiceSpec>> services = new HashMap<CmServiceType, Set<CmServiceSpec>>();

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public boolean getIsParcel() {
      return isParcel;
   }

   public void setIsParcel(boolean isParcel) {
      this.isParcel = isParcel;
   }

   public Map<String, Map<String, Map<String, String>>> getConfiguration() {
      return configuration;
   }

   public void setConfiguration(Map<String, Map<String, Map<String, String>>> configuration) {
      this.configuration = configuration;
   }

   public synchronized boolean isEmpty() {
      for (CmServiceType type : services.keySet()) {
         if (!services.get(type).isEmpty()) {
            return server == null || agents.isEmpty() && nodes.isEmpty();
         }
      }
      return true;
   }

   public synchronized void addServiceConfiguration(String versionApi, String group, String setting, String value)
         throws CmException {
      if (configuration.get(versionApi) == null) {
         configuration.put(versionApi, new HashMap<String, Map<String, String>>());
      }
      if (configuration.get(versionApi).get(group) == null) {
         configuration.get(versionApi).put(group, new HashMap<String, String>());
      }
      configuration.get(versionApi).get(group).put(setting, value);
   }

   public synchronized void addServiceConfigurationAll(Map<String, Map<String, Map<String, String>>> configuration)
         throws CmException {
      for (String versionApi : configuration.keySet()) {
         for (String group : configuration.get(versionApi).keySet()) {
            for (String setting : configuration.get(versionApi).get(group).keySet()) {
               addServiceConfiguration(versionApi, group, setting, configuration.get(versionApi).get(group).get(setting));
            }
         }
      }
   }

   public synchronized boolean addServiceType(CmServiceType type) throws CmException {
      if (type.getParent() == null || type.getParent().getParent() == null) {
         throw new CmException("Invalid cluster topology: Attempt to add non leaf type [" + type + "]");
      }
      switch (type) {
         case HDFS_NAMENODE:
            if (getServices(CmServiceType.HDFS_NAMENODE).size() > 0) {
               throw new CmException("Invalid cluster topology: Attempt to add multiple types [" + type + "]");
            }
            break;
         default:
            break;
      }
      if (!services.containsKey(type.getParent())) {
         services.put(type.getParent(), new TreeSet<CmServiceSpec>());
         return true;
      }
      return false;
   }

   public synchronized boolean addService(CmServiceSpec service) throws CmException {
      addServiceType(service.getType());
      if (services.containsKey(service.getType().getParent())) {
         services.get(service.getType().getParent()).add(service);
      }
      return true;
   }




   public synchronized boolean setServer(CmServiceSpec server) throws CmException {
      if (this.server != null) {
         throw new CmException("Invalid cluster topology: Attempt to add multiple servers with existing server "
               + this.server + " and new server " + server);
      }
      return (this.server = server) != null;
   }

   public synchronized boolean addAgent(CmServiceSpec agent) throws CmException {
      if (!agents.add(agent)) {
         throw new CmException("Invalid cluster topology: Attempt to add col-located agents");
      }
      return true;
   }

   public synchronized boolean addNode(CmServiceSpec node) throws CmException {
      if (!nodes.add(node)) {
         throw new CmException("Invalid cluster topology: Attempt to add co-located nodes");
      }
      return true;
   }

   public synchronized Set<CmServiceType> getServiceTypes() {
      return new TreeSet<CmServiceType>(services.keySet());
   }

   public synchronized Set<CmServiceType> getServiceTypes(int versionApi, int versionCdh) {
      Set<CmServiceType> types = new TreeSet<CmServiceType>();
      for (CmServiceType type : services.keySet()) {
         if (type.isValid(versionApi, versionCdh)) {
            types.add(type);
         }
      }
      return types;
   }

   public synchronized Set<CmServiceType> getServiceTypes(CmServiceType type) {
      return getServiceTypes(type, CmServiceSpec.VERSION_UNBOUNDED, CmServiceSpec.VERSION_UNBOUNDED);
   }

   public synchronized Set<CmServiceType> getServiceTypes(CmServiceType type, int versionApi, int versionCdh) {
      Set<CmServiceType> types = new TreeSet<CmServiceType>();
      if (type.equals(CmServiceType.CLUSTER)) {
         for (CmServiceType serviceType : services.keySet()) {
            for (CmServiceSpec service : services.get(serviceType)) {
               if (service.getType().isValid(versionApi, versionCdh)) {
                  types.add(service.getType());
               }
            }
         }
      } else if (services.containsKey(type)) {
         for (CmServiceSpec service : services.get(type)) {
            if (service.getType().isValid(versionApi, versionCdh)) {
               types.add(service.getType());
            }
         }
      } else if (services.containsKey(type.getParent())) {
         for (CmServiceSpec service : services.get(type.getParent())) {
            if (service.getType().equals(type)) {
               if (service.getType().isValid(versionApi, versionCdh)) {
                  types.add(service.getType());
               }
            }
         }
      }
      return types;
   }

   public synchronized CmServiceSpec getService(CmServiceType type) {
      return getService(type, CmServiceSpec.VERSION_UNBOUNDED, CmServiceSpec.VERSION_UNBOUNDED);
   }

   public synchronized CmServiceSpec getService(CmServiceType type, int vesionApi, int versionCdh) {
      Set<CmServiceSpec> serviceCopy = getServices(type, vesionApi, versionCdh);
      return serviceCopy.size() == 0 ? null : serviceCopy.iterator().next();
   }

   public synchronized Set<CmServiceSpec> getServices(CmServiceType type) {
      return getServices(type, CmServiceSpec.VERSION_UNBOUNDED, CmServiceSpec.VERSION_UNBOUNDED);
   }

   public synchronized Set<CmServiceSpec> getServices(CmServiceType type, int versionApi, int versionCdh) {
      Set<CmServiceSpec> servicesCopy = new TreeSet<CmServiceSpec>();
      if (type.equals(CmServiceType.CLUSTER)) {
         for (CmServiceType serviceType : services.keySet()) {
            if (type.isValid(versionApi, versionCdh)) {
               for (CmServiceSpec serviceTypeSub : services.get(serviceType)) {
                  if (serviceTypeSub.getType().isValid(versionApi, versionCdh)) {
                     servicesCopy.add(serviceTypeSub);
                  }
               }
            }
         }
      } else if (services.containsKey(type)) {
         for (CmServiceSpec serviceTypeSub : services.get(type)) {
            if (serviceTypeSub.getType().isValid(versionApi, versionCdh)) {
               servicesCopy.add(serviceTypeSub);
            }
         }
      } else if (services.containsKey(type.getParent())) {
         for (CmServiceSpec service : services.get(type.getParent())) {
            if (service.getType().equals(type)) {
               if (service.getType().isValid(versionApi, versionCdh)) {
                  servicesCopy.add(service);
               }
            }
         }
      }
      return servicesCopy;
   }

   public synchronized String getServiceName(CmServiceType type) throws IOException {
      if (type.equals(CmServiceType.CLUSTER) && name != null) {
         return name;
      }
      if (services.get(type) != null) {
         CmServiceSpec service = services.get(type).iterator().next();
         if (service.getType().equals(type)) {
            return service.getName();
         } else {
            //TODO
            //return new CmServiceSpecBuilder().type(type).tag(service.getTag()).build().getName();
         }
      } else {
         Set<CmServiceSpec> servicesChild = null;
         if (!services.isEmpty() && !(servicesChild = services.get(getServiceTypes().iterator().next())).isEmpty()) {
            // TODO
            //return new CmServiceSpecBuilder().type(type).tag(servicesChild.iterator().next().getTag()).build().getName();
         }
      }
      throw new IOException("Cannot determine service name, cluster is empty");
   }

   public synchronized CmServiceSpec getServer() {
      return server;
   }

   public synchronized Set<CmServiceSpec> getAgents() {
      return new HashSet<CmServiceSpec>(agents);
   }

   public synchronized Set<CmServiceSpec> getNodes() {
      return new HashSet<CmServiceSpec>(nodes);
   }

   public synchronized Map<String, Map<String, Map<String, String>>> getServiceConfiguration() {
      return configuration;
   }

   public synchronized Map<String, Map<String, String>> getServiceConfiguration(int versionApi) {
      Map<String, Map<String, String>> configuration = new HashMap<String, Map<String, String>>();
      for (String configVersion : this.configuration.keySet()) {
         for (String configGroup : this.configuration.get(configVersion).keySet()) {
            for (String configSetting : this.configuration.get(configVersion).get(configGroup).keySet()) {
               if (StringUtils.isNumeric(configVersion)) {
                  if (versionApi < 0 || Integer.parseInt(configVersion) <= versionApi) {
                     if (configuration.get(configGroup) == null) {
                        configuration.put(configGroup, new HashMap<String, String>());
                     }
                     configuration.get(configGroup).put(configSetting,
                           this.configuration.get(configVersion).get(configGroup).get(configSetting));
                  }
               }
            }
         }
      }
      return configuration;
   }
}
