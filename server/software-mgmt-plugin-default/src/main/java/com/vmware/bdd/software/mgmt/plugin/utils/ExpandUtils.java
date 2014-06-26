package com.vmware.bdd.software.mgmt.plugin.utils;

import org.apache.commons.configuration.ConversionException;
import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.InstanceType;
import com.vmware.bdd.spectypes.GroupType;

public class ExpandUtils {
   private static final Logger logger = Logger.getLogger(ExpandUtils.class);
   private static int[][] templateStorage;
   static {
      initTemplateValues();
   }

   private static void initTemplateValues() {
      templateStorage = new int[GroupType.values().length][InstanceType.values().length];
      int value;

      value = setTemplateStorage("storage.defaultgroup.extralarge",
            GroupType.DEFAULT_GROUP.ordinal(), InstanceType.EXTRA_LARGE.ordinal(), 120);
      templateStorage[GroupType.DEFAULT_GROUP.ordinal()][InstanceType.EXTRA_LARGE.ordinal()] = value;
      logger.debug("extra large storage of default group  is " + value + "GB.");

      value = setTemplateStorage("storage.defaultgroup.large",
            GroupType.DEFAULT_GROUP.ordinal(), InstanceType.LARGE.ordinal(), 80);
      templateStorage[GroupType.DEFAULT_GROUP.ordinal()][InstanceType.LARGE.ordinal()] = value;
      logger.debug("large storage of default group  is " + value + "GB.");

      value = setTemplateStorage("storage.defaultgroup.medium",
            GroupType.DEFAULT_GROUP.ordinal(), InstanceType.MEDIUM.ordinal(), 40);
      templateStorage[GroupType.DEFAULT_GROUP.ordinal()][InstanceType.MEDIUM.ordinal()] = value;
      logger.debug("medium storage of default group  is " + value + "GB.");

      value = setTemplateStorage("storage.defaultgroup.small",
            GroupType.DEFAULT_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 20);
      templateStorage[GroupType.DEFAULT_GROUP.ordinal()][InstanceType.SMALL.ordinal()] = value;
      logger.debug("small storage of default group  is " + value + "GB.");

      value = setTemplateStorage("storage.mastergroup.extralarge",
            GroupType.MASTER_GROUP.ordinal(), InstanceType.EXTRA_LARGE.ordinal(), 200);
      templateStorage[GroupType.MASTER_JOBTRACKER_GROUP.ordinal()][InstanceType.EXTRA_LARGE.ordinal()] = value;
      templateStorage[GroupType.YARN_RESOURCEMANAGER_GROUP.ordinal()][InstanceType.EXTRA_LARGE.ordinal()] = value;
      templateStorage[GroupType.HBASE_MASTER_GROUP.ordinal()][InstanceType.EXTRA_LARGE.ordinal()] = value;
      templateStorage[GroupType.MAPR_MASTER_GROUP.ordinal()][InstanceType.EXTRA_LARGE.ordinal()] = value;
      logger.debug("extra large storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.mastergroup.large",
            GroupType.MASTER_GROUP.ordinal(), InstanceType.LARGE.ordinal(), 100);
      templateStorage[GroupType.MASTER_JOBTRACKER_GROUP.ordinal()][InstanceType.LARGE.ordinal()] = value;
      templateStorage[GroupType.YARN_RESOURCEMANAGER_GROUP.ordinal()][InstanceType.LARGE.ordinal()] = value;
      templateStorage[GroupType.HBASE_MASTER_GROUP.ordinal()][InstanceType.LARGE.ordinal()] = value;
      templateStorage[GroupType.MAPR_MASTER_GROUP.ordinal()][InstanceType.LARGE.ordinal()] = value;
      logger.debug("large storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.mastergroup.medium",
            GroupType.MASTER_GROUP.ordinal(), InstanceType.MEDIUM.ordinal(), 50);
      templateStorage[GroupType.MASTER_JOBTRACKER_GROUP.ordinal()][InstanceType.MEDIUM.ordinal()] = value;
      templateStorage[GroupType.YARN_RESOURCEMANAGER_GROUP.ordinal()][InstanceType.MEDIUM.ordinal()] = value;
      templateStorage[GroupType.HBASE_MASTER_GROUP.ordinal()][InstanceType.MEDIUM.ordinal()] = value;
      templateStorage[GroupType.MAPR_MASTER_GROUP.ordinal()][InstanceType.MEDIUM.ordinal()] = value;
      logger.debug("medium storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.mastergroup.small",
            GroupType.MASTER_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 25);
      templateStorage[GroupType.MASTER_JOBTRACKER_GROUP.ordinal()][InstanceType.SMALL.ordinal()] =  value;
      templateStorage[GroupType.YARN_RESOURCEMANAGER_GROUP.ordinal()][InstanceType.SMALL.ordinal()] =  value;
      templateStorage[GroupType.HBASE_MASTER_GROUP.ordinal()][InstanceType.SMALL.ordinal()] = value;
      templateStorage[GroupType.MAPR_MASTER_GROUP.ordinal()][InstanceType.SMALL.ordinal()] = value;
      logger.debug("small storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.extralarge",
            GroupType.WORKER_GROUP.ordinal(), InstanceType.EXTRA_LARGE.ordinal(), 400);
      templateStorage[GroupType.MAPR_WORKER_GROUP.ordinal()][InstanceType.EXTRA_LARGE.ordinal()] = value;
      logger.debug("extra large storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.large",
            GroupType.WORKER_GROUP.ordinal(), InstanceType.LARGE.ordinal(), 200);
      templateStorage[GroupType.MAPR_WORKER_GROUP.ordinal()][InstanceType.LARGE.ordinal()] = value;
      logger.debug("large storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.medium",
            GroupType.WORKER_GROUP.ordinal(), InstanceType.MEDIUM.ordinal(), 100);
      templateStorage[GroupType.MAPR_WORKER_GROUP.ordinal()][InstanceType.MEDIUM.ordinal()] = value;
      logger.debug("medium storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.small",
            GroupType.WORKER_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 50);
      templateStorage[GroupType.MAPR_WORKER_GROUP.ordinal()][InstanceType.SMALL.ordinal()] = value;
      logger.debug("small storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.clientgroup.extralarge",
            GroupType.CLIENT_GROUP.ordinal(), InstanceType.EXTRA_LARGE.ordinal(), 400);
      templateStorage[GroupType.MAPR_CLIENT_GROUP.ordinal()][InstanceType.EXTRA_LARGE.ordinal()] = value;
      logger.debug("extral large storage of client group  is " + value + "GB.");

      value = setTemplateStorage("storage.clientgroup.large",
            GroupType.CLIENT_GROUP.ordinal(), InstanceType.LARGE.ordinal(), 200);
      templateStorage[GroupType.MAPR_CLIENT_GROUP.ordinal()][InstanceType.LARGE.ordinal()] = value;
      logger.debug("large storage of client group  is " + value + "GB.");

      value = setTemplateStorage("storage.clientgroup.medium",
            GroupType.CLIENT_GROUP.ordinal(), InstanceType.MEDIUM.ordinal(), 100);
      templateStorage[GroupType.MAPR_CLIENT_GROUP.ordinal()][InstanceType.MEDIUM.ordinal()] = value;
      logger.debug("medium storage of client group  is " + value + "GB.");

      value = setTemplateStorage("storage.clientgroup.small",
            GroupType.CLIENT_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 50);
      templateStorage[GroupType.MAPR_CLIENT_GROUP.ordinal()][InstanceType.SMALL.ordinal()] = value;
      logger.debug("small storage of client group  is " + value + "GB.");

      value = setTemplateStorage("storage.zookeepergroup.extralarge",
            GroupType.ZOOKEEPER_GROUP.ordinal(), InstanceType.EXTRA_LARGE.ordinal(), 120);
      templateStorage[GroupType.MAPR_ZOOKEEPER_GROUP.ordinal()][InstanceType.EXTRA_LARGE.ordinal()] = value;
      logger.debug("extra large storage of zookeeper group  is " + value + "GB.");

      value = setTemplateStorage("storage.zookeepergroup.large",
            GroupType.ZOOKEEPER_GROUP.ordinal(), InstanceType.LARGE.ordinal(), 80);
      templateStorage[GroupType.MAPR_ZOOKEEPER_GROUP.ordinal()][InstanceType.LARGE.ordinal()] = value;
      logger.debug("large storage of zookeeper group  is " + value + "GB.");

      value = setTemplateStorage("storage.zookeepergroup.medium",
            GroupType.ZOOKEEPER_GROUP.ordinal(), InstanceType.MEDIUM.ordinal(), 40);
      templateStorage[GroupType.MAPR_ZOOKEEPER_GROUP.ordinal()][InstanceType.MEDIUM.ordinal()] = value;
      logger.debug("medium storage of zookeeper group  is " + value + "GB.");

      value = setTemplateStorage("storage.zookeepergroup.small",
            GroupType.ZOOKEEPER_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 20);
      templateStorage[GroupType.MAPR_ZOOKEEPER_GROUP.ordinal()][InstanceType.SMALL.ordinal()] = value;
      logger.debug("small storage of zookeeper group  is " + value + "GB.");

      value = setTemplateStorage("storage.mysql.extralarge",
            GroupType.MAPR_MYSQL_SERVER_GROUP.ordinal(), InstanceType.EXTRA_LARGE.ordinal(), 120);
      logger.debug("extra large storage of mapr mysql group is " + value + "GB.");

      value = setTemplateStorage("storage.mysql.large",
            GroupType.MAPR_MYSQL_SERVER_GROUP.ordinal(), InstanceType.LARGE.ordinal(), 80);
      logger.debug("large storage of mapr mysql group is " + value + "GB.");

      value = setTemplateStorage("storage.mysql.medium",
            GroupType.MAPR_MYSQL_SERVER_GROUP.ordinal(), InstanceType.MEDIUM.ordinal(), 40);
      logger.debug("medium storage of mapr mysql group is " + value + "GB.");

      value = setTemplateStorage("storage.mysql.small",
            GroupType.MAPR_MYSQL_SERVER_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 20);
      logger.debug("small storage of mapr mysql group is " + value + "GB.");
   }

   private static int setTemplateStorage(String propertyName, int groupType, int instanceType, int defaultVal) {
      int value = 0;
      try {
         value = Configuration.getInt(propertyName, defaultVal);
      } catch (ConversionException e) {
         value = defaultVal;
      }
      templateStorage[groupType][instanceType] = value;
      return value;
   }

   public static int getStorage(InstanceType instance, GroupType groupType) {
      return templateStorage[groupType.ordinal()][instance.ordinal()];
   }
}
