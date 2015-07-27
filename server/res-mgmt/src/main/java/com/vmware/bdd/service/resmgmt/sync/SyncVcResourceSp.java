package com.vmware.bdd.service.resmgmt.sync;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcObject;
import com.vmware.aurora.vc.VcObjectImpl;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import org.apache.log4j.Logger;

/**
 * Created by xiaoliangl on 7/16/15.
 */
public class SyncVcResourceSp extends AbstractSyncVcResSP {
   private static final Logger LOGGER = Logger.getLogger(SyncResourceSp.class);

   private final ManagedObjectReference moRef;

   public SyncVcResourceSp(ManagedObjectReference moRef1) {
      moRef = moRef1;
   }

   @Override
   protected VcObject syncThis() {
      return VcContext.inVcSessionDo(new VcSession<VcObject>() {
         public VcObject body() throws Exception {
            VcObject vcObject = null;
            try {
               vcObject = VcObjectImpl.loadFromMoRef(moRef);

               VcCache.put(moRef, vcObject);

               if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug(String.format("retrieve vc resource[%1s] is updated.", moRef));
               }
            } catch (Exception ex) {
               LOGGER.error(String.format("retrieve vc resource[%1s] failed.", moRef), ex);
            }

            return vcObject;
         }
      });
   }
}
