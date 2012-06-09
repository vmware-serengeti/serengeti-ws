/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
package com.vmware.bdd.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.Configuration;

class RolePackageMapping {
   private List<String> roles;
   private String tarball;

   public List<String> getRoles() {
      return roles;
   }

   public void setRoles(List<String> roles) {
      this.roles = roles;
   }

   public String getTarball() {
      return tarball;
   }

   public void setTarball(String tarball) {
      this.tarball = tarball;
   }
}

class Distro {
   private String name;
   private String version; // ignored now
   private List<RolePackageMapping> packages;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public List<RolePackageMapping> getPackages() {
      return packages;
   }

   public void setPackages(List<RolePackageMapping> packages) {
      this.packages = packages;
   }

   @Override
   public String toString() {
      return "Distro [name=" + name + ", version=" + version + ", packages=" + packages
            + "]";
   }
}

public class DistroManager {
   private static String distroRootUrl = "http://localhost/distros/";
   private static String distrosManifestUrl;
   private static final Logger logger = Logger.getLogger(DistroManager.class);

   static {
      distroRootUrl = Configuration.getString("serengeti.distro_root", distroRootUrl);
      distrosManifestUrl = distroRootUrl + "/manifest";
   }

   private Map<String, Distro> distros = null;

   public DistroManager() {
      try {
         loadManifest(false);
      } catch (BddException ex) {
         logger.error("failed to load distro manifest");
      }
   }

   private String readDistroManifest() throws IOException {
      URL manifestUrl = new URL(distrosManifestUrl);
      BufferedReader in = new BufferedReader(
            new InputStreamReader(manifestUrl.openStream()));

      StringBuffer sb = new StringBuffer();
      String line;
      while ((line = in.readLine()) != null) {
         sb.append(line);
      }

      in.close();
      return sb.toString();
   }

   private void loadManifest(boolean reload) {
      if (distros == null || reload) {
         Map<String, Distro> distrosLoading = new HashMap<String, Distro>();
         Gson gson = new Gson();
         try {
            Type type = new TypeToken<ArrayList<Distro>>() {
            }.getType();
            List<Distro> distrosList = gson.fromJson(readDistroManifest(), type);
            for (Distro d : distrosList) {
               distrosLoading.put(d.getName(), d);
            }
         } catch (JsonSyntaxException e) {
            logger.error("failed to parse manifest: " + distrosManifestUrl, e);
            throw BddException.INTERNAL(e, "failed to parse manifest: "
                  + distrosManifestUrl);
         } catch (IOException e) {
            logger.error("failed to read manifest: " + distrosManifestUrl, e);
            throw BddException
                  .INTERNAL(e, "failed to read manifest: " + distrosManifestUrl);
         }
         distros = distrosLoading;
      }
   }

   public String getPackageUrlByDistroRole(String distroName, String role) {
      loadManifest(false);
      Distro distro = distros.get(distroName);
      if (distro != null) {
         for (RolePackageMapping pkg : distro.getPackages()) {
            for (String r : pkg.getRoles()) {
               if (r.equals(role)) {
                  return distroRootUrl + "/" + pkg.getTarball();
               }
            }
         }
      }
      return null;
   }

   public List<DistroRead> getDistros() {
      loadManifest(false);
      List<DistroRead> drs = new ArrayList<DistroRead>();

      for (Distro distro : distros.values()) {
         DistroRead dr = new DistroRead();
         Set<String> roles = new TreeSet<String>();
         for (RolePackageMapping pkg : distro.getPackages()) {
            for (String r : pkg.getRoles()) {
               if (!roles.contains(r)) {
                  roles.add(r);
               } else {
                  logger.error("duplicated roles: " + r);
               }
            }
         }

         dr.setName(distro.getName());
         dr.setRoles(new ArrayList<String>(roles));
         drs.add(dr);
      }

      return drs;
   }

   public DistroRead getDistroByName(String name) {
      loadManifest(false);
      DistroRead dr = null;

      Distro distro = distros.get(name);
      if (distro != null) {
         dr = new DistroRead();
         Set<String> roles = new TreeSet<String>();
         for (RolePackageMapping pkg : distro.getPackages()) {
            for (String r : pkg.getRoles()) {
               if (!roles.contains(r)) {
                  roles.add(r);
               } else {
                  logger.error("duplicated roles: " + r);
               }
            }
         }

         dr.setName(name);
         dr.setRoles(new ArrayList<String>(roles));
      }

      return dr;
   }
}
