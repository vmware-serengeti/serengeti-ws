package com.vmware.bdd.exception;

public class SetPasswordException extends BddException{
   private static final long serialVersionUID = 1l;

   public SetPasswordException() {
   }

   public SetPasswordException(Throwable cause, String errorId,
         Object... detail) {
      super(cause, "SET_PASSWORD", errorId, detail);
   }

   public static SetPasswordException SETUP_PASSWORDLESS_LOGIN_TIMEOUT(Throwable cause, String nodeIP) {
      return new SetPasswordException(cause, "SETUP_PASSWORDLESS_LOGIN_TIMEOUT", nodeIP);
   }

   public static SetPasswordException FAIL_TO_SETUP_PASSWORDLESS_LOGIN(String nodeIP) {
      return new SetPasswordException(null, "SETUP_PASSWORDLESS_LOGIN_FAILED", nodeIP);
   }

   public static SetPasswordException FAIL_TO_SET_PASSWORD(String nodeIP, String msg) {
      return new SetPasswordException(null, "FAILED_TO_SET_PASSWORD", nodeIP, msg);
   }

   public static SetPasswordException PASSWORD_CONTAIN_INVALID_CHARACTER() {
      return new SetPasswordException(null, "PASSWORD_CONTAIN_INVALID_CHARACTER");
   }

   public static SetPasswordException FAIL_TO_REMOVE_SSH_LIMIT(String nodeIP) {
      return new SetPasswordException(null, "FAIL_TO_REMOVE_SSH_LIMIT", nodeIP);
   }

   public static SetPasswordException GOT_JSCH_EXCEPTION_WHEN_SET_PASSWORD(
         Throwable cause, String nodeIP) {
      return new SetPasswordException(cause, "GOT_JSCH_EXCEPTION_WHEN_SET_PASSWORD", nodeIP);
   }

   public static SetPasswordException FAIL_TO_GET_NODE_IP(String vmName) {
      return new SetPasswordException(null, "FAIL_TO_GET_NODE_IP", vmName);
   }
}
