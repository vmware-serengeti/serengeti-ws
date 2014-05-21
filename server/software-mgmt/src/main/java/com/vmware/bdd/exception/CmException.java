package com.vmware.bdd.exception;

import java.util.List;

/**
 * Author: Xiaoding Bian
 * Date: 5/21/14
 * Time: 1:13 PM
 */
public class CmException extends BddException {

   //private static final long serialVersionUID = 1L;

   public CmException() {
   }

   public CmException(String msg) {}

   public CmException(Throwable cause, String errorId, Object... detail) {
      super(cause, "ClouderaManager", errorId, detail);
   }

   public static CmException INVALID_VERSION(String version) {
      return new CmException(null, "INVALID_VERSION", version);
   }

   public static CmException UNSURE_CLUSTER_EXIST(String clusterName) {
      return new CmException(null, "UNSURE_CLUSTER_EXIST", clusterName);
   }

   public static CmException PROVISION_FAILED(String clusterName) {
      return new CmException(null, "PROVISION_FAILED", clusterName);
   }
}
