package com.vmware.bdd.exception;

public class SetLocalRepoException extends BddException {
   private static final long serialVersionUID = 1l;

   public SetLocalRepoException() {
   }

   public SetLocalRepoException(Throwable cause, String errorId,
         Object... detail) {
      super(cause, "SET_LOCAL_REPO", errorId, detail);
   }

   public static SetLocalRepoException FAIL_TO_SET_LOCAL_REPO(String nodeIP,
         String msg) {
      return new SetLocalRepoException(null, "FAILED_TO_SET_LOCAL_REPO",
            nodeIP, msg);
   }
}
