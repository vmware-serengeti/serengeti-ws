package com.vmware.bdd.service;

import com.vmware.bdd.apitypes.CmClusterDef;
import com.vmware.bdd.exception.CmException;

import java.io.File;

/**
 * Author: Xiaoding Bian
 * Date: 5/21/14
 * Time: 1:02 PM
 */

public interface ICmProviderService {

   public String getVersion();

   public int getVersionApi();

   public int getVersionCdh();

   public boolean provision(CmClusterDef cluster) throws CmException;

   public boolean unprovision(CmClusterDef cluster) throws CmException;

   public boolean isProvisioned(CmClusterDef cluster) throws CmException;

   public boolean start(CmClusterDef cluster) throws CmException;

   public boolean isStarted(CmClusterDef cluster) throws CmException;

   public boolean stop(CmClusterDef cluster) throws CmException;

   public boolean isStopped(CmClusterDef cluster) throws CmException;

   //public boolean configure(CmClusterSpec cluster) throws CmException;
   public boolean configure(CmClusterDef cluster) throws CmException;

   public boolean isConfigured(CmClusterDef cluster) throws CmException;

   public boolean unconfigure(CmClusterDef cluster) throws CmException;

   public boolean initialize(CmClusterDef cluster) throws CmException;

   public boolean getServiceConfigs(CmClusterDef cluster, File path) throws CmException;

}