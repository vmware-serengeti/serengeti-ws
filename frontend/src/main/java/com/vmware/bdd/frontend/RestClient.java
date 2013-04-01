/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.frontend;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.vmware.bdd.apitypes.Connect;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.DatastoreRead;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.apitypes.ResourcePoolRead;

public class RestClient {

	protected RestTemplate client;
	private String hostUri;
	private String cookieInfo;

	public RestClient() {
		client = new RestTemplate();
	}

	public Connect.ConnectType connect(final String host, final String username, final String password) {
		hostUri = Constants.HTTP_CONNECTION_PREFIX + host + Constants.HTTP_CONNECTION_LOGIN_SUFFIX;

		try {
			ResponseEntity<String> response = login(Constants.REST_PATH_LOGIN,
					String.class, username, password);

			if (response.getStatusCode() == HttpStatus.OK) {
				String cookieValue = response.getHeaders().getFirst(
						"Set-Cookie");
				if (cookieValue.contains(";")) {
					cookieValue = cookieValue.split(";")[0];
				}
				cookieInfo = cookieValue;
				System.out.println(Constants.CONNECT_SUCCESS);
			} else {
				System.out.println(Constants.CONNECT_FAILURE);
				return Connect.ConnectType.ERROR;
			}
		} catch (CliRestException cliRestException) {
			if (cliRestException.getStatus() == HttpStatus.UNAUTHORIZED) {
				System.out.println(Constants.CONNECT_UNAUTHORIZATION);
				return Connect.ConnectType.UNAUTHORIZATION;
			} else {
				System.out.println(Constants.CONNECT_FAILURE + ": "
						+ cliRestException.getStatus() + " "
						+ cliRestException.getMessage().toLowerCase());
				return Connect.ConnectType.ERROR;
			}
		} catch (Exception e) {
			System.out.println(Constants.CONNECT_FAILURE + ": "
					+ e.getCause().getMessage().toLowerCase());
			return Connect.ConnectType.ERROR;
		}
		return Connect.ConnectType.SUCCESS;
	}

	public void disconnect() {
		try {
			logout(Constants.REST_PATH_LOGOUT, String.class);
		} catch (CliRestException cliRestException) {
			if (cliRestException.getStatus() == HttpStatus.UNAUTHORIZED) {
				cookieInfo = "";
			}
		} catch (Exception e) {
			System.out.println(Constants.DISCONNECT_FAILURE + ":"
					+ e.getMessage());
		}
	}

	private <T> ResponseEntity<T> login(final String path,
			final Class<T> respEntityType, final String username,
			final String password) {
		StringBuilder uriBuff = new StringBuilder();
		uriBuff.append(hostUri).append(path);
		if (!"".equals(username) && !"".equals(password)) {
			uriBuff.append("?").append("j_username=").append(username)
					.append("&j_password=").append(password);
		}
		return restPostByUri(uriBuff.toString(), respEntityType);
	}

	private <T> ResponseEntity<T> logout(final String path,
			final Class<T> respEntityType) {
		StringBuilder uriBuff = new StringBuilder();
		uriBuff.append(hostUri).append(path);
		return restGetByUri(uriBuff.toString(), respEntityType);
	}

	private <T> ResponseEntity<T> restGetByUri(String uri,
			Class<T> respEntityType) {
		HttpHeaders headers = buildHeaders();
		HttpEntity<String> entity = new HttpEntity<String>(headers);

		return client.exchange(uri, HttpMethod.GET, entity, respEntityType);
	}

	private <T> ResponseEntity<T> restPostByUri(String uri,
			Class<T> respEntityType) {
		HttpHeaders headers = buildHeaders();
		HttpEntity<String> entity = new HttpEntity<String>(headers);

		return client.exchange(uri, HttpMethod.POST, entity, respEntityType);
	}

	private ResponseEntity<String> restPost(String path, Object entity) {
		String targetUri = hostUri + Constants.HTTP_CONNECTION_API + path;

		HttpHeaders headers = buildHeaders();
		HttpEntity<Object> postEntity = new HttpEntity<Object>(entity, headers);

		return client.exchange(targetUri, HttpMethod.POST, postEntity,
				String.class);
	}

	private HttpHeaders buildHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedTypes = new ArrayList<MediaType>();
		acceptedTypes.add(MediaType.APPLICATION_JSON);
		acceptedTypes.add(MediaType.TEXT_HTML);
		headers.setAccept(acceptedTypes);

		headers.add("Cookie", cookieInfo == null ? "" : cookieInfo);

		return headers;
	}

	//
	// Cluster Actions
	//
	
	public void deleteCluster(String name) {
		HttpEntity<Object> entity = new HttpEntity<Object>(name, buildHeaders());
		String targetUri = hostUri + Constants.HTTP_CONNECTION_API
				+ Constants.REST_PATH_CLUSTER + "/" + name;
		
		client.exchange(targetUri, HttpMethod.DELETE, entity, String.class);
	}

	public String createCluster(ClusterCreate cluster) {
		ResponseEntity<String> response = restPost(Constants.REST_PATH_CLUSTERS, cluster);

		return response.getBody();
	}

	public List<ClusterRead> getClusters() {
		ResponseEntity<ClusterRead[]> response = restGetByUri(hostUri
				+ Constants.HTTP_CONNECTION_API 
				+ Constants.REST_PATH_CLUSTERS,	ClusterRead[].class);
		return Arrays.asList(response.getBody());
	}

	public List<ResourcePoolRead> getResourcePools() {
		ResponseEntity<ResourcePoolRead[]> response = restGetByUri(hostUri
				+ Constants.HTTP_CONNECTION_API
				+ Constants.REST_PATH_RESOURCEPOOLS, ResourcePoolRead[].class);
		return Arrays.asList(response.getBody());
	}

	public List<DatastoreRead> getDatastores() {
		ResponseEntity<DatastoreRead[]> response = restGetByUri(hostUri
				+ Constants.HTTP_CONNECTION_API
				+ Constants.REST_PATH_DATASTORES, DatastoreRead[].class);
		return Arrays.asList(response.getBody());
	}

	public List<NetworkRead> getNetworks() {
		ResponseEntity<NetworkRead[]> response = restGetByUri(hostUri
				+ Constants.HTTP_CONNECTION_API
				+ Constants.REST_PATH_NETWORKS, NetworkRead[].class);
		return Arrays.asList(response.getBody());
	}

}
