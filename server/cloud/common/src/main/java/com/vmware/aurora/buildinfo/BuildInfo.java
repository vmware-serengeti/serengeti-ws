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

package com.vmware.aurora.buildinfo;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.interfaces.build.IBuildInfo;
import com.vmware.aurora.util.ResourceManager;

public class BuildInfo extends BuildConfig implements IBuildInfo {
   private static Logger logger = Logger.getLogger(BuildInfo.class);
   /*
    * BuildConfig, which we inherit from, also defines two public final static boolean
    * fields, "debug" and "devel", which can be used for conditional compilation:
    *
    * public final static boolean debug = <depends on build type>;
    * public final static boolean devel = <depends on build type>;
    *
    * And one which defines the build type:
    *
    * public final static BuildType buildType = <depends on build type>;
    *
    * Client code can access these directly if it wishes.
    */

   /*
    * Singleton instance, populated and returned only via getBuildInfo().
    */
   private static BuildInfo buildInfo;

   /*
    * Private instance variables to hold the manifest and its AuroraBuildMetadata section.
    */
   private Manifest buildMetadataManifest;
   private Attributes buildMetadataAttributes;
   private final String manifestDateFormat = "yyyy-MM-dd HH:mm:ss"; // Keep in sync with manifest generation in pom.xml
   private final String deployDateFormat   = "yyyy-MM-dd'T'hh:mm:ssZ";

   /**
    * Singleton factory method.
    * @return The BuildInfo instance.
    */
   public final static synchronized BuildInfo getInstance() {
      if (buildInfo == null) {
         buildInfo = new BuildInfo();
      }

      return buildInfo;
   }

   /*
    * Methods from the BuildInfo interface, referencing static data in the BuildConfig base class
    * (intended for interface clients; class clients can just reference the public static fields
    * directly).
    */

   public final String getBuildType() {
      return buildType.toString();
   }

   public final boolean isDevel() {
      return devel;
   }

   public final boolean isDebug() {
      return debug;
   }

   /*
    * Methods to query build metadata from manifest.
    *
    * Can specify this as one of the IBuildInfo.BuildMetadata defined names, or as an arbitrary string.
    *
    * Note there are 3 cases on how much build metadata will be available:
    * - official build: passes all parameters into build, packages as a jar containing our manifest, all fields available
    * - developer deployed build: doesn't set all build parameters, but does package as a jar containing our manifest, some fields available
    * - developer ad-hoc build (i.e. running via Eclipse): jar doesn't contain the real manifest, no fields available
    * Clients should tolerate null return values.
    */

   /** Get build metadata field by predefined key.
    * @param name: name of field
    * @return Value of field; may be null
    */
   public String getBuildMetadata(IBuildInfo.BuildMetadata name) {
      return getBuildMetadata(name.toString());
   }

   /** Get build metadata field by name.
    * @param name: name of field
    * @return Value of field; may be null
    */
   public String getBuildMetadata(String name) {
      if (buildMetadataAttributes != null) {
         /*
          * Assuming we were able to load the manifest with the Aurora build
          * metadata section, return attributes there.
          */
         Object value = buildMetadataAttributes.getValue(name);
         return (String) value;
      } else {
         /*
          * Can't find manifest section; we must be (and had better be) running inside Eclipse.
          *
          * We know how to fake the value of certain fields just for UI testing.
          */
         if (devel) {
            if (name.equals(BuildMetadata.BUILD_TIME.toString())) {
               // get timestamp for this compilation unit
               URL thisJar = this.getClass().getProtectionDomain().getCodeSource().getLocation();
               File file = new File(thisJar.getFile());
               Date date = new Date(file.lastModified());
               SimpleDateFormat formatter = new SimpleDateFormat(manifestDateFormat, new DateFormatSymbols(Locale.US));
               return formatter.format(date);
            } else if (name.equals(BuildMetadata.BUILD_BUILDTYPE.toString())) {
               return buildType.toString();
            } else if (name.equals(BuildMetadata.PRODUCT_VERSION.toString())) {
               return "IDE build";
            }
         }
         return "N/A";
      }
   }

   public String getBuildDescription(Locale locale) {
      return ResourceManager.getLocalizedText("CommonMessageBundle", locale,
            "com.vmware.aurora.buildinfo.BuildInfo.BUILD_DESCRIPTION");
   }

   public String getBuildDescription() {
      return getBuildDescription(Locale.getDefault());
   }

   /** Get build time as a Java Date object.
    * @return Date that Aurora was built.
    */
   public Date getBuildTime() {
      String dateString = getBuildMetadata(BuildMetadata.BUILD_TIME);
      SimpleDateFormat formatter = new SimpleDateFormat(manifestDateFormat, new DateFormatSymbols(Locale.US));
      try {
         return formatter.parse(dateString);
      } catch (ParseException ex) {
         logger.info("Unable to parse date from " + dateString);
         return null;
      }
   }

   /**
    * Get the time that Aurora was installed. If Aurora has been upgraded,
    * return the last upgrade time.
    */
   public Date getDeployTime() {
      SimpleDateFormat formatter = new SimpleDateFormat(deployDateFormat);
      String time = Configuration.getString("cms.deployDate");
      try {
         return formatter.parse(time);
      } catch (ParseException ex) {
         logger.info("Unable to parse date from " + time);
         return null;
      }
   }

   /** Get list of all build metadata keys from the manifest.
    * @return List of existing keys.
    */
   public String[] getBuildMetadataKeys() {
      ArrayList<String> results = new ArrayList<String>();
      if (buildMetadataAttributes != null) {
         Set<Object> keys = buildMetadataAttributes.keySet();
         for (Object key: keys) {
            results.add((String) key);
         }
      }
      return results.toArray(new String[results.size()]);
   }

   /*
    * Constructor: load the manifest.
    */
   private BuildInfo() {
      try {
         /*
          * Need to find /META-INF/MANIFEST.MF for *this* jar.
          *
          * The standard way of doing this in Java is getClass().getResource("/META-INF/MANIFEST.MF"),
          * but that seems to return the first MANIFEST.MF from the first jar on the classpath, not the
          * one from this jar. I found 4 workarounds:
          * 1) getClass().getProtectionDomain().getCodeSource().getLocation().getPath() gives path to this jar,
          *    which could be used to construct a better call to getResource()
          * 2) use ClassLoader.getResources("/META-INF/MANIFEST.MF") to find *all* the manifests, and
          *    traverse the list to choose one; I couldn't get this to work, probably because I don't
          *    know which ClassLoader to ask.
          * 3) use getClass().getResource() to ask for a resource unique to this class, and parse
          *    parse and munge the resulting resource path into a fully-qualified path to our manifest
          *    and call getResource() again
          * 4) use getClass().getResource() to ask for a resource unique to this class, then cast
          *    the result to JarURLConnection, and ask the connection for its manifest.
          *
          * We'll use #4 because it's least brittle.
          *
          * Note that this all depends on our code being packaged in a jar in the first place! This
          * will be the case for real deployments, but not in developer testing -- i.e. when running
          * from inside Eclipse, or even running from Maven's "test" goal -- so we treat failure to
          * find a manifest as meaning we're running a developer build.
          */
         URL thisJar = this.getClass().getResource(this.getClass().getSimpleName() + ".class");
         JarURLConnection conn = (JarURLConnection) thisJar.openConnection();
         buildMetadataManifest = conn.getManifest();
         buildMetadataAttributes = buildMetadataManifest.getAttributes("AuroraBuildMetadata");
      } catch (Exception ex) {
         buildMetadataManifest = null;
         buildMetadataAttributes = null;
      }
   }

}
