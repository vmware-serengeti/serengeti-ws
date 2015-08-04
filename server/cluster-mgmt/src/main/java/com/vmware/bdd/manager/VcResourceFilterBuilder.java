package com.vmware.bdd.manager;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.dal.IDatastoreDAO;
import com.vmware.bdd.dal.INetworkDAO;
import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.VcDatastoreEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.service.resmgmt.VC_RESOURCE_TYPE;
import com.vmware.bdd.service.resmgmt.sync.filter.VcResourceFilters;
import com.vmware.bdd.utils.CommonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiaoliangl on 8/3/15.
 */
@Service
public class VcResourceFilterBuilder {
   @Autowired
   private IDatastoreDAO dsDao;

   @Autowired
   private IResourcePoolDAO rpDao;

   @Autowired
   private INetworkDAO networkDao;

   public VcResourceFilters build(List<String> dsNames, List<String> rpNames, List<String> networkNames) {
      VcResourceFilters filters = new VcResourceFilters();

      String[] vcDsNameRegs = new String[dsNames.size()];
      int i = 0;
      for (String dsSpecName : dsNames) {
         List<VcDatastoreEntity> dsEntities = dsDao.findByName(dsSpecName);
         if (CollectionUtils.isNotEmpty(dsEntities)) {
            VcDatastoreEntity dsEntity = dsEntities.get(0);
            if (dsEntity.getRegex()) {
               vcDsNameRegs[i] = dsEntity.getVcDatastore();
            } else {
               vcDsNameRegs[i] = CommonUtil.getDatastoreJavaPattern(dsEntity.getVcDatastore());
            }
         }
         i++;
      }

      filters.addNameFilter(VC_RESOURCE_TYPE.DATA_STORE, vcDsNameRegs, true);
      filters.addHostFilterByDatastore(vcDsNameRegs);


      String[] vcClusterNames = new String[rpNames.size()];
      i = 0;
      for(String rpSpecName : rpNames) {
         VcResourcePoolEntity rpEntity = rpDao.findByName(rpSpecName);
         vcClusterNames[i] = rpEntity.getVcCluster();
         i++;
      }

      filters.addNameFilter(VC_RESOURCE_TYPE.CLUSTER, vcClusterNames, false);


      String[] vcNetworkNames = new String[networkNames.size()];
      i = 0;
      for(String networkName : networkNames) {
         NetworkEntity networkEntity = networkDao.findNetworkByName(networkName);
         vcNetworkNames[i] = networkEntity.getPortGroup();
         i++;
      }

      filters.addHostFilterByNetwork(vcNetworkNames);

      return filters;
   }
}
