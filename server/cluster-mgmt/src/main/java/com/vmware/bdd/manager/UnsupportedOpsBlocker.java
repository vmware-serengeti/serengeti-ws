package com.vmware.bdd.manager;

import com.vmware.aurora.util.CommandExec;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ElasticityRequestBody;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.dal.IAppManagerDAO;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.resmgmt.IAppManagerService;
import com.vmware.bdd.software.mgmt.thrift.SoftwareManagement;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * For ClouderaManager, Ambari, some ops are not supported in M9.
 * https://wiki.eng.vmware.com/BigData/Releases/M9/CM#Operations_support_matrix_for_new_app_manager:_Cloudera_Manager_and_Ambari
 */
@Component
public class UnsupportedOpsBlocker {

   @Autowired
   private IClusterEntityManager clusterEntityManager;

   @Autowired
   private IAppManagerDAO appManager;


   public void blockUnsupportedOpsByCluster(String ops, String clusterName) {
      ClusterEntity clusterEntity = clusterEntityManager.findByName(clusterName);

      if(CommonUtil.isBlank(clusterEntity.getAppManager())) {
         throw BddException.CLUSTER_HAS_NO_APP_MGR(clusterName);
      }

      AppManagerEntity appMgrEntity = appManager.findByName(clusterEntity.getAppManager());

      if(appMgrEntity == null) {
         throw BddException.APP_MGR_NOT_FOUND(clusterName);
      }

      if(CommonUtil.isBlank(appMgrEntity.getType())) {
         throw BddException.APP_MGR_TYPE_IS_BLANK(appMgrEntity.getName());
      }

      blockUnsupportedOpsByAppMgr(ops, appMgrEntity.getType());
   }

   public void blockUnsupportedOpsByAppMgr(String ops, String appMgr) {
      if(!Constants.IRONFAN.equals(appMgr)) {
         throw BddException.UNSUPPORTED_OPS(ops, appMgr);
      }
   }
}
