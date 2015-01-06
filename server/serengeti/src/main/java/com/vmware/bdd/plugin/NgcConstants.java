/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 ***************************************************************************/
package com.vmware.bdd.plugin;

import com.vmware.aurora.global.Configuration;

public class NgcConstants {
   public static final String EVC_SCHEMA =
         "http://www.vmware.com/schema/vservice/ExtensionVService";

   public static final String NGC_KEY = Configuration
         .getString("ngc.extension.info.key");
   public static final String NGC_VERSION = Configuration
         .getString("ngc.extension.info.version");
   public static final String NGC_COMPANY = Configuration
         .getString("ngc.extension.info.company");
   public static final String NGC_PLUGIN_URL_PREFIX = Configuration
         .getString("ngc.extension.info.url_prefix");
   public static final String NGC_PLUGIN_URL_SUFFIX = Configuration
         .getString("ngc.extension.info.url_suffix");
   public static final String NGC_LABEL = Configuration
         .getString("ngc.extension.desc.label");
   public static final String NGC_SUMMARY = Configuration
         .getString("ngc.extension.desc.summary");
   public static final String NGC_CLIENT_TYPE = Configuration
         .getString("ngc.extension.client.type");
   public static final String NGC_ADMIN_EMAIL = Configuration
         .getString("ngc.extension.server.admin_email");
   public static final String NGC_SERVER_TYPE = Configuration
         .getString("ngc.extension.server.type");

   public static final String PLUGIN_PATH = Configuration
         .getString("ngc.plugin.path");
   public static final String PLUGIN_ZIP_NAME = Configuration
         .getString("ngc.plugin.zip_name");
   public static final String PLUGIN_PROPERTIES = Configuration
         .getString("ngc.plugin.properties");

   public static final String NGC_OSVMW_PLUGIN_URL_SUFFIX = Configuration
         .getString("ngc.osvmw.extension.info.url_suffix");
   public static final String NGC_OSVMW_KEY = Configuration
         .getString("ngc.osvmw.extension.info.key");
   public static final String NGC_OSVMW_LABEL = Configuration
         .getString("ngc.osvmw.extension.desc.label");
   public static final String NGC_OSVMW_SUMMARY = Configuration
         .getString("ngc.osvmw.extension.desc.summary");
   public static final String NGC_OSVMW_SERVER_URL_PORT = Configuration
         .getString("ngc.osvmw.extension.server.url_port");
   //public static final String NGC_OSVMW_SERVER_URL_SUFFIX = Configuration.getString("ngc.osvmw.extension.server.url_suffix");
}