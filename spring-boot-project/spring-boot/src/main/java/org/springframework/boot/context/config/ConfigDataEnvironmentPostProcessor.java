/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.config;

import java.util.Collection;

import org.apache.commons.logging.Log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.log.LogMessage;

/**
 * {@link EnvironmentPostProcessor} that loads and apply {@link ConfigData} to Spring's
 * {@link Environment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public class ConfigDataEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * The default order for the processor.
	 */
	public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	private final DeferredLogFactory logFactory;

	private final Log logger;

	public ConfigDataEnvironmentPostProcessor(DeferredLogFactory logFactory) {
		this.logFactory = logFactory;
		this.logger = logFactory.getLog(getClass());
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		addPropertySources(environment, application.getResourceLoader(), application.getAdditionalProfiles());
	}

	protected final void addPropertySources(ConfigurableEnvironment environment, ResourceLoader resourceLoader,
			Collection<String> additionalProfiles) {
		try {
			this.logger.trace("Post-processing environment to add config data");
			resourceLoader = (resourceLoader != null) ? resourceLoader : new DefaultResourceLoader();
			getConfigDataEnvironment(environment, resourceLoader, additionalProfiles).processAndApply();
		}
		catch (UseLegacyConfigProcessingException ex) {
			this.logger.debug(LogMessage.format("Switching to legacy config file processing [%s]",
					ex.getConfigurationProperty()));
			postProcessUsingLegacyApplicationListener(environment, resourceLoader);
		}
	}

	ConfigDataEnvironment getConfigDataEnvironment(ConfigurableEnvironment environment, ResourceLoader resourceLoader,
			Collection<String> additionalProfiles) {
		return new ConfigDataEnvironment(this.logFactory, environment, resourceLoader, additionalProfiles);
	}

	private void postProcessUsingLegacyApplicationListener(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader) {
		getLegacyListener().addPropertySources(environment, resourceLoader);
	}

	@SuppressWarnings("deprecation")
	LegacyConfigFileApplicationListener getLegacyListener() {
		return new LegacyConfigFileApplicationListener(this.logFactory.getLog(ConfigFileApplicationListener.class));
	}

	@SuppressWarnings("deprecation")
	static class LegacyConfigFileApplicationListener extends ConfigFileApplicationListener {

		LegacyConfigFileApplicationListener(Log logger) {
			super(logger);
		}

		@Override
		public void addPropertySources(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
			super.addPropertySources(environment, resourceLoader);
		}

	}

}
