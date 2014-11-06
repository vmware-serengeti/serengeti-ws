package com.vmware.bdd.manager;

import com.vmware.bdd.exception.BddException;

/**
 * Created by qjin on 11/15/14.
 */
public class ShrinkException extends BddException {
   private static final long serialVersionUID = 1l;

   public ShrinkException() {
   }

   public ShrinkException(Throwable cause, String errorId, Object... detail) {
      super(cause,"CLUSTER_SHRINK", errorId, detail);
   }

   public static ShrinkException DECOMISSION_FAILED(Throwable cause, String errMsg) {
      return new ShrinkException(cause, "DECOMISSION_FAILED", errMsg);
   }

   public static ShrinkException NO_NEED_TO_SHRINK() {
      return new ShrinkException(null, "NO_NEED_TO_SHRINK");
   }

   public static ShrinkException SHRINK_NODE_GROUP_FAILED(Throwable t, String clusterName, String errMsg) {
      return new ShrinkException(t, "SHRINK_NODE_GROUP_FAILED", clusterName, errMsg);
   }

   public static ShrinkException DELETE_VM_FAILED(Throwable cause, String clusterName,
                                                  String nodeName) {
      return new ShrinkException(cause, "DELETE_VM_FAILED", clusterName, nodeName, cause.getMessage());
   }
}
