package com.vmware.bdd.service.resmgmt.impl;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.PluginAdd;
import com.vmware.bdd.apitypes.SoftwareMgtProvider;
import com.vmware.bdd.dal.IPluginDAO;
import com.vmware.bdd.entity.PluginEntity;
import com.vmware.bdd.service.resmgmt.IPluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.log4j.Logger;

import java.util.List;


/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 5:46 PM
 */
@Service
public class PluginService implements IPluginService{

   private static Logger logger = Logger.getLogger(PluginService.class);

   private IPluginDAO pluginDAO;

   @Override
   public synchronized void addPlugin(PluginAdd pluginAdd) {
      logger.info((new Gson()).toJson(pluginAdd));
      // TODO: validation
      PluginEntity pluginEntity = new PluginEntity(pluginAdd);
      pluginDAO.insert(pluginEntity);
   }

   @Override
   public PluginEntity findPluginByName(String name) {
      return pluginDAO.findByName(name);
   }

   @Override
   public List<PluginEntity> findPluginsByProvider(SoftwareMgtProvider provider) {
      return pluginDAO.findByProvider(provider);
   }

   public IPluginDAO getPluginDAO() {
      return pluginDAO;
   }

   @Autowired
   public void setPluginDAO(IPluginDAO pluginDAO) {
      this.pluginDAO = pluginDAO;
   }
}
