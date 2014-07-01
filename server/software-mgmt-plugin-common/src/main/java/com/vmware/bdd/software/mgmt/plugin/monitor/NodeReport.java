package com.vmware.bdd.software.mgmt.plugin.monitor;

import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;

/**
 * Author: Xiaoding Bian
 * Date: 6/10/14
 * Time: 7:33 PM
 */
public class NodeReport implements Cloneable{

   private String name;

   private String ipAddress;

   private String action;

   private NodeStatus status;

   private boolean finished;

   private boolean success;

   private int progress;

   private int errCode;

   private String errMsg;

   public NodeReport() {}

   public NodeReport(NodeInfo nodeInfo) {
      this.name = nodeInfo.getName();
      this.ipAddress = nodeInfo.getMgtIpAddress();
      this.action = null;
      this.status = null;
      this.success = false;
      this.finished = false;
      this.errCode = 0;
      this.errMsg = null;
      this.progress = 0;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public String getAction() {
      return action;
   }

   public void setAction(String action) {
      this.action = action;
   }

   public NodeStatus getStatus() {
      return status;
   }

   public void setStatus(NodeStatus status) {
      this.status = status;
   }

   public boolean isFinished() {
      return finished;
   }

   public void setFinished(boolean finished) {
      this.finished = finished;
   }

   public boolean isSuccess() {
      return success;
   }

   public void setSuccess(boolean success) {
      this.success = success;
   }

   public int getProgress() {
      return progress;
   }

   public void setProgress(int progress) {
      this.progress = progress;
   }

   public int getErrCode() {
      return errCode;
   }

   public void setErrCode(int errCode) {
      this.errCode = errCode;
   }

   public String getErrMsg() {
      return errMsg;
   }

   public void setErrMsg(String errMsg) {
      this.errMsg = errMsg;
   }

   /**
    * deep clone
    * @return
    */
   @Override
   public NodeReport clone() {
      try {
         return (NodeReport) super.clone();
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }

}
