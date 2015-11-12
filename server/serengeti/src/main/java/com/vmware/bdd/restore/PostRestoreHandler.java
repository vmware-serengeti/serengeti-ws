package com.vmware.bdd.restore;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.impl.SoftwareManagementService;
import com.vmware.bdd.service.resmgmt.INodeTemplateService;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.utils.Constants;

public class PostRestoreHandler {
   private static final Logger logger =
         Logger.getLogger(PostRestoreHandler.class);
   @Autowired
   private IClusterEntityManager clusterEntityMgr;
   @Autowired
   private SoftwareManagerCollector softwareManagerCollector;
   @Autowired
   private SoftwareManagementService softwareManagementService;
   @Autowired
   private INodeTemplateService nodeTemplateService;

   public void handlePostRestore() {
      updateClusterTemplate();
      reconfigIronfanClusters();
   }

   public void updateClusterTemplate() {
      HashMap<String, String> templateMoidMap = nodeTemplateService.getTemplateMoidMap();
      logger.info("The templateMoidMap for restore is: " + templateMoidMap);
      if ( null != templateMoidMap && !templateMoidMap.isEmpty() ) {
         logger.info("update all clusters with new template moid.");
         List<ClusterEntity> allClusters = clusterEntityMgr.findAllClusters();
         for ( ClusterEntity clusterEntity : allClusters ) {
            String oldTemplateMoid = clusterEntity.getTemplateId();
            String newTemplateMoid = templateMoidMap.get(oldTemplateMoid);
            if ( null != newTemplateMoid ) {
               clusterEntity.setTemplateId(newTemplateMoid);
               clusterEntityMgr.update(clusterEntity);
            }
         }
      }
   }

   public void reconfigIronfanClusters() {
      // first get all clusters
      List<String> allClusters = clusterEntityMgr.findAllClusterNames();
      // second configure each cluster to update chef server info if it is created by ironfan
      for ( String clusterName : allClusters ) {
         SoftwareManager softwareManager = softwareManagerCollector.getSoftwareManagerByClusterName(clusterName);
         if (Constants.IRONFAN.equalsIgnoreCase(softwareManager.getType())) {
            logger.info("reconfiguring cluster " + clusterName + " during post restore.");
            this.softwareManagementService.configCluster(clusterName);
         }
      }
   }
}
