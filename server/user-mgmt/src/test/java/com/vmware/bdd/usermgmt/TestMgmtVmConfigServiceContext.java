/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.usermgmt;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;

import com.vmware.bdd.usermgmt.persist.MgmtVmCfgDao;
import com.vmware.bdd.usermgmt.persist.MgmtVmCfgEao;
import com.vmware.bdd.usermgmt.persist.UserMgmtServerDao;

/**
 * Created By xiaoliangl on 12/3/14.
 */
@Configuration
@ComponentScan(basePackages = "com.vmware.bdd.usermgmt",
      includeFilters = {
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = MgmtVmCfgService.class)
      }

)
public class TestMgmtVmConfigServiceContext {

   @Bean
   @Autowired
   public MgmtVmCfgEao mgmtVmCfgEao() {
      return Mockito.mock(MgmtVmCfgEao.class);
   }

}
