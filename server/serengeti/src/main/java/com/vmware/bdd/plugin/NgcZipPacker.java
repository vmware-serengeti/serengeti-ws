/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

/**
 * Compress the ngc plugin files into a zip file for vc to download after
 * registration.
 * 
 */
public class NgcZipPacker {

   private static final Logger LOGGER = Logger.getLogger(NgcZipPacker.class);
   private static String pluginPath = NgcConstants.PLUGIN_PATH;
   private static String pluginProperties = NgcConstants.PLUGIN_PROPERTIES;
   private File ngcFile;
   private ZipFile ngcZipFile;
   private Properties properties;

   public NgcZipPacker(Properties properties, String packageName) {
      this.properties = properties;
      String ngcFilePath = pluginPath + packageName+".zip";
      ngcFile = new File(ngcFilePath);
   }

   public void repack() throws ZipException, IOException {
      LOGGER.debug("Repacking NGC zipfile...");
      try {
         ngcZipFile = new ZipFile(ngcFile);
         Enumeration<? extends ZipEntry> ngcZipEngtires = unpackZipFile();
         ZipEntry serviceEntry = findServiceEntry(ngcZipEngtires);
         if (serviceEntry != null) {
            File propertiesFile = generatePropertiesFile();
            propertiesFile.deleteOnExit();
            packZipFile(serviceEntry, propertiesFile);
            LOGGER.debug("NGC zipfile repacking finished.");
         } else {
            LOGGER.error("Packing error: service jar not found.");
         }
      } finally {
         if (ngcZipFile != null) {
            try {
               ngcZipFile.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close ngc zip file.");
            }
         }
      }
   }

   private void packZipFile(ZipEntry serviceEntry, File propertiesFile)
         throws IOException {
      // NOTE: in order to repack the zip file, we will copy everything
      // under the legacy zip file to tempfile, make modifications and
      // then copy them back to the original zip file

      FileOutputStream serviceEntryJarStreamTmp = null;

      try {
         LOGGER.info("add property file to zip package");
         InputStream serviceEntryStream =
               ngcZipFile.getInputStream(serviceEntry);
         File serviceEntryTemp = File.createTempFile("serviceEntryTmp", null);
         serviceEntryJarStreamTmp = new FileOutputStream(serviceEntryTemp);
         streamWriter(serviceEntryStream, serviceEntryJarStreamTmp);
         serviceEntryJarStreamTmp.close();
         serviceEntryStream.close();

         String propertiesFilePath = pluginProperties;
         String serviceEntryJarPath = serviceEntry.getName();
         addFileToJar(serviceEntryTemp, propertiesFile, propertiesFilePath);
         addFileToZip(ngcFile, serviceEntryTemp, serviceEntryJarPath);
         serviceEntryTemp.delete();
      } catch (Exception e) {
         LOGGER.error("Error to close serviceEntryJarStreamTmp.");
      } finally {
         if (serviceEntryJarStreamTmp != null) {
            try {
               serviceEntryJarStreamTmp.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close serviceEntryJarStreamTmp.");
            }
         }
      }
   }

   private File generatePropertiesFile() throws IOException {
      String propertiesPath = pluginPath + pluginProperties;
      File propertiesFile = new File(propertiesPath);
      FileOutputStream propertiesStream = null;
      try {
         propertiesStream = new FileOutputStream(propertiesFile);
         String comments = "Big data NGC plugin properties";
         properties.store(propertiesStream, comments);
         return new File(propertiesPath);
      } finally {
         if (propertiesStream != null) {
            try {
               propertiesStream.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close propertiesStream.");
            }
         }
      }
   }

   private ZipEntry findServiceEntry(
         Enumeration<? extends ZipEntry> ngcZipEngtires) {
      while (ngcZipEngtires.hasMoreElements()) {
         ZipEntry entry = ngcZipEngtires.nextElement();
         if (entry.getName().contains("service")) {
            return entry;
         }
      }
      return null;
   }

   private Enumeration<? extends ZipEntry> unpackZipFile() throws ZipException,
         IOException {
      return ngcZipFile.entries();
   }

   private void addFileToZip(File source, File file, String filePath)
         throws IOException {
      File tempZip = File.createTempFile(source.getName(), null);
      tempZip.delete();
      source.renameTo(tempZip);

      ZipInputStream zipSource = null;
      ZipOutputStream zipDest = null;

      // write the file to be added first
      InputStream fileIn = null;

      try {
         fileIn = new FileInputStream(file);
         zipSource = new ZipInputStream(new FileInputStream(tempZip));
         zipDest = new ZipOutputStream(new FileOutputStream(source));

         zipDest.putNextEntry(new ZipEntry(filePath));
         streamWriter(fileIn, zipDest);
         zipDest.closeEntry();

         // copy other files in the original jar file
         for (ZipEntry zipEntry = zipSource.getNextEntry(); zipEntry != null; zipEntry =
               zipSource.getNextEntry()) {
            if (!zipEntry.getName().equals(filePath)) {
               zipDest.putNextEntry(zipEntry);
               streamWriter(zipSource, zipDest);
               zipDest.closeEntry();
            }
         }

         tempZip.delete();
      } finally {
         if (fileIn != null) {
            try {
               fileIn.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close file.");
            }
         }
         if (zipSource != null) {
            try {
               zipSource.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close zip source file.");
            }
         }
         if (zipDest != null) {
            try {
               zipDest.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close zip dest file.");
            }
         }
      }
   }

   private void addFileToJar(File source, File file, String filePath)
         throws IOException {
      File tempJar = File.createTempFile(source.getName(), null);
      tempJar.delete();
      source.renameTo(tempJar);

      InputStream fileIn = null;

      FileInputStream fileSource = null;
      FileOutputStream fileDest = null;
      JarInputStream jarSource = null;
      JarOutputStream jarDest = null;

      try {
         fileSource = new FileInputStream(tempJar);
         jarSource = new JarInputStream(fileSource);

         fileDest = new FileOutputStream(source);
         if (jarSource.getManifest() == null) {
            jarDest = new JarOutputStream(fileDest);
         } else {
            jarDest = new JarOutputStream(fileDest, jarSource.getManifest());
         }

         // write the file to be added first
         fileIn = new FileInputStream(file);
         jarDest.putNextEntry(new ZipEntry(filePath));
         streamWriter(fileIn, jarDest);
         jarDest.closeEntry();

         // copy other files in the original jar file
         for (ZipEntry jarEntry = jarSource.getNextEntry(); jarEntry != null; jarEntry =
               jarSource.getNextEntry()) {
            if (!jarEntry.getName().equals(filePath)) {
               jarDest.putNextEntry(jarEntry);
               streamWriter(jarSource, jarDest);
               jarDest.closeEntry();
            }
         }

         tempJar.delete();
      } finally {
         if (fileIn != null) {
            try {
               fileIn.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close file.");
            }
         }
         if (jarSource != null) {
            try {
               jarSource.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close jar source file.");
            }
         }
         if (jarDest != null) {
            try {
               jarDest.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close jar dest file.");
            }
         }
         if (fileSource != null) {
            try {
               fileSource.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close fileSource.");
            }
         }
         if (fileDest != null) {
            try {
               fileDest.close();
            } catch (IOException ex) {
               LOGGER.error("Error to close fileDest.");
            }
         }
      }
   }

   private void streamWriter(InputStream source, OutputStream dest)
         throws IOException {
      byte[] buffer = new byte[4096];
      for (int read = source.read(buffer); read > -1; read =
            source.read(buffer)) {
         dest.write(buffer, 0, read);
      }
   }
}
