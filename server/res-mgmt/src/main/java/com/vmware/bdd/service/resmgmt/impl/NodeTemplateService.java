/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package com.vmware.bdd.service.resmgmt.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.apitypes.VirtualMachineRead;
import com.vmware.bdd.dal.INodeTemplateDAO;
import com.vmware.bdd.entity.NodeTemplateEntity;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.service.resmgmt.INodeTemplateService;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;

@Component
public class NodeTemplateService implements INodeTemplateService {

   private static long REFRESH_NODE_TEMPLATES_INTERVAL = 30; // in seconds
   private static final Logger logger = Logger.getLogger(NodeTemplateService.class);

   private long lastRefreshTimestamp = 0L;

   @Autowired
   private INodeTemplateDAO nodeTemplateDAO;

   private HashMap<String, String> templateMoidMap = new HashMap<String, String>();

   public HashMap<String, String> getTemplateMoidMap() {
      return templateMoidMap;
   }

   public void setTemplateMoidMap(HashMap<String, String> templateMoidMap) {
      this.templateMoidMap = templateMoidMap;
   }

   @Override
   public VcVirtualMachine getNodeTemplateVMByName(String templateName) {
      String moid = getNodeTemplateIdByName(templateName);
      return getNodeTemplateVMByMoid(moid);
   }

   @Override
   public VcVirtualMachine getNodeTemplateVMByMoid(String moid) {
      return VcResourceUtils.findVM(moid);
   }

   @Override
   public String getNodeTemplateIdByName(String templateName) {
      logger.info("finding node template moid by name " + templateName);
      refreshNodeTemplates();
      if (!CommonUtil.isBlank(templateName)) {
         // use the specified node template
         List<NodeTemplateEntity> entities = nodeTemplateDAO.findByName(templateName);
         if (entities.size() == 1) {
            return entities.get(0).getMoid();
         } else if (entities.size() == 0) {
            throw ClusteringServiceException.TEMPLATE_VM_NOT_FOUND(templateName);
         } else {
            throw ClusteringServiceException.DUPLICATE_NODE_TEMPLATE(templateName);
         }
      } else {
         // use the default node template
         List<NodeTemplateEntity> entities = nodeTemplateDAO.findAll();
         if (entities.size() == 1) {
            return entities.get(0).getMoid();
         } else if  (entities.size() == 0) {
            throw ClusteringServiceException.NO_AVAILABLE_NODE_TEMPLATE();
         } else {
            throw ClusteringServiceException.MORE_THAN_ONE_NODE_TEMPLATE();
         }
      }
   }

   @Override
   public String getNodeTemplateNameByMoid(String moid) {
      return getNodeTemplateVMByMoid(moid).getName();
   }

   @Override
   public VirtualMachineRead getNodeTemplateByMoid(String moid) {
      return toVirtualMachineRead(nodeTemplateDAO.findByMoid(moid));
   }

   @Override
   public VirtualMachineRead getNodeTemplateByName(String name) {
      String moid = getNodeTemplateIdByName(name);
      return getNodeTemplateByMoid(moid);
   }

   @Override
   @Transactional
   public List<VirtualMachineRead> getAllNodeTemplates() {
      refreshNodeTemplates();

      List<VirtualMachineRead> templates = new ArrayList<VirtualMachineRead>();
      List<NodeTemplateEntity> entities = nodeTemplateDAO.findAllOrderByName();
      for (NodeTemplateEntity entity : entities) {
         templates.add(toVirtualMachineRead(entity));
      }
      return templates;
   }

   public VirtualMachineRead toVirtualMachineRead(NodeTemplateEntity entity) {
      VirtualMachineRead vmr = new VirtualMachineRead();
      vmr.setName(entity.getName());
      vmr.setMoid(entity.getMoid());
      vmr.setTag(entity.getTag());
      vmr.setLastModified(entity.getLastModified());
      return vmr;
   }

   /*
    * Detect the latest node templates from vCenter and save to db.
    */
   @Override
   @Transactional
   public synchronized void refreshNodeTemplates() {
      long curTime = System.currentTimeMillis() / 1000;
      if (curTime - this.lastRefreshTimestamp < getRefreshNodeTemplateInterval()) {
         return;
      }
      logger.info("Refreshing node templates from vCenter");
      List<VcVirtualMachine> vms = VcResourceUtils.findAllNodeTemplates();
      HashSet<String> moids = new HashSet<String>();
      HashMap<String, String> nameToMoid = new HashMap<String, String>();
      for (VcVirtualMachine vm : vms) {
         String moid = vm.getId();
         moids.add(moid);
         nameToMoid.put(vm.getName(), moid);
         long timestamp = System.currentTimeMillis();
         NodeTemplateEntity entity = nodeTemplateDAO.findByMoid(moid);
         if (entity == null) {
            entity = new NodeTemplateEntity();
            convertVirtualMachineToEntity(vm, entity, timestamp);
            nodeTemplateDAO.insert(entity);
         } else {
            convertVirtualMachineToEntity(vm, entity, timestamp);
            nodeTemplateDAO.update(entity);
         }
      }
      // remove the non-exists templates from DB
      for (NodeTemplateEntity entity : nodeTemplateDAO.findAll()) {
         if (!moids.contains(entity.getMoid())) {
            if ( ConfigInfo.isJustRestored() ) {
               // for restore and upgrade, we need to get the mapping between
               // old moid to new moid for all the node templates
               logger.info("The Serengeti Server was just restored, so create the "
                     + "moid mapping between old and new templates, then update the "
                     + "template id in cluster table.");
               String oldMoid = entity.getMoid();
               String newMoid = nameToMoid.get(entity.getName());
               if ( null != newMoid ) {
                  templateMoidMap.put(oldMoid, newMoid);
               }
            }
            nodeTemplateDAO.delete(entity);
         }
      }
      logger.info("Refreshing node templates completed");
      this.lastRefreshTimestamp = System.currentTimeMillis() / 1000;
   }

   public long getRefreshNodeTemplateInterval() {
      return Configuration.getLong("vc.refresh.node_templates.interval", REFRESH_NODE_TEMPLATES_INTERVAL);
   }

   private void convertVirtualMachineToEntity(VcVirtualMachine vm, NodeTemplateEntity entity, long timestamp) {
      entity.setMoid(vm.getId());
      entity.setName(vm.getName());
      Date time = getLastModifiedTime(vm);
      entity.setLastModified(time);
      if (entity.getLastModified() != null && time.compareTo(entity.getLastModified()) != 0) {
         logger.info(String.format("Last modified time or powered on time of VM %s is changed on %s", vm.getName(), time.toString()));
      }
   }

   /*
    * The last time a VM's configuration is modified or the VM is powered on and changes made to the OS inside.
    */
   private Date getLastModifiedTime(VcVirtualMachine vm) {
      // The changeVersion is a unique identifier for a given version of the configuration.
      // This is typically implemented as an ever increasing count or a time-stamp.
      // Each change to the VM configuration in VM Settings or powering on the VM will update this value.
      // See https://www.vmware.com/support/developer/converter-sdk/conv50_apireference/vim.vm.ConfigInfo.html
      // Its value is something like "2015-08-26T08:07:31.366195Z".
      Calendar cal = javax.xml.bind.DatatypeConverter.parseDateTime(vm.getConfig().getChangeVersion());
      Date time = cal.getTime();
      logger.debug(String.format("Last modified time or powered on time of VM %s is %s", vm.getName(), time.toString()));
      return time;
   }

}
