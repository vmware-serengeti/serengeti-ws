package com.vmware.bdd.plugin.ironfan.utils;

import java.io.File;

/**
 * Created by qjin on 3/12/15.
 */
public class DefaultUtils {
   public static String getConfDir() {
      return com.vmware.bdd.utils.CommonUtil.getConfDir() + File.separator + Constants.DEFAULT_PLUGIN_NAME;
   }
}
