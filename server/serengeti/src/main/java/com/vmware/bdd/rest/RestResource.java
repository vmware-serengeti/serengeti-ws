/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.rest;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.service.resmgmt.IAppManagerService;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.DatastoreAdd;
import com.vmware.bdd.apitypes.DatastoreRead;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.apitypes.ElasticityRequestBody;
import com.vmware.bdd.apitypes.FixDiskRequestBody;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.apitypes.RackInfo;
import com.vmware.bdd.apitypes.RackInfoList;
import com.vmware.bdd.apitypes.ResourcePoolAdd;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.apitypes.ResourceScale;
import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.apitypes.TaskRead.Type;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.DistroManager;
import com.vmware.bdd.manager.JobManager;
import com.vmware.bdd.manager.RackInfoManager;
import com.vmware.bdd.manager.ScaleManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.service.impl.ClusteringService;
import com.vmware.bdd.service.resmgmt.IDatastoreService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.IpAddressUtil;

@Controller
public class RestResource {
   private static final Logger logger = Logger.getLogger(RestResource.class);

   @Autowired
   private ClusterManager clusterMgr;
   @Autowired
   private JobManager jobManager;
   @Autowired
   private IResourcePoolService vcRpSvc;
   @Autowired
   private INetworkService networkSvc;
   @Autowired
   private RackInfoManager rackInfoManager;
   @Autowired
   private DistroManager distroManager;
   @Autowired
   private IDatastoreService datastoreSvc;
   @Autowired
   private IAppManagerService appManagerService;
   @Autowired
   private ScaleManager scaleMgr;
   @Autowired
   private SoftwareManagerCollector softwareManagerCollector;

   private static final String ERR_CODE_FILE = "serengeti-errcode.properties";
   private static final int DEFAULT_HTTP_ERROR_CODE = 500;

   /* HTTP status code read from a config file. */
   private static org.apache.commons.configuration.Configuration httpStatusCodes =
         init();

   private static org.apache.commons.configuration.Configuration init() {
      PropertiesConfiguration config = null;
      try {
         config = new PropertiesConfiguration();
         config.setListDelimiter('\0');
         config.load(ERR_CODE_FILE);
      } catch (ConfigurationException ex) {
         // error out if the configuration file is not there
         String message = "Cannot load Serengeti error message file.";
         Logger.getLogger(RestResource.class).fatal(message, ex);
         throw BddException.APP_INIT_ERROR(ex, message);
      }
      return config;
   }

   private static int getHttpErrorCode(String errorId) {
      return httpStatusCodes.getInteger(errorId, DEFAULT_HTTP_ERROR_CODE);
   }

   /**
    * Get REST api version
    * @return REST api version
    */
   @RequestMapping(value = "/hello", method = RequestMethod.GET)
   @ResponseStatus(HttpStatus.OK)
   @ResponseBody
   public String getHello() {
      return Constants.VERSION;
   }

   // task API
   /**
    * Get latest tasks of exiting clusters
    * @return A list of task information
    */
   @RequestMapping(value = "/tasks", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<TaskRead> getTasks() {
      return jobManager.getLatestTaskForExistedClusters();
   }

   /**
    * Get a specific task by its id
    * @param taskId The identity returned as part of uri in the response(Accepted status) header of Location, such as https://hostname:8443/serengeti/api/task/taskId
    * @return Task information
    */
   @RequestMapping(value = "/task/{taskId}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public TaskRead getTaskById(@PathVariable long taskId) throws Exception {

      // TODO add exception handling
      TaskRead task = jobManager.getJobExecutionStatus(taskId);
      if (task.getStatus() == TaskRead.Status.COMPLETED) {
         task.setProgress(1.0);
      }
      if (task.getType() == null) {
         task.setType(Type.INNER); // XXX just keep the interface now
      }
      return task;
   }

   // cluster API
   /**
    * Create a cluster
    * @param createSpec create specification
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    */
   @RequestMapping(value = "/clusters", method = RequestMethod.POST, consumes = "application/json")
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void createCluster(@RequestBody ClusterCreate createSpec,
         HttpServletRequest request, HttpServletResponse response)
         throws Exception {
      verifyInitialized();
      String clusterName = createSpec.getName();
      if (!CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }
      if (CommonUtil.isBlank(createSpec.getAppManager())) {
         createSpec.setAppManager(Constants.IRONFAN);
      } else {
         AppManagerEntity appManager =
               appManagerService.findAppManagerByName(createSpec
                     .getAppManager());
         if (appManager == null) {
            throw BddException.NOT_FOUND("application manager",
                  createSpec.getAppManager());
         }
      }
      long jobExecutionId = clusterMgr.createCluster(createSpec);
      redirectRequest(jobExecutionId, request, response);
   }

   /**
    * Configure a hadoop or hbase cluster's properties
    * @param clusterName
    * @param createSpec The existing create specification plus the configuration map supported in cluster and node group levels(please refer to a sample specification file)
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    */
   @RequestMapping(value = "/cluster/{clusterName}/config", method = RequestMethod.PUT, consumes = "application/json")
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void configCluster(@PathVariable("clusterName") String clusterName,
         @RequestBody ClusterCreate createSpec, HttpServletRequest request,
         HttpServletResponse response) throws Exception {
      verifyInitialized();
      if (!CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }
      Long taskId = clusterMgr.configCluster(clusterName, createSpec);
      redirectRequest(taskId, request, response);
   }

   private void redirectRequest(long taskId, HttpServletRequest request,
         HttpServletResponse response) {
      StringBuffer url = request.getRequestURL();
      String pathInfo = request.getPathInfo();
      if (!CommonUtil.validataPathInfo(pathInfo)) {
         throw BddException.INVALID_PARAMETER("requested path info", pathInfo);
      }
      int subLength = url.length() - pathInfo.length();
      url.setLength(subLength);
      url.append("/task/").append(Long.toString(taskId));
      response.setHeader("Location", url.toString());
   }

   /**
    * Delete a cluster
    * @param clusterName
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    */
   @RequestMapping(value = "/cluster/{clusterName}", method = RequestMethod.DELETE)
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void deleteCluster(@PathVariable("clusterName") String clusterName,
         HttpServletRequest request, HttpServletResponse response)
         throws Exception {
      verifyInitialized();
      clusterName = CommonUtil.decode(clusterName);
      // make sure cluster name is valid
      if (!CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }
      Long taskId = clusterMgr.deleteClusterByName(clusterName);
      redirectRequest(taskId, request, response);
   }

   /**
    * Start or stop a normal cluster, or resume a failed cluster after adjusting the resources allocated to this cluster
    * @param clusterName
    * @param state Can be start, stop, or resume
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    */
   @RequestMapping(value = "/cluster/{clusterName}", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void startStopResumeCluster(
         @PathVariable("clusterName") String clusterName,
         @RequestParam(value = "state", required = true) String state,
         HttpServletRequest request, HttpServletResponse response)
         throws Exception {

      verifyInitialized();
      clusterName = CommonUtil.decode(clusterName);
      if (CommonUtil.isBlank(clusterName)
            || !CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }

      Long taskId;
      if (state.equals("stop")) {
         taskId = clusterMgr.stopCluster(clusterName);
         redirectRequest(taskId, request, response);
      } else if (state.equals("start")) {
         taskId = clusterMgr.startCluster(clusterName);
         redirectRequest(taskId, request, response);
      } else if (state.equals("resume")) {
         taskId = clusterMgr.resumeClusterCreation(clusterName);
         redirectRequest(taskId, request, response);
      } else {
         throw BddException.INVALID_PARAMETER("cluster state", state);
      }
   }

   /**
    * Expand the number of nodes in a node group
    * @param clusterName
    * @param groupName
    * @param instanceNum The target instance number after resize. It must be larger than existing instance number in this node group
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    */
   @RequestMapping(value = "/cluster/{clusterName}/nodegroup/{groupName}/instancenum", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void resizeCluster(@PathVariable("clusterName") String clusterName,
         @PathVariable("groupName") String groupName,
         @RequestBody Integer instanceNum, HttpServletRequest request,
         HttpServletResponse response) throws Exception {

      verifyInitialized();

      if (CommonUtil.isBlank(clusterName)
            || !CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }

      if (CommonUtil.isBlank(groupName)
            || !CommonUtil.validateNodeGroupName(groupName)) {
         throw BddException.INVALID_PARAMETER("node group name", groupName);
      }

      if (instanceNum <= 0) {
         throw BddException.INVALID_PARAMETER("node group instance number",
               String.valueOf(instanceNum));
      }
      Long taskId =
            clusterMgr.resizeCluster(clusterName, groupName, instanceNum);
      redirectRequest(taskId, request, response);
   }

   /**
    * Upgrade a cluster
    * @param clusterName
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    * @throws Exception
    */
   @RequestMapping(value = "/cluster/{clusterName}/upgrade", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void upgradeCluster(@PathVariable("clusterName") String clusterName,
         HttpServletRequest request, HttpServletResponse response)
         throws Exception {
      // make sure cluster name is valid
      if (!CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }
      Long taskId = clusterMgr.upgradeClusterByName(clusterName);
      redirectRequest(taskId, request, response);
   }

   /**
    * Scale up or down the cpu and memory of each node in a node group
    * @param clusterName
    * @param groupName
    * @param scale The new cpu and memory allocated to each node in this node group
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    */
   @RequestMapping(value = "/cluster/{clusterName}/nodegroup/{groupName}/scale", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void scale(@PathVariable("clusterName") String clusterName,
         @PathVariable("groupName") String groupName,
         @RequestBody ResourceScale scale, HttpServletRequest request,
         HttpServletResponse response) throws Exception {

      verifyInitialized();

      if (CommonUtil.isBlank(clusterName)
            || !CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }

      if (CommonUtil.isBlank(groupName)
            || !CommonUtil.validateNodeGroupName(groupName)) {
         throw BddException.INVALID_PARAMETER("node group name", groupName);
      }

	  if (scale.getCpuNumber() <= 0
			&& scale.getMemory() < Constants.MIN_MEM_SIZE) {
		 throw BddException.INVALID_PARAMETER_WITHOUT_EQUALS_SIGN(
						"node group scale parameter. The number of CPUs must be greater than zero, and the memory size in MB must be greater than or equal to "
								+ Constants.MIN_MEM_SIZE + ":", scale);
		}
      logger.info("scale cluster: " + scale.toString());
      Long taskId =
            scaleMgr.scaleNodeGroupResource(scale);
      redirectRequest(taskId, request, response);
   }

   /**
    * Turn on or off some compute nodes
    * @param clusterName
    * @param requestBody
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    */
   @RequestMapping(value = "/cluster/{clusterName}/param_wait_for_result", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void asyncSetParam(@PathVariable("clusterName") String clusterName, @RequestBody ElasticityRequestBody requestBody, HttpServletRequest request,
         HttpServletResponse response) throws Exception {
      verifyInitialized();
      validateInput(clusterName, requestBody);
      ClusterRead cluster = clusterMgr.getClusterByName(clusterName, false);
      if (!cluster.needAsyncUpdateParam(requestBody)) {
            throw BddException.BAD_REST_CALL(null, "invalid input to cluster.");
      }

      Long taskId =
            clusterMgr.asyncSetParam(clusterName,
                  requestBody.getActiveComputeNodeNum(),
                  requestBody.getMinComputeNodeNum(),
                  requestBody.getMaxComputeNodeNum(),
                  requestBody.getEnableAuto(), requestBody.getIoPriority());
      redirectRequest(taskId, request, response);
   }

   /**
    * Change elasticity mode, IO priority, and maximum or minimum number of powered on compute nodes under auto mode
    * @param clusterName
    * @param requestBody
    * @param request
    */
   @RequestMapping(value = "/cluster/{clusterName}/param", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.OK)
   public void syncSetParam(@PathVariable("clusterName") String clusterName, @RequestBody ElasticityRequestBody requestBody, HttpServletRequest request,
         HttpServletResponse response) throws Exception {
      verifyInitialized();
      validateInput(clusterName, requestBody);
      clusterMgr.syncSetParam(clusterName,
            requestBody.getActiveComputeNodeNum(),
            requestBody.getMinComputeNodeNum(),
            requestBody.getMaxComputeNodeNum(),
            requestBody.getEnableAuto(),
            requestBody.getIoPriority());
   }

   private void validateInput(String clusterName, ElasticityRequestBody requestBody) {
      if (CommonUtil.isBlank(clusterName) || !CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }

      Integer minComputeNodeNum = requestBody.getMinComputeNodeNum();
      if (minComputeNodeNum != null && minComputeNodeNum < -1) {
         throw BddException.INVALID_PARAMETER("min compute node num", minComputeNodeNum.toString());
      }

      Integer maxComputeNodeNum = requestBody.getMaxComputeNodeNum();
      if (maxComputeNodeNum != null && maxComputeNodeNum < -1) {
         throw BddException.INVALID_PARAMETER("max compute node num", maxComputeNodeNum.toString());
      }

      Integer activeComputeNodeNum = requestBody.getActiveComputeNodeNum();
      // The active compute node number must be a positive number or -1.
      if (activeComputeNodeNum != null && activeComputeNodeNum < -1) {
         logger.error("Invalid instance number: " + activeComputeNodeNum + " !");
         throw BddException.INVALID_PARAMETER("instance number", activeComputeNodeNum.toString());
      }
   }

   /**
    * Replace some failed disks with new disks
    * @param clusterName
    * @param fixDiskSpec
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    */
   @RequestMapping(value = "/cluster/{clusterName}/fix/disk", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void fixCluster(@PathVariable("clusterName") String clusterName,
         @RequestBody FixDiskRequestBody fixDiskSpec,
         HttpServletRequest request, HttpServletResponse response)
         throws Exception {
      verifyInitialized();
      Long taskId =
            clusterMgr.fixDiskFailures(clusterName,
                  fixDiskSpec.getNodeGroupName());
      redirectRequest(taskId, request, response);
   }

   /**
    * Retrieve a cluster information by its name
    * @param clusterName
    * @param details not used by this version
    * @return The cluster information
    */
   @RequestMapping(value = "/cluster/{clusterName}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public ClusterRead getCluster(
         @PathVariable("clusterName") String clusterName,
         @RequestParam(value = "details", required = false) Boolean details) {
      clusterName = CommonUtil.decode(clusterName);
      if (CommonUtil.isBlank(clusterName)
            || !CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }
      return clusterMgr.getClusterByName(clusterName, (details == null) ? false
            : details);
   }

   /**
    * Retrieve a cluster's specification by its name
    * @param clusterName
    * @return The cluster specification
    */
   @RequestMapping(value = "/cluster/{clusterName}/spec", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public ClusterCreate getClusterSpec(
         @PathVariable("clusterName") String clusterName) {
      clusterName = CommonUtil.decode(clusterName);
      if (CommonUtil.isBlank(clusterName)
            || !CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }
      return clusterMgr.getClusterSpec(clusterName);
   }

   /**
    * Get all clusters' information
    * @param details not used by this version
    * @return A list of cluster information
    */
   @RequestMapping(value = "/clusters", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<ClusterRead> getClusters(
         @RequestParam(value = "details", required = false) Boolean details) {
      return clusterMgr.getClusters((details == null) ? false : details);
   }

   // cloud provider API
   /**
    * Add a VC resourcepool into BDE
    * @param rpSpec
    */
   @RequestMapping(value = "/resourcepools", method = RequestMethod.POST, consumes = "application/json")
   @ResponseStatus(HttpStatus.OK)
   public void addResourcePool(@RequestBody ResourcePoolAdd rpSpec) {
      verifyInitialized();
      if (rpSpec == null) {
         throw BddException.INVALID_PARAMETER("rpSpec", null);
      }
      if (CommonUtil.isBlank(rpSpec.getName())
            || !CommonUtil.validateResourceName(rpSpec.getName())) {
         throw BddException.INVALID_PARAMETER("resource pool name",
               rpSpec.getName());
      }
      if (CommonUtil.isBlank(rpSpec.getVcClusterName())
            || !CommonUtil.validateVcResourceName(rpSpec.getVcClusterName())) {
         throw BddException.INVALID_PARAMETER("vCenter Server cluster name",
               rpSpec.getVcClusterName());
      }
      rpSpec.setResourcePoolName(CommonUtil.notNull(
            rpSpec.getResourcePoolName(), ""));
      if (!CommonUtil.isBlank(rpSpec.getResourcePoolName())
            && !CommonUtil.validateVcResourceName(rpSpec.getResourcePoolName())) {
         throw BddException.INVALID_PARAMETER(
               "vCenter Server resource pool name",
               rpSpec.getResourcePoolName());
      }

      vcRpSvc.addResourcePool(rpSpec.getName(), rpSpec.getVcClusterName(),
            rpSpec.getResourcePoolName());
   }

   /**
    * Get all BDE resource pools' information
    * @return a list of BDE resource pool information
    */
   @RequestMapping(value = "/resourcepools", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<ResourcePoolRead> getResourcePools() {
      return vcRpSvc.getAllResourcePoolForRest();
   }

   /**
    * Get a BDE resource pool's information by its name
    * @param rpName
    * @return The resource pool information
    */
   @RequestMapping(value = "/resourcepool/{rpName}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public ResourcePoolRead getResourcePool(@PathVariable("rpName") String rpName) {
      rpName = CommonUtil.decode(rpName);
      if (CommonUtil.isBlank(rpName)
            || !CommonUtil.validateResourceName(rpName)) {
         throw BddException.INVALID_PARAMETER("resource pool name", rpName);
      }
      ResourcePoolRead read = vcRpSvc.getResourcePoolForRest(rpName);
      if (read == null) {
         throw BddException.NOT_FOUND("Resource pool", rpName);
      }
      return read;
   }

   /**
    * Delete a BDE resource pool, and the corresponding VC resource pool will still keep there
    * @param rpName
    */
   @RequestMapping(value = "/resourcepool/{rpName}", method = RequestMethod.DELETE)
   @ResponseStatus(HttpStatus.OK)
   public void deleteResourcePool(@PathVariable("rpName") String rpName) {
      verifyInitialized();
      rpName = CommonUtil.decode(rpName);
      if (CommonUtil.isBlank(rpName)
            || !CommonUtil.validateResourceName(rpName)) {
         throw BddException.INVALID_PARAMETER("resource pool name", rpName);
      }
      vcRpSvc.deleteResourcePool(rpName);
   }

   /**
    * Add a VC datastore, or multiple VC datastores When regex is true into BDE
    * @param dsSpec
    */
   @RequestMapping(value = "/datastores", method = RequestMethod.POST, consumes = "application/json")
   @ResponseStatus(HttpStatus.OK)
   public void addDatastore(@RequestBody DatastoreAdd dsSpec) {
      verifyInitialized();
      if (dsSpec == null) {
         throw BddException.INVALID_PARAMETER("dsSpec", null);
      }
      if (CommonUtil.isBlank(dsSpec.getName())
            || !CommonUtil.validateResourceName(dsSpec.getName())) {
         throw BddException.INVALID_PARAMETER("datestore name",
               dsSpec.getName());
      }
      if (!dsSpec.getRegex() && !CommonUtil.validateVcDataStoreNames(dsSpec.getSpec())) {
         throw BddException.INVALID_PARAMETER("vCenter Server datastore name",
               dsSpec.getSpec().toString());
      }
      datastoreSvc.addDatastores(dsSpec);
   }

   /**
    * Get a BDE datastore information
    * @param dsName
    * @return The BDE datastore information
    */
   @RequestMapping(value = "/datastore/{dsName}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public DatastoreRead getDatastore(@PathVariable("dsName") String dsName) {
      dsName = CommonUtil.decode(dsName);
      if (CommonUtil.isBlank(dsName)
            || !CommonUtil.validateResourceName(dsName)) {
         throw BddException.INVALID_PARAMETER("datestore name", dsName);
      }
      DatastoreRead read = datastoreSvc.getDatastoreRead(dsName);
      if (read == null) {
         throw BddException.NOT_FOUND("Data store", dsName);
      }
      return read;
   }

   /**
    * Get all BDE datastores' information
    * @return A list of BDE datastore information
    */
   @RequestMapping(value = "/datastores", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<DatastoreRead> getDatastores() {
      return datastoreSvc.getAllDatastoreReads();
   }

   /**
    * Delete a BDE datastore, and the corresponding VC datastore will still keep there
    * @param dsName
    */
   @RequestMapping(value = "/datastore/{dsName}", method = RequestMethod.DELETE)
   @ResponseStatus(HttpStatus.OK)
   public void deleteDatastore(@PathVariable("dsName") String dsName) {
      verifyInitialized();
      dsName = CommonUtil.decode(dsName);
      if (CommonUtil.isBlank(dsName)
            || !CommonUtil.validateResourceName(dsName)) {
         throw BddException.INVALID_PARAMETER("date store name", dsName);
      }
      datastoreSvc.deleteDatastore(dsName);
   }

   /**
    * Delete a BDE network, and the corresponding VC network will still keep there
    * @param networkName
    */
   @RequestMapping(value = "/network/{networkName}", method = RequestMethod.DELETE)
   @ResponseStatus(HttpStatus.OK)
   public void deleteNetworkByName(
         @PathVariable("networkName") String networkName) {
      verifyInitialized();
      networkName = CommonUtil.decode(networkName);
      if (CommonUtil.isBlank(networkName)
            || !CommonUtil.validateResourceName(networkName)) {
         throw BddException.INVALID_PARAMETER("network name", networkName);
      }
      networkSvc.removeNetwork(networkName);
   }

   /**
    * Get the BDE network information by its name
    * @param networkName
    * @param details true will return information about allocated ips to cluster nodes
    * @return The BDE network information
    */
   @RequestMapping(value = "/network/{networkName}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public NetworkRead getNetworkByName(
         @PathVariable("networkName") String networkName,
         @RequestParam(value = "details", required = false, defaultValue = "false") final Boolean details) {
      networkName = CommonUtil.decode(networkName);
      if (CommonUtil.isBlank(networkName)
            || !CommonUtil.validateResourceName(networkName)) {
         throw BddException.INVALID_PARAMETER("network name", networkName);
      }
      NetworkRead network =
            networkSvc.getNetworkByName(networkName, details != null ? details
                  : false);
      if (network == null) {
         throw NetworkException.NOT_FOUND("Network", networkName);
      }
      return network;
   }

   /**
    * Get all BDE networks' information
    * @param details true will return information about allocated ips to cluster nodes
    * @return A list of BDE network information
    */
   @RequestMapping(value = "/networks", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<NetworkRead> getNetworks(
         @RequestParam(value = "details", required = false, defaultValue = "false") final Boolean details) {
      return networkSvc.getAllNetworks(details != null ? details : false);
   }

   /**
    * Add a VC network into BDE
    * @param na
    */
   @RequestMapping(value = "/networks", method = RequestMethod.POST, consumes = "application/json")
   @ResponseStatus(HttpStatus.OK)
   public void addNetworks(@RequestBody final NetworkAdd na) {
      verifyInitialized();
      if (CommonUtil.isBlank(na.getName())
            || !CommonUtil.validateResourceName(na.getName())) {
         throw BddException.INVALID_PARAMETER("name", na.getName());
      }
      if (CommonUtil.isBlank(na.getPortGroup())
            || !CommonUtil.validateVcResourceName(na.getPortGroup())) {
         throw BddException.INVALID_PARAMETER("port group", na.getPortGroup());
      }

      if (na.getIsDhcp()) {
         networkSvc.addDhcpNetwork(na.getName(), na.getPortGroup());
      } else {
         if (!IpAddressUtil.isValidNetmask(na.getNetmask())) {
            throw BddException.INVALID_PARAMETER("netmask", na.getNetmask());
         }
         long netmask = IpAddressUtil.getAddressAsLong(na.getNetmask());
         if (na.getGateway() != null && !IpAddressUtil.isValidIp(netmask,
               IpAddressUtil.getAddressAsLong(na.getGateway()))) {
            throw BddException.INVALID_PARAMETER("gateway", na.getGateway());
         }
         if (na.getDns1() != null && !IpAddressUtil.isValidIp(na.getDns1())) {
            throw BddException.INVALID_PARAMETER("primary DNS", na.getDns1());
         }
         if (na.getDns2() != null && !IpAddressUtil.isValidIp(na.getDns2())) {
            throw BddException.INVALID_PARAMETER("secondary DNS", na.getDns2());
         }
         IpAddressUtil.verifyIPBlocks(na.getIpBlocks(), netmask);
         networkSvc.addIpPoolNetwork(na.getName(), na.getPortGroup(),
               na.getNetmask(), na.getGateway(), na.getDns1(), na.getDns2(),
               na.getIpBlocks());
      }
   }

   /**
    * Add ips into an existing BDE network
    * @param networkName
    * @param network
    * @param request
    */
   @RequestMapping(value = "/network/{networkName}", method = RequestMethod.PUT, consumes = "application/json")
   @ResponseStatus(HttpStatus.OK)
   public void increaseIPs(@PathVariable("networkName") String networkName,
         @RequestBody NetworkAdd network, HttpServletRequest request,
         HttpServletResponse response) {
      verifyInitialized();
      networkName = CommonUtil.decode(networkName);
      if (CommonUtil.isBlank(networkName)
            || !CommonUtil.validateResourceName(networkName)) {
         throw BddException.INVALID_PARAMETER("network name", networkName);
      }
      networkSvc.increaseIPs(networkName, network.getIpBlocks());
   }

   /**
    * Get all appmanager types supported by BDE
    * @return The list of Application Manager types in BDE
    */
   @RequestMapping(value = "/appmanagers/types", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<String> getAppManagerTypes() {
      return softwareManagerCollector.getAllAppManagerTypes();
   }

   /**
    * Add an appmanager to BDE
    * @param appManagerAdd
    */
   @RequestMapping(value = "/appmanagers", method = RequestMethod.POST, consumes = "application/json")
   @ResponseStatus(HttpStatus.OK)
   public void addAppManager(@RequestBody final AppManagerAdd appManagerAdd) {
      if (appManagerAdd == null) {
         throw BddException.INVALID_PARAMETER("appManagerAdd", null);
      }
      if (CommonUtil.isBlank(appManagerAdd.getName())) {
         throw BddException.INVALID_PARAMETER("App Manager instance name",
               appManagerAdd.getName());
      }
      softwareManagerCollector.createSoftwareManager(appManagerAdd);
      //pluginService.addPlugin(pluginAdd);
   }

   /**
    * Get a BDE appmanager information
    * @param appManagerName
    * @return The BDE appmanager information
    */
   @RequestMapping(value = "/appmanager/{appManagerName}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public AppManagerRead getAppManager(@PathVariable("appManagerName") String appManagerName) {
      appManagerName = CommonUtil.decode(appManagerName);
      if (CommonUtil.isBlank(appManagerName)
            || !CommonUtil.validateResourceName(appManagerName)) {
         throw BddException.INVALID_PARAMETER("appmanager name", appManagerName);
      }
      AppManagerRead read = softwareManagerCollector.getAppManagerRead(appManagerName);
      if (read == null) {
         throw BddException.NOT_FOUND("App Manager", appManagerName);
      }
      return read;
   }

   /**
    * Get supported stack information of a BDE appmanager
    * @param appManagerName
    * @return The list of supported stacks
    */
   @RequestMapping(value = "/appmanager/{appManagerName}/stacks", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<HadoopStack> getAppManagerStacks(@PathVariable("appManagerName") String appManagerName) {
      appManagerName = CommonUtil.decode(appManagerName);
      if (CommonUtil.isBlank(appManagerName)
            || !CommonUtil.validateResourceName(appManagerName)) {
         throw BddException.INVALID_PARAMETER("appmanager name", appManagerName);
      }
      //TODO: remove switch after Distro Management moved to software-mgmt-plugin-default
      if (Constants.IRONFAN.equalsIgnoreCase(appManagerName)) {
         List<HadoopStack> stacks = new ArrayList<HadoopStack>();
         List<DistroRead> distros = distroManager.getDistros();
         for (DistroRead distro : distros) {
            HadoopStack stack = new HadoopStack();
            stack.setDistro(distro.getName());
            stack.setVendor(distro.getVendor());
            stack.setFullVersion(distro.getVersion());
            stack.setHveSupported(distro.isHveSupported());
            stack.setRoles(distro.getRoles());
            stacks.add(stack);
         }
         return stacks;
      } else {
         SoftwareManager softMgr = softwareManagerCollector.getSoftwareManager(appManagerName);
         if (softMgr == null) {
            throw BddException.NOT_FOUND("App Manager", appManagerName);
         }
         return softMgr.getSupportedStacks();
      }
   }

   /**
    * Get all BDE appmanagers' information
    * @return The list of Application Managers in BDE
    */
   @RequestMapping(value = "/appmanagers", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<AppManagerRead> getAppManagers() {
      return softwareManagerCollector.getAllAppManagerReads();
   }

   /**
    * Store rack list information into BDE for rack related support, such as hadoop rack awareness and node placement policies
    * @param racksInfo
    */
   @RequestMapping(value = "/racks", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.OK)
   public void importRacks(@RequestBody final RackInfoList racksInfo)
         throws Exception {

      verifyInitialized();

      if (racksInfo == null || racksInfo.size() == 0) {
         throw BddException.INVALID_PARAMETER("rack list", "empty");
      }

      rackInfoManager.importRackInfo(racksInfo);
   }

   /**
    * Get the rack list
    * @return A list of rack information
    */
   @RequestMapping(value = "/racks", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<RackInfo> exportRacks() throws Exception {
      return rackInfoManager.exportRackInfo();
   }

   /**
    * Get available distributions information
    * @return A list of distribution information
    */
   @RequestMapping(value = "/distros", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<DistroRead> getDistros() {
      return distroManager.getDistros();
   }

   /**
    * Get available distributions information of application manager
    * @return A list of distribution information
    */
   @RequestMapping(value = "/{appManager}/distros", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public List<DistroRead> getDistros(
         @PathVariable("appManager") String appManager) {
      if(CommonUtil.isBlank(appManager) || Constants.IRONFAN.equalsIgnoreCase(appManager)) {
         throw BddException.INVALID_PARAMETER("appManager", appManager);
      }
      return distroManager.getPluginSupportDistro(appManager);
   }

   /**
    * Get the distribution information by its name such as apache, bigtop, cdh, intel, gphd, hdp, mapr, phd,etc.
    * @param distroName
    * @return The distribution information
    */
   @RequestMapping(value = "/distro/{distroName}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public DistroRead getDistroByName(
         @PathVariable("distroName") String distroName) {
      if (CommonUtil.isBlank(distroName)
            || !CommonUtil.validateDistroName(distroName)) {
         throw BddException.INVALID_PARAMETER("distro name", distroName);
      }
      DistroRead distro = distroManager.getDistroByName(distroName);
      if (distro == null) {
         throw BddException.NOT_FOUND("Distro", distroName);
      }

      return distro;
   }

   /**
    * Get the distribution information of application manager by its name .
    * @param distroName
    * @return The distribution information
    */
   @RequestMapping(value = "/{appManager}/distro/{distroName}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   public DistroRead getDistroByName(
         @PathVariable("appManager") String appManager,
         @PathVariable("distroName") String distroName) {
      if(CommonUtil.isBlank(appManager) || Constants.IRONFAN.equalsIgnoreCase(appManager)) {
         throw BddException.INVALID_PARAMETER("appManager", appManager);
      }
      if (CommonUtil.isBlank(distroName)
            || !CommonUtil.validateDistroName(distroName)) {
         throw BddException.INVALID_PARAMETER("distro name", distroName);
      }
      DistroRead distro = distroManager.getDistroByName(appManager, distroName);
      if (distro == null) {
         throw BddException.NOT_FOUND("Distro", distroName);
      }

      return distro;
   }

   @ExceptionHandler(Throwable.class)
   @ResponseBody
   public BddErrorMessage handleException(Throwable t,
         HttpServletResponse response) {
      if (t instanceof NestedRuntimeException) {
         t = BddException.BAD_REST_CALL(t, t.getMessage());
      }
      BddException ex =
            BddException.wrapIfNeeded(t, "REST API transport layer error.");
      logger.error("rest call error", ex);
      response.setStatus(getHttpErrorCode(ex.getFullErrorId()));
      return new BddErrorMessage(ex.getFullErrorId(), extractErrorMessage(ex));
   }

   private String extractErrorMessage(BddException ex) {
      String msg = ex.getMessage();
      if (ex.getCause() instanceof DataAccessException) {
         msg = "Data access layer exception. See the detailed error in the log";
      }
      return msg;
   }

   private void verifyInitialized() {
      if (!ClusteringService.isInitialized()) {
         throw BddException.INIT_VC_FAIL();
      }
   }
}
