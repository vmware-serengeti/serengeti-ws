package com.vmware.bdd.exception;

import java.util.List;

/**
 * Created by jiahuili on 7/14/15.
 */
public class WarningMessageException extends BddException {

   private static final long serialVersionUID = 5011637701410867484L;
   private boolean isWarningMsg = true;

   public WarningMessageException(String msg) {
      super(msg);
   }

   public WarningMessageException(Throwable cause, String section, String errorId,
         Object... detail) {
      super(cause, section, errorId, detail);
   }

   public boolean isWarningMsg() {
      return isWarningMsg;
   }

   public void setWarningMsg(boolean isWarningMsg) {
      this.isWarningMsg = isWarningMsg;
   }

   public static WarningMessageException NEW_DATASTORES_EXCLUDE_OLD_DATASTORES(Throwable ex, String oldDS,
         String newDS) {
      return new WarningMessageException(ex, "CLUSTER_UPDATE", "NEW_DS_EXCLUDE_OLD_DS", oldDS, newDS);
   }

   public static WarningMessageException SET_EMPTY_DATASTORES_TO_NON_EMTPY(Throwable ex, String newDS) {
      return new WarningMessageException(ex, "CLUSTER_UPDATE", "SET_EMPTY_DS_TO_NON_EMTPY", newDS);
   }

}
