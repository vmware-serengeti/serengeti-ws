package com.vmware.bdd.service.resmgmt.sync;

import com.vmware.aurora.vc.VcObject;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.utils.AuAssert;
import org.apache.log4j.Logger;

/**
 * Created by xiaoliangl on 7/16/15.
 */
public class SyncResourceSp extends AbstractSyncVcResSP {
   private static final Logger LOGGER = Logger.getLogger(SyncResourceSp.class);

   private final VcObject vcObject;

   public SyncResourceSp(VcObject vcObject1) {
      AuAssert.check(vcObject1 != null, "can't sync an null Vc Object");
      vcObject = vcObject1;
   }

   @Override
   protected VcObject syncThis() {
      //sync the current vc resource object
      return VcContext.inVcSessionDo(new VcSession<VcObject>() {
         @Override
         protected VcObject body() {
            try {
               vcObject.update();
               if(LOGGER.isDebugEnabled()) {
                  LOGGER.debug(String.format("vc resource[%1s] is updated.", vcObject.getId()));
               }
            } catch (Exception e) {
               LOGGER.error(String.format("update vc resource[%1s] failed.", vcObject.getMoRef()), e);
            }

            return vcObject;
         }
      });
   }

}
