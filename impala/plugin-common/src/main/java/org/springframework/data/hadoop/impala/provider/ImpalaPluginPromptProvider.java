/*
 * Copyright 2011-2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.hadoop.impala.provider;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.PromptProvider;
import org.springframework.stereotype.Component;

/**
 * Prompt provider.
 * 
 * @author Jarred Li
 *
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ImpalaPluginPromptProvider implements PromptProvider {

	public String getPrompt() {
		return "Impala>";
	}

	public String name() {
		return "Impala Cli Prompt Provider";
	}
}
