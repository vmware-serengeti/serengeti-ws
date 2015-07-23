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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.exception.ExceptionUtils;
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

import com.vmware.bdd.aop.annotation.RestCallPointcut;
import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.DatacenterMap;
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
import com.vmware.bdd.apitypes.ValidateResult;
import com.vmware.bdd.apitypes.VcClusterMap;
import com.vmware.bdd.apitypes.VcResourceMap;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.exception.WarningMessageException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.JobManager;
import com.vmware.bdd.manager.RackInfoManager;
import com.vmware.bdd.manager.SWMgrCollectorInternalException;
import com.vmware.bdd.manager.ScaleManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.service.impl.ClusteringService;
import com.vmware.bdd.service.resmgmt.IAppManagerService;
import com.vmware.bdd.service.resmgmt.IDatastoreService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
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

   /**
    * Get REST api version
    * @return REST api version
    */
   @RequestMapping(value = "/hello", method = RequestMethod.GET, produces = "application/json")
   @ResponseStatus(HttpStatus.OK)
   @ResponseBody
   public Map<String, String> getHello() {
      Map<String, String> serverInfo = new HashMap<String, String>();
      serverInfo.put("version", Constants.VERSION);
      return serverInfo;
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
      response.setHeader(Constants.RESPONSE_HEADER_LOCATION, url.toString());
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

   @RequestMapping(value = "/cluster/{clusterName}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
   public void updateCluster(@PathVariable("clusterName") String clusterName,
         @RequestBody(required = false) ClusterCreate clusterUpdate,
         @RequestParam(value = "state", required = false) String state,
         @RequestParam(value = "force", required = false, defaultValue = "false") Boolean force,
         @RequestParam(value = "ignorewarning", required = false, defaultValue = "false") boolean ignoreWarning,
         HttpServletRequest request, HttpServletResponse response) throws Exception {
      if (state != null) {
         // forward request to startStopResumeCluster() for backward compatibility
         request.getRequestDispatcher(clusterName + "/action").forward(request, response);
         response.setStatus(HttpStatus.ACCEPTED.value());
         return;
      }
      verifyInitialized();
      clusterName = CommonUtil.decode(clusterName);
      if (CommonUtil.isBlank(clusterName) || !CommonUtil.validateResourceName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }

      clusterMgr.updateCluster(clusterUpdate, ignoreWarning);
      response.setStatus(HttpStatus.OK.value());
   }

   /**
    * Start or stop a normal cluster, or resume a failed cluster after adjusting the resources allocated to this cluster
    * @param clusterName
    * @param state Can be start, stop, or resume
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    */
   @RequestMapping(value = "/cluster/{clusterName}/action", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void startStopResumeCluster(
         @PathVariable("clusterName") String clusterName,
         @RequestParam(value = "state", required = true) String state,
         @RequestParam(value = "force", required = false, defaultValue = "false") Boolean force,
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
         taskId = clusterMgr.startCluster(clusterName, force);
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
    * @param instanceNum The target instance number after resize. It can be larger/smaller than existing instance number in this node group
    * @param request
    * @return Return a response with Accepted status and put task uri in the Location of header that can be used to monitor the progress
    */
   @RequestMapping(value = "/cluster/{clusterName}/nodegroup/{groupName}/instancenum", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.ACCEPTED)
   public void resizeCluster(@PathVariable("clusterName") String clusterName,
         @PathVariable("groupName") String groupName,
         @RequestBody Integer instanceNum,
         @RequestParam(value = "force", required = false, defaultValue = "false") Boolean force,
         HttpServletRequest request,
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
            clusterMgr.resizeCluster(clusterName, groupName, instanceNum, force);
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
   @RestCallPointcut
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
   @RestCallPointcut
   public ClusterCreate getClusterSpec(
         @PathVariable("clusterName") String clusterName) {
      clusterName = CommonUtil.decode(clusterName);
      if (CommonUtil.isBlank(clusterName)
            || !CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }
      return clusterMgr.getClusterSpec(clusterName);
   }

   @RequestMapping(value = "/cluster/{clusterName}/rack", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public Map<String, String> getClusterRackTopology(
         @PathVariable("clusterName") String clusterName,
         @RequestParam(value = "topology", required = false) String topology) {
      clusterName = CommonUtil.decode(clusterName);
      if (CommonUtil.isBlank(clusterName)
            || !CommonUtil.validateClusterName(clusterName)) {
         throw BddException.INVALID_PARAMETER("cluster name", clusterName);
      }
      return clusterMgr.getRackTopology(clusterName, topology);
   }

   /**
    * Get all clusters' information
    * @param details not used by this version
    * @return A list of cluster information
    */
   @RequestMapping(value = "/clusters", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
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
   @RestCallPointcut
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
   @RestCallPointcut
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
   @RestCallPointcut
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
   @RestCallPointcut
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
   @RestCallPointcut
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
   @RestCallPointcut
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
   @RestCallPointcut
   public List<DatastoreRead> getDatastores() {
      return datastoreSvc.getAllDatastoreReads();
   }

   /**
    * Delete a BDE datastore, and the corresponding VC datastore will still keep there
    * @param dsName
    */
   @RequestMapping(value = "/datastore/{dsName}", method = RequestMethod.DELETE)
   @ResponseStatus(HttpStatus.OK)
   @RestCallPointcut
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
   @RestCallPointcut
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
   @RestCallPointcut
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
   @RestCallPointcut
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
   @RestCallPointcut
   public void addNetworks(@RequestBody final NetworkAdd na) {
      verifyInitialized();

      List<String> missingParameters = new ArrayList<String>();
      if (CommonUtil.isBlank(na.getName())) {
         missingParameters.add("name");
      }
      if (CommonUtil.isBlank(na.getPortGroup())) {
         missingParameters.add("portGroup");
      }
      if (na.getDnsType() == null) {
         missingParameters.add("dnsType");
      }
      if (na.getIsGenerateHostname() == null) {
         missingParameters.add("generateHostname");
      }
      if (!missingParameters.isEmpty()) {
         throw BddException.MISSING_PARAMETER(missingParameters);
      }

      if (!CommonUtil.validateResourceName(na.getName())) {
         throw BddException.INVALID_PARAMETER("name", na.getName());
      }

      if (!CommonUtil.validateDnsType(na.getDnsType())) {
         throw BddException.INVALID_DNS_TYPE(na.getDnsType());
      }

      if (na.getIsDhcp()) {
         networkSvc.addDhcpNetwork(na.getName(), na.getPortGroup(), na.getDnsType(), na.getIsGenerateHostname());
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
               na.getIpBlocks(), na.getDnsType(), na.getIsGenerateHostname());
      }
   }

   /**
    * Add ips into an existing BDE network
    * @param networkName
    * @param request
    */
   @RequestMapping(value = "/network/{networkName}", method = RequestMethod.PUT, consumes = "application/json")
   @ResponseStatus(HttpStatus.OK)
   @RestCallPointcut
   public void updateNetwork(@PathVariable("networkName") String networkName,
         @RequestBody NetworkAdd networkAdd, HttpServletRequest request,
         HttpServletResponse response) {
      verifyInitialized();
      networkName = CommonUtil.decode(networkName);
      if (CommonUtil.isBlank(networkName)
            || !CommonUtil.validateResourceName(networkName)) {
         throw BddException.INVALID_PARAMETER("network name", networkName);
      }
      if (networkAdd.getIpBlocks() == null && networkAdd.getDnsType() == null && networkAdd.getIsGenerateHostname() == null) {
         throw BddException.INVALID_OPTIONS_WHEN_UPDATE_NETWORK(new String[]{"addIP", "dnsType", "generateHostname"});
      }
      if (networkAdd.getDnsType() != null && !CommonUtil.validateDnsType(networkAdd.getDnsType())) {
         throw BddException.INVALID_DNS_TYPE(networkAdd.getDnsType());
      }
      networkSvc.updateNetwork(networkName, networkAdd);
   }

   /**
    * Get all appmanager types supported by BDE
    * @return The list of Application Manager types in BDE
    */
   @RequestMapping(value = "/appmanagers/types", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public List<String> getAppManagerTypes() {
      return softwareManagerCollector.getAllAppManagerTypes();
   }

   /**
    * Add an appmanager to BDE
    * @param appManagerAdd
    */
   @RequestMapping(value = "/appmanagers", method = RequestMethod.POST, consumes = "application/json")
   @ResponseStatus(HttpStatus.OK)
   @RestCallPointcut
   public void addAppManager(@RequestBody final AppManagerAdd appManagerAdd) {
      if (appManagerAdd == null) {
         throw BddException.INVALID_PARAMETER("appManagerAdd", null);
      }
      if (CommonUtil.isBlank(appManagerAdd.getName())
            || !CommonUtil.validateResourceName(appManagerAdd.getName())) {
         throw BddException.INVALID_PARAMETER("appmanager name",
               appManagerAdd.getName());
      }
      softwareManagerCollector.createSoftwareManager(appManagerAdd);
      //pluginService.addPlugin(pluginAdd);
   }

   /**
    * Modify an app manager
    * @param appManagerAdd
    * @param request
    * @param response
    */
   @RequestMapping(value = "/appmanagers", method = RequestMethod.PUT, consumes = "application/json")
   @ResponseStatus(HttpStatus.OK)
   @RestCallPointcut
   public void modifyAppManager(@RequestBody AppManagerAdd appManagerAdd,
         HttpServletRequest request,
         HttpServletResponse response) {
      if (appManagerAdd == null) {
         throw BddException.INVALID_PARAMETER("appManagerAdd", null);
      }


      try {
         softwareManagerCollector.modifySoftwareManager(appManagerAdd);
      } catch (SWMgrCollectorInternalException ex) {
         throw BddException.wrapIfNeeded(ex, "App Manager Management Error");
      }
   }

   /**
    * Delete an app manager
    * @param appManagerName
    */
   @RequestMapping(value = "/appmanager/{appManagerName}", method = RequestMethod.DELETE)
   @ResponseStatus(HttpStatus.OK)
   @RestCallPointcut
   public void deleteAppManager(@PathVariable("appManagerName") String appManagerName) {
      appManagerName = CommonUtil.decode(appManagerName);
      if (CommonUtil.isBlank(appManagerName)
            || !CommonUtil.validateResourceName(appManagerName)) {
         throw BddException.INVALID_PARAMETER("appmanager name", appManagerName);
      }

      try {
         softwareManagerCollector.deleteSoftwareManager(appManagerName);
      } catch (SWMgrCollectorInternalException ex) {
         throw BddException.wrapIfNeeded(ex, "App Manager Management Error");
      }
   }

   /**
    * Get a BDE appmanager information
    * @param appManagerName
    * @return The BDE appmanager information
    */
   @RequestMapping(value = "/appmanager/{appManagerName}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public AppManagerRead getAppManager(@PathVariable("appManagerName") String appManagerName) {
      appManagerName = CommonUtil.decode(appManagerName);
      if (CommonUtil.isBlank(appManagerName)
            || !CommonUtil.validateResourceName(appManagerName)) {
         throw BddException.INVALID_PARAMETER("appmanager name", appManagerName);
      }
      AppManagerRead read = softwareManagerCollector.getAppManagerRead(appManagerName);
      return read;
   }

   /**
    * Get supported distro information of a BDE appmanager
    * @param appManagerName
    * @return The list of supported distros
    */
   @RequestMapping(value = "/appmanager/{appManagerName}/distros", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public List<DistroRead> getAppManagerDistros(@PathVariable("appManagerName") String appManagerName) {
      appManagerName = CommonUtil.decode(appManagerName);
      if (CommonUtil.isBlank(appManagerName)
            || !CommonUtil.validateResourceName(appManagerName)) {
         throw BddException.INVALID_PARAMETER("appmanager name", appManagerName);
      }
      SoftwareManager softMgr =
            softwareManagerCollector.getSoftwareManager(appManagerName);
      List<HadoopStack> stacks = null;
      try {
         stacks = softMgr.getSupportedStacks();
      } catch (Exception e) {
         // call SoftwareManagerCollector.loadSoftwareManager() to detect connection issue
         logger.error("Failed to get supported stacks from appmaanger "
               + appManagerName, e);
         SoftwareManager softwareManager =
               softwareManagerCollector.getSoftwareManager(appManagerName);
         // if succeed, call getSupportedStacks() again
         try {
            logger.info("Call getSupportedStacks() again.");
            stacks = softwareManager.getSupportedStacks();
         } catch (Exception ex) {
            logger.error("Failed to get supported stacks from appmanager "
                  + appManagerName + " again.", ex);
            throw SoftwareManagerCollectorException.CONNECT_FAILURE(
                  appManagerName, ExceptionUtils.getRootCauseMessage(ex));
         }
      }

      List<DistroRead> distros = new ArrayList<DistroRead>(stacks.size());
      for (HadoopStack stack : stacks) {
         distros.add(new DistroRead(stack));
      }
      return distros;
   }

   /**
    * Get supported role information of a distro of a BDE appmanager
    * @param appManagerName
    * @param distroName
    * @return The list of supported roles
    */
   @RequestMapping(value = "/appmanager/{appManagerName}/distro/{distroName}/roles", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public List<String> getAppManagerDistroRoles(
         @PathVariable("appManagerName") String appManagerName,
         @PathVariable("distroName") String distroName) {
      appManagerName = CommonUtil.decode(appManagerName);
      if (CommonUtil.isBlank(appManagerName)
            || !CommonUtil.validateResourceName(appManagerName)) {
         throw BddException.INVALID_PARAMETER("appmanager name", appManagerName);
      }
      SoftwareManager softMgr =
            softwareManagerCollector.getSoftwareManager(appManagerName);
      List<HadoopStack> stacks = softMgr.getSupportedStacks();
      HadoopStack hadoopStack = null;
      for (HadoopStack stack : stacks) {
         if (distroName.equals(stack.getDistro())) {
            hadoopStack = stack;
            break;
         }
      }
      if (hadoopStack == null) {
         throw BddException.NOT_FOUND("Distro", distroName);
      } else {
         return hadoopStack.getRoles();
      }
   }

   /**
    * Get supported configuration information of a distro of a BDE appmanager
    * @param appManagerName
    * @param distroName
    * @return The list of supported configurations
    */
   @RequestMapping(value = "/appmanager/{appManagerName}/distro/{distroName}/configurations", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public String getAppManagerStackConfigurations(
         @PathVariable("appManagerName") String appManagerName,
         @PathVariable("distroName") String distroName) {
      appManagerName = CommonUtil.decode(appManagerName);
      if (CommonUtil.isBlank(appManagerName)
            || !CommonUtil.validateResourceName(appManagerName)) {
         throw BddException.INVALID_PARAMETER("appmanager name", appManagerName);
      }

      SoftwareManager softMgr =
            softwareManagerCollector.getSoftwareManager(appManagerName);
      List<HadoopStack> stacks = softMgr.getSupportedStacks();
      HadoopStack hadoopStack = null;
      for (HadoopStack stack : stacks) {
         if (distroName.equals(stack.getDistro())) {
            hadoopStack = stack;
            break;
         }
      }
      if (hadoopStack == null) {
         throw BddException.NOT_FOUND("distro", distroName);
      } else {
         return softMgr.getSupportedConfigs(hadoopStack);
      }
   }


   @RequestMapping(value = "/appmanager/{appManagerName}/defaultdistro", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public DistroRead getDefaultStack(@PathVariable("appManagerName") String appManagerName) {
      appManagerName = CommonUtil.decode(appManagerName);
      if (CommonUtil.isBlank(appManagerName)
            || !CommonUtil.validateResourceName(appManagerName)) {
         throw BddException.INVALID_PARAMETER("appmanager name", appManagerName);
      }
      SoftwareManager softMgr = softwareManagerCollector.getSoftwareManager(appManagerName);
      HadoopStack stack = softMgr.getDefaultStack();
      if (stack == null) {
         return null;
      } else {
         return new DistroRead(stack);
      }
   }


   @RequestMapping(value = "/appmanager/{appManagerName}/distro/{distroName:.+}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public DistroRead getDistroByName(
         @PathVariable("appManagerName") String appManagerName,
         @PathVariable("distroName") String distroName) {
      appManagerName = CommonUtil.decode(appManagerName);
      distroName = CommonUtil.decode(distroName);
      if (CommonUtil.isBlank(appManagerName)
            || !CommonUtil.validateResourceName(appManagerName)) {
         throw BddException.INVALID_PARAMETER("appmanager name", appManagerName);
      }
      SoftwareManager softMgr = softwareManagerCollector.getSoftwareManager(appManagerName);
      HadoopStack stack = clusterMgr.filterDistroFromAppManager(softMgr, distroName);
      if (stack == null) {
         return null;
      } else {
         return new DistroRead(stack);
      }
   }

   /**
    * Get all BDE appmanagers' information
    * @return The list of Application Managers in BDE
    */
   @RequestMapping(value = "/appmanagers", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public List<AppManagerRead> getAppManagers() {
      return softwareManagerCollector.getAllAppManagerReads();
   }

   /**
    * Store rack list information into BDE for rack related support, such as hadoop rack awareness and node placement policies
    * @param racksInfo A list of rack information
    */
   @RequestMapping(value = "/racks", method = RequestMethod.PUT)
   @ResponseStatus(HttpStatus.OK)
   @RestCallPointcut
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
   @RestCallPointcut
   public List<RackInfo> exportRacks() throws Exception {
      return rackInfoManager.exportRackInfo();
   }

   /**
    * Get available distributions information
    * @return A list of distribution information
    */
   @RequestMapping(value = "/distros", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public List<DistroRead> getDistros() {
      //TODO handle appmanager case
      SoftwareManager softMgr =
            softwareManagerCollector.getSoftwareManager(Constants.IRONFAN);
      List<HadoopStack> stacks = softMgr.getSupportedStacks();
      List<DistroRead> distros = new ArrayList<DistroRead>(stacks.size());
      for (HadoopStack stack : stacks) {
         distros.add(new DistroRead(stack));
      }
      return distros;
   }

   /**
    * Get the distribution information by its name such as apache, bigtop, cdh, intel, gphd, hdp, mapr, phd,etc.
    * @param distroName
    * @return The distribution information
    */
   @RequestMapping(value = "/distro/{distroName}", method = RequestMethod.GET, produces = "application/json")
   @ResponseBody
   @RestCallPointcut
   public DistroRead getDistroByName(
         @PathVariable("distroName") String distroName) {
      if (CommonUtil.isBlank(distroName)
            || !CommonUtil.validateDistroName(distroName)) {
         throw BddException.INVALID_PARAMETER("distro name", distroName);
      }

      //TODO handle appmanager case
      SoftwareManager softMgr =
            softwareManagerCollector.getSoftwareManager(Constants.IRONFAN);
      List<HadoopStack> stacks = softMgr.getSupportedStacks();
      for (HadoopStack stack : stacks) {
         if (distroName.equalsIgnoreCase(stack.getDistro())) {
            return new DistroRead(stack);
         }
      }
      throw BddException.NOT_FOUND("Distro", distroName);
   }

   @RequestMapping(value = "/cluster/{clusterName}/validate", method = RequestMethod.POST, produces = "application/json")
   @ResponseBody
   public ValidateResult validateBlueprint(@RequestBody ClusterCreate createSpec) {
      SoftwareManager softwareManager =
            clusterMgr.getClusterConfigMgr().getSoftwareManager(
                  createSpec.getAppManager());
      ClusterBlueprint blueprint = createSpec.toBlueprint();
      ValidateResult result = new ValidateResult();
      boolean validated = false;
      try {
         validated = softwareManager.validateBlueprint(blueprint);
      } catch (ValidationException ve) {
         result.setFailedMsgList(ve.getFailedMsgList());
         result.setWarningMsgList(ve.getWarningMsgList());
      }
      result.setValidated(validated);
      return result;
   }

   private void verifyInitialized() {
      if (!ClusteringService.isInitialized()) {
         throw BddException.INIT_VC_FAIL();
      }
   }

   /**
    * Recover clusters for disaster recovery from a data center to another
    * @param resMap: vc resource name mapping from one data center to another
    * @param request
    */
   @RequestMapping(value = "/recover", method = RequestMethod.PUT, consumes = "application/json")
   @ResponseStatus(HttpStatus.OK)
   public void recoverClusters(@RequestBody VcResourceMap vcResMap,
         HttpServletRequest request, HttpServletResponse response)
         throws Exception {
      List<VcClusterMap> clstMaps = null;
      List<DatacenterMap> dcmaps = vcResMap.getDatacenters();
      if ( null != dcmaps ) {
         // currently for bde, only one data center is considered
         DatacenterMap dcmap = dcmaps.get(0);
         clstMaps = dcmap.getClusters();

         // update all the resource pools on the vc cluster info from name mapping
         List<ResourcePoolRead> allRps = vcRpSvc.getAllResourcePoolForRest();
         for ( ResourcePoolRead rp : allRps ) {
            String rpName = rp.getRpName();
            String srcVcCluster = rp.getVcCluster();
            for ( VcClusterMap clstmap : clstMaps ) {
               String clst = clstmap.getSrc();
               if ( srcVcCluster.equals(clst) ) {
                  vcRpSvc.updateResourcePool(rpName, clstmap.getTgt(), null);
               }
            }
         }
      }

      // update all the nodes on vm moid and host_name
      // if clstMaps is null, it means doing recover on the same data center
      clusterMgr.recoverClusters(clstMaps);
   }
}
