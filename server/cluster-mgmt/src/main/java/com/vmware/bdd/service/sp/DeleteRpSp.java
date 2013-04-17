package com.vmware.bdd.service.sp;

import java.util.List;
import java.util.concurrent.Callable;

import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;

public class DeleteRpSp implements Callable<Void> {
   final private VcResourcePool vcRp;
   final private String deleteRpName;

   public DeleteRpSp(VcResourcePool vcRp, String deleteRpName) {
      this.vcRp = vcRp;
      this.deleteRpName = deleteRpName;
   }

   public VcResourcePool getVcRp() {
      return vcRp;
   }

   public String getDeleteRpName() {
      return deleteRpName;
   }

   @Override
   public Void call() throws Exception {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            List<VcResourcePool> rps = vcRp.getChildren();
            VcResourcePool delete = null;
            for (VcResourcePool rp : rps) {
               if (rp.getName().equals(deleteRpName)) {
                  delete = rp;
               }
            }
            if (delete != null) {
               delete.destroy();
            }
            return null;
         }
         protected boolean isTaskSession() {
            return true;
         }
      });
      return null;
   }
}
