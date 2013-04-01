/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

package com.vmware.bdd.placement.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.vmware.bdd.utils.AuAssert;

abstract class AbstractObject {
   String name;

   public AbstractObject(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      AbstractObject other = (AbstractObject) obj;
      if (name == null) {
         if (other.name != null)
            return false;
      } else if (!name.equals(other.name))
         return false;
      return true;
   }
}

public class AbstractDatacenter extends AbstractObject {

   public static class AbstractDatastore extends AbstractObject implements
         Comparable<AbstractDatastore> {
      // GB
      int freeSpace;

      public AbstractDatastore(String name) {
         super(name);
      }

      public AbstractDatastore(String name, int freeSpace) {
         super(name);
         this.freeSpace = freeSpace;
      }

      public int getFreeSpace() {
         return freeSpace;
      }

      public void setFreeSpace(int freeSpace) {
         this.freeSpace = freeSpace;
      }

      public void allocate(int sizeGB) {
         AuAssert.check(this.freeSpace - sizeGB >= 0);
         this.freeSpace -= sizeGB;
      }

      @Override
      public int compareTo(AbstractDatastore o) {
         if (this.freeSpace == o.freeSpace)
            return 0;
         else if (this.freeSpace < o.freeSpace)
            return -1;
         else
            return 1;
      }

      @Override
      public String toString() {
         return "AbstractDatastore [freeSpace=" + freeSpace + ", name=" + name
               + "]";
      }
   }

   public static class AbstractHost extends AbstractObject {
      transient AbstractCluster parent;
      List<AbstractDatastore> datastores;

      public AbstractHost(String name) {
         super(name);
      }

      public AbstractHost(String name, List<AbstractDatastore> datastores) {
         super(name);
         this.datastores = datastores;
      }

      public AbstractCluster getParent() {
         return parent;
      }

      public void setParent(AbstractCluster parent) {
         this.parent = parent;
      }

      public List<AbstractDatastore> getDatastores() {
         return datastores;
      }

      public void setDatastores(List<AbstractDatastore> datastores) {
         this.datastores = datastores;
      }

      public void addDatastore(AbstractDatastore datastore) {
         if (this.datastores == null)
            this.datastores = new ArrayList<AbstractDatastore>();

         this.datastores.add(datastore);
      }

      public int getTotalSpaceInGB() {
         if (this.datastores == null)
            return 0;

         int total = 0;
         for (AbstractDatastore ds : this.datastores) {
            total += ds.getFreeSpace();
         }

         return total;
      }

      public int getTotalSpaceInGB(String[] namePatterns) {
         if (this.datastores == null || this.datastores.size() == 0)
            return 0;

         int total = 0;
         for (AbstractDatastore ds : this.datastores) {
            for (String pattern : namePatterns) {
               if (ds.name.matches(pattern)) {
                  total += ds.getFreeSpace();
                  break;
               }
            }
         }
         return total;
      }

      /**
       * count the total free space in a list of hosts, be caution to count the
       * shared datastores only once
       * 
       * @param dc
       * @param hosts
       * @return
       */
      public static int getTotalSpaceInGB(List<AbstractHost> hosts) {
         AuAssert.check(hosts != null && hosts.size() != 0);

         int total = 0;
         Set<AbstractDatastore> disjointDs = new HashSet<AbstractDatastore>();

         for (AbstractHost host : hosts) {
            disjointDs.addAll(host.getDatastores());
         }

         for (AbstractDatastore ds : disjointDs) {
            total += ds.getFreeSpace();
         }

         return total;
      }

      public List<AbstractDatastore> getDatastores(String[] namePatterns) {
         if (this.datastores == null || this.datastores.size() == 0)
            return null;

         List<AbstractDatastore> matches = new ArrayList<AbstractDatastore>();

         for (AbstractDatastore ds : this.datastores) {
            for (String pattern : namePatterns) {
               if (ds.getName().matches(pattern) && ds.getFreeSpace() > 0) {
                  matches.add(ds);
                  break;
               }
            }
         }

         return matches;
      }

      /**
       * deep clone a host object, use Gson as the tricky
       * 
       * @param other
       * @return
       */
      public static AbstractHost clone(AbstractHost other) {
         Gson gson = new Gson();
         return gson.fromJson(gson.toJson(other), AbstractHost.class);
      }

      @Override
      public String toString() {
         return "AbstractHost [name=" + name + "]";
      }
   }

   public static class AbstractRp extends AbstractObject {
      public AbstractRp(String name) {
         super(name);
      }

      public static List<AbstractRp> getAbstractRps(List<String> rpNames) {
         List<AbstractRp> rps = new ArrayList<AbstractRp>();

         for (String name : rpNames) {
            rps.add(new AbstractRp(name));
         }

         return rps;
      }

      public static List<String> getRpNames(List<AbstractRp> rps) {
         if (rps == null || rps.size() == 0)
            return null;

         List<String> rpNames = new ArrayList<String>();

         for (AbstractRp rp : rps) {
            rpNames.add(rp.getName());
         }

         return rpNames;
      }
   }

   public static class AbstractCluster extends AbstractObject {
      List<AbstractHost> hosts;

      List<AbstractRp> rps;

      List<AbstractDatastore> datastores;

      public AbstractCluster(String name) {
         super(name);
      }

      public AbstractCluster(String name, List<AbstractHost> hosts) {
         super(name);
         this.hosts = hosts;
      }

      public AbstractCluster(String name, List<AbstractHost> hosts,
            List<AbstractRp> rps) {
         super(name);
         this.hosts = hosts;
         this.rps = rps;
      }

      public List<AbstractHost> getHosts() {
         return hosts;
      }

      public void setHosts(List<AbstractHost> hosts) {
         this.hosts = hosts;
      }

      public void addHost(AbstractHost host) {
         if (this.hosts == null)
            this.hosts = new ArrayList<AbstractHost>(0);
         host.setParent(this);
         this.hosts.add(host);
      }

      public List<AbstractRp> getRps() {
         return rps;
      }

      public void setRps(List<AbstractRp> rps) {
         this.rps = rps;
      }

      public List<AbstractDatastore> getDatastores() {
         return datastores;
      }

      public void setDatastores(List<AbstractDatastore> datastores) {
         this.datastores = datastores;
      }

      public void addDatastore(AbstractDatastore ds) {
         if (this.datastores == null)
            this.datastores = new ArrayList<AbstractDatastore>();

         this.datastores.add(ds);
      }

      public int getTotalSpaceInGB() {
         if (this.datastores == null || this.datastores.size() == 0)
            return 0;

         int total = 0;
         for (AbstractDatastore ds : this.datastores) {
            total += ds.getFreeSpace();
         }

         return total;
      }
   }

   List<AbstractCluster> clusters;

   List<AbstractDatastore> datastores;

   public AbstractDatacenter(String name) {
      super(name);
   }

   public AbstractDatacenter(String name, List<AbstractCluster> clusters,
         List<AbstractDatastore> datastores) {
      super(name);
      this.clusters = clusters;
      this.datastores = datastores;
   }

   public List<AbstractCluster> getClusters() {
      return clusters;
   }

   public void setClusters(List<AbstractCluster> clusters) {
      this.clusters = clusters;
   }

   public List<AbstractDatastore> getDatastores() {
      return datastores;
   }

   public void addDatastore(AbstractDatastore datastore) {
      if (this.datastores == null) {
         this.datastores = new ArrayList<AbstractDatastore>();
      }

      if (!this.datastores.contains(datastore)) {
         this.datastores.add(datastore);
      }

      return;
   }

   public void setDatastores(List<AbstractDatastore> datastores) {
      this.datastores = datastores;
   }

   public void addCluster(AbstractCluster cluster) {
      if (this.clusters == null) {
         this.clusters = new ArrayList<AbstractCluster>();
      }

      if (!this.clusters.contains(cluster)) {
         this.clusters.add(cluster);
      }

      return;
   }

   public AbstractCluster findAbstractCluster(String clusterName) {
      if (this.clusters == null || clusterName == null) {
         return null;
      }

      for (AbstractCluster cluster : this.clusters) {
         if (cluster.getName().equals(clusterName)) {
            return cluster;
         }
      }

      return null;
   }

   public AbstractDatastore findAbstractDatastore(String dsName) {
      if (this.datastores == null) {
         return null;
      }

      for (AbstractDatastore ds : this.datastores) {
         if (ds.getName().equals(dsName)) {
            return ds;
         }
      }

      return null;
   }

   public int getFreeSpaceInGB() {
      if (this.datastores == null || this.datastores.size() == 0)
         return 0;

      int total = 0;
      for (AbstractDatastore ds : this.datastores) {
         total += ds.getFreeSpace();
      }

      return total;
   }

   public int getFreeSpaceInGB(String clusterName) {
      AuAssert.check(clusterName != null && this.clusters != null
            && this.clusters.size() != 0);

      boolean found = false;

      for (AbstractCluster cluster : this.clusters) {
         if (cluster.getName().equals(clusterName)) {
            found = true;
            return cluster.getTotalSpaceInGB();
         }
      }

      AuAssert.check(found);
      return 0;
   }

   public AbstractDatastore getDatastore(String name) {
      for (AbstractDatastore ds : this.getDatastores()) {
         if (ds.getName().equals(name)) {
            return ds;
         }
      }

      return null;
   }

   public List<AbstractHost> getAllHosts() {
      List<AbstractHost> hosts = new ArrayList<AbstractHost>();

      for (AbstractCluster cluster : this.clusters) {
         hosts.addAll(cluster.getHosts());
      }

      return hosts;
   }
}