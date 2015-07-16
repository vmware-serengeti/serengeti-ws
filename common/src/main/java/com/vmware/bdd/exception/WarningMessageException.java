package com.vmware.bdd.exception;

import com.vmware.bdd.security.tls.CertificateInfo;

import java.util.List;

/**
 * Created by jiahuili on 7/14/15.
 */
public class WarningMessageException extends BddException {


   private List<String> warningMsgList;

   public WarningMessageException(List<String> warningMsg) {
      super(null, "CLUSTER.UPDATE", "DS_UPDATE_WARNING");
      warningMsgList = warningMsg;
   }

   public WarningMessageException() {
   }

   public List<String> getWarningMsgList() {
      return warningMsgList;
   }

   public void setWarningMsgList(List<String> warningMsg) {
      this.warningMsgList = warningMsg;
   }

}
