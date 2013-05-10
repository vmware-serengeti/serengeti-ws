package com.vmware.bdd.exception;

public class ScaleServiceException extends BddException {

   private static final long serialVersionUID = 1l;

   public ScaleServiceException() {
   }

   public ScaleServiceException(Throwable cause, String errorId,
         Object... detail) {
      super(cause, "SCALE_SERVICE", errorId, detail);
   }

   public static ScaleServiceException CURRENT_DATASTORE_UNACCESSIBLE(
         String dsName) {
      return new ScaleServiceException(null, "CURRENT_DATASTORE_UNACCESSIBLE",
            dsName);
   }

   public static ScaleServiceException CANNOT_FIND_VALID_DATASTORE_FOR_SWAPDISK(
         String nodeName) {
      return new ScaleServiceException(null,
            "CANNOT_FIND_VALID_DATASTORE_FOR_SWAPDISK", nodeName);
   }

   public static ScaleServiceException NOT_NEEDED(String clusterName) {
      return new ScaleServiceException(null, "NOT_NEEDED", clusterName);
   }

   public static ScaleServiceException JOB_LAUNCH_FAILURE(String clusterName,
         Throwable t, String errorMsg) {
      return new ScaleServiceException(t, "JOB_LAUNCH_FAILURE", clusterName,
            errorMsg);
   }


}
