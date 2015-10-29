/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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

package com.vmware.bdd.cli.rest;

import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.utils.CommonUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 5:21 PM
 */
@Component
public class AppManagerRestClient {

    @Autowired
    private RestClient restClient;

    public void add(AppManagerAdd appManagerAdd) {
        final String path = Constants.REST_PATH_APPMANAGERS;
        final HttpMethod httpverb = HttpMethod.POST;
        restClient.createObject(appManagerAdd, path, httpverb);
    }

    public AppManagerRead get(String name) {
        name = CommonUtil.encode(name);
        final String path = Constants.REST_PATH_APPMANAGER;
        final HttpMethod httpverb = HttpMethod.GET;

        return restClient.getObject(name, AppManagerRead.class, path, httpverb,
                false);
    }

    public AppManagerRead[] getAll() {
        final String path = Constants.REST_PATH_APPMANAGERS;
        final HttpMethod httpverb = HttpMethod.GET;

        return restClient.getAllObjects(AppManagerRead[].class, path, httpverb,
                false);
    }

    public DistroRead[] getDistros(String name) {
        name = CommonUtil.encode(name);
        final String path = Constants.REST_PATH_APPMANAGER + "/" + name
                + "/" + Constants.REST_PATH_DISTROS;
        final HttpMethod httpverb = HttpMethod.GET;
        return restClient.getAllObjects(DistroRead[].class, path, httpverb, false);
    }

    public String[] getTypes() {
        final String path = Constants.REST_PATH_APPMANAGERS + "/types";
        final HttpMethod httpverb = HttpMethod.GET;

        return restClient.getAllObjects(String[].class, path, httpverb,
                false);
    }

    public String[] getRoles(String appMgrName, String distroName) {
        final String path =
                Constants.REST_PATH_APPMANAGER + "/" + appMgrName + "/"
                        + Constants.REST_PATH_DISTRO + "/" + distroName + "/"
                        + Constants.REST_PATH_ROLES;
        final HttpMethod httpverb = HttpMethod.GET;

        return restClient.getAllObjects(String[].class, path, httpverb,
                false);
    }

    public String getConfigurations(String appMgrName, String distroName) {
        final String path =
                Constants.REST_PATH_APPMANAGER + "/" + appMgrName + "/"
                        + Constants.REST_PATH_DISTRO + "/" + distroName + "/"
                        + Constants.REST_PATH_CONFIGURATIONS;
        final HttpMethod httpverb = HttpMethod.GET;

        return restClient.getAllObjects(String.class, path, httpverb,
                false);
    }

    public DistroRead getDefaultDistro(String name) {
        name = CommonUtil.encode(name);
        final String path =
                Constants.REST_PATH_APPMANAGER + "/" + name + "/"
                        + Constants.REST_PATH_DEFAULT_DISTRO;
        final HttpMethod httpverb = HttpMethod.GET;

        return restClient.getAllObjects(DistroRead.class, path, httpverb,
                false);
    }

    public DistroRead getDistroByName(String appMangerName, String distroName) {
        appMangerName = CommonUtil.encode(appMangerName);
        distroName = CommonUtil.encode(distroName);
        final String path =
                Constants.REST_PATH_APPMANAGER + "/" + appMangerName + "/"
                        + Constants.REST_PATH_DISTRO;
        final HttpMethod httpverb = HttpMethod.GET;

        return restClient.getObject(distroName, DistroRead.class, path, httpverb,
                false);
    }

    public void modify(AppManagerAdd appManagerAdd) {
        final String path = Constants.REST_PATH_APPMANAGERS;
        final HttpMethod httpverb = HttpMethod.PUT;
        restClient.update(appManagerAdd, path, httpverb);
    }

    public void delete(String id) {
        id = CommonUtil.encode(id);
        final String path = Constants.REST_PATH_APPMANAGER;
        final HttpMethod httpverb = HttpMethod.DELETE;

        restClient.deleteObject(id, path, httpverb);
    }
}
