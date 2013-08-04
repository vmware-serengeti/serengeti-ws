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
package org.springframework.data.hadoop.impala.provider;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.springframework.data.hadoop.impala.provider.ImpalaPluginPromptProvider;

/**
 * @author Jarred Li
 *
 */
public class SpringHadoopAdminPromptProviderTest {

	private ImpalaPluginPromptProvider provider;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeMethod
	public void setUp() throws Exception {
		provider = new ImpalaPluginPromptProvider();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterMethod
	public void tearDown() throws Exception {
		provider = null;
	}

	/**
	 * Test method for {@link org.springframework.data.hadoop.impala.provider.ImpalaPluginPromptProvider#getPromptText()}.
	 */
	@Test
	public void testGetPromptText() {
		String prompt = provider.getPrompt();
		Assert.assertNotNull(prompt);
	}

	/**
	 * Test method for {@link org.springframework.data.hadoop.impala.provider.ImpalaPluginPromptProvider#name()}.
	 */
	@Test
	public void testName() {
		String name = provider.name();
		Assert.assertNotNull(name);
	}

}
