package com.vmware.bdd.service.resmgmt;

import com.vmware.bdd.apitypes.PluginAdd;
import com.vmware.bdd.apitypes.SoftwareMgtProvider;
import com.vmware.bdd.entity.PluginEntity;
import sun.plugin.navig.motif.Plugin;

import java.util.List;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 5:44 PM
 */
public interface IPluginService {

   void addPlugin(PluginAdd pluginAdd);

   public PluginEntity findPluginByName(String name);

   public List<PluginEntity> findPluginsByProvider(SoftwareMgtProvider provider);
}
