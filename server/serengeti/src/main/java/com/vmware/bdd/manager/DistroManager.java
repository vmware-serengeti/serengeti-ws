/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Configuration;

class RolePackageMapping {
   private List<String> roles;
   private String tarball;
   //yum or ubuntu apt repos
   @Expose
   @SerializedName("package_repos")
   private List<String> packageRepos;
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

   public List<String> getPackageRepos() {
      return packageRepos;
   }

   public void setPackageRepos(List<String> packageRepos) {
      this.packageRepos = packageRepos;
   }

}

class Distro {
   private static final Logger logger = Logger.getLogger(Distro.class);

   private String name;
   private Boolean hveSupported;
   private String version; // ignored now
   private List<RolePackageMapping> packages;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Boolean getHveSupported() {
      return hveSupported;
   }

   public void setHveSupported(Boolean hveSupported) {
      this.hveSupported = hveSupported;
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
   
   public DistroRead convert() {
      DistroRead dr = new DistroRead();
      Set<String> roles = new TreeSet<String>();
      if (this.getPackages() == null) {
         return null;
      }
      for (RolePackageMapping pkg : this.getPackages()) {
         if (pkg.getRoles() == null) {
            return null;
         }
         for (String r : pkg.getRoles()) {
            if (r == null || r.isEmpty()) {
               return null;
            }
            if (!roles.contains(r)) {
               roles.add(r);
            } else {
               logger.error("duplicated roles: " + r);
            }
         }
      }

      dr.setName(this.getName());
      dr.setHveSupported(this.getHveSupported() == null ? false : this.getHveSupported());
      dr.setRoles(new ArrayList<String>(roles));
      return dr;
   }

   @Override
   public String toString() {
      return "Distro [name=" + name + ", hveSupported=" + hveSupported + ", version="
            + version + ", packages=" + packages + "]";
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

   public enum PackagesExistStatus {
      NONE, TARBALL, REPO, BOTH
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
      BufferedReader in = null;
      try {
         in = new BufferedReader(
               new InputStreamReader(manifestUrl.openStream()));

         StringBuffer sb = new StringBuffer();
         String line;
         while ((line = in.readLine()) != null) {
            sb.append(line);
         }
         return sb.toString();
      } finally {
         if (in != null) {
            in.close();
         }
      }
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

   public PackagesExistStatus checkPackagesExistStatus(final String distroName) {
      loadManifest(false);
      Distro distro = distros.get(distroName);
      boolean hasPackageRepo = false, hasTarball = false;
      for (RolePackageMapping pkg : distro.getPackages()) {
         List<String> packageRepos = pkg.getPackageRepos();
         if (packageRepos != null && !packageRepos.isEmpty()) {
            hasPackageRepo = true;
         }
         if (!CommonUtil.isBlank(pkg.getTarball())) {
            hasTarball = true;
            if (hasPackageRepo) {
               return PackagesExistStatus.BOTH;
            }
         }
      }
      if (!hasPackageRepo && !hasTarball) {
         return PackagesExistStatus.NONE;
      } else if (hasPackageRepo) {
         return PackagesExistStatus.REPO;
      } else {
         return PackagesExistStatus.TARBALL;
      }
   }

   public List<DistroRead> getDistros() {
      loadManifest(false);
      List<DistroRead> drs = new ArrayList<DistroRead>();

      for (Distro distro : distros.values()) {
         DistroRead dr = distro.convert();
         if (dr != null) {
            drs.add(dr);
         } else {
            logger.error("discard invalid distro: " + distro);
         }
      }

      return drs;
   }

   public DistroRead getDistroByName(String name) {
      loadManifest(false);
      DistroRead dr = null;

      Distro distro = distros.get(name);
      if (distro != null) {
         dr = distro.convert();
         if (dr == null) {
            logger.error("discard invalid distro: " + distro);
         }
      }

      return dr;
   }

   public List<String> getPackageRepos(String distroName) {
      loadManifest(true);
      Distro distro = distros.get(distroName);
      List<String> packageRepos = new ArrayList<String> ();
      for (RolePackageMapping pkg : distro.getPackages()) {
         List<String> subPackageRepos = pkg.getPackageRepos();
         if (subPackageRepos != null && !subPackageRepos.isEmpty()) {
            packageRepos.addAll(subPackageRepos);
         }
      }
      return packageRepos.isEmpty() ? null : packageRepos;
   }
}
