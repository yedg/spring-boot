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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributors.BinderOption;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ConfigDataEnvironmentContributors}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataEnvironmentContributorsTests {

	private DeferredLogFactory logFactory = Supplier::get;

	private MockEnvironment environment;

	private Binder binder;

	private ConfigDataImporter importer;

	private ConfigDataActivationContext activationContext;

	@Captor
	private ArgumentCaptor<ConfigDataLocationResolverContext> locationResolverContext;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		this.environment = new MockEnvironment();
		this.binder = Binder.get(this.environment);
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.binder, null);
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory);
		this.importer = new ConfigDataImporter(resolvers, loaders);
		this.activationContext = new ConfigDataActivationContext(CloudPlatform.KUBERNETES, null);
	}

	@Test
	void createCreatesWithInitialContributors() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport("test");
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(contributor));
		Iterator<ConfigDataEnvironmentContributor> iterator = contributors.iterator();
		assertThat(iterator.next()).isSameAs(contributor);
		assertThat(iterator.next().getKind()).isEqualTo(Kind.ROOT);
	}

	@Test
	void withProcessedImportsWhenHasNoUnprocessedImportsReturnsSameInstance() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor
				.ofExisting(new MockPropertySource());
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(contributor));
		ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer,
				this.activationContext);
		assertThat(withProcessedImports).isSameAs(contributors);
	}

	@Test
	void withProcessedImportsResolvesAndLoads() {
		this.importer = mock(ConfigDataImporter.class);
		List<String> locations = Arrays.asList("testimport");
		MockPropertySource propertySource = new MockPropertySource();
		Map<ConfigDataLocation, ConfigData> imported = new LinkedHashMap<>();
		imported.put(new TestConfigDataLocation("a"), new ConfigData(Arrays.asList(propertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), eq(locations))).willReturn(imported);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport("testimport");
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(contributor));
		ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer,
				this.activationContext);
		Iterator<ConfigDataEnvironmentContributor> iterator = withProcessedImports.iterator();
		assertThat(iterator.next().getPropertySource()).isSameAs(propertySource);
		assertThat(iterator.next().getKind()).isEqualTo(Kind.INITIAL_IMPORT);
		assertThat(iterator.next().getKind()).isEqualTo(Kind.ROOT);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void withProcessedImportsResolvesAndLoadsChainedImports() {
		this.importer = mock(ConfigDataImporter.class);
		List<String> initialLocations = Arrays.asList("initialimport");
		MockPropertySource initialPropertySource = new MockPropertySource();
		initialPropertySource.setProperty("spring.config.import", "secondimport");
		Map<ConfigDataLocation, ConfigData> initialImported = new LinkedHashMap<>();
		initialImported.put(new TestConfigDataLocation("a"), new ConfigData(Arrays.asList(initialPropertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), eq(initialLocations)))
				.willReturn(initialImported);
		List<String> secondLocations = Arrays.asList("secondimport");
		MockPropertySource secondPropertySource = new MockPropertySource();
		Map<ConfigDataLocation, ConfigData> secondImported = new LinkedHashMap<>();
		secondImported.put(new TestConfigDataLocation("b"), new ConfigData(Arrays.asList(secondPropertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), eq(secondLocations)))
				.willReturn(secondImported);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor
				.ofInitialImport("initialimport");
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(contributor));
		ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer,
				this.activationContext);
		Iterator<ConfigDataEnvironmentContributor> iterator = withProcessedImports.iterator();
		assertThat(iterator.next().getPropertySource()).isSameAs(secondPropertySource);
		assertThat(iterator.next().getPropertySource()).isSameAs(initialPropertySource);
		assertThat(iterator.next().getKind()).isEqualTo(Kind.INITIAL_IMPORT);
		assertThat(iterator.next().getKind()).isEqualTo(Kind.ROOT);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void withProcessedImportsProvidesLocationResolverContextWithAccessToBinder() {
		MockPropertySource existingPropertySource = new MockPropertySource();
		existingPropertySource.setProperty("test", "springboot");
		ConfigDataEnvironmentContributor existingContributor = ConfigDataEnvironmentContributor
				.ofExisting(existingPropertySource);
		this.importer = mock(ConfigDataImporter.class);
		List<String> locations = Arrays.asList("testimport");
		MockPropertySource propertySource = new MockPropertySource();
		Map<ConfigDataLocation, ConfigData> imported = new LinkedHashMap<>();
		imported.put(new TestConfigDataLocation("a'"), new ConfigData(Arrays.asList(propertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), eq(locations))).willReturn(imported);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport("testimport");
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(existingContributor, contributor));
		contributors.withProcessedImports(this.importer, this.activationContext);
		verify(this.importer).resolveAndLoad(any(), this.locationResolverContext.capture(), any());
		ConfigDataLocationResolverContext context = this.locationResolverContext.getValue();
		assertThat(context.getBinder().bind("test", String.class).get()).isEqualTo("springboot");
	}

	@Test
	void withProcessedImportsProvidesLocationResolverContextWithAccessToParent() {
		this.importer = mock(ConfigDataImporter.class);
		List<String> initialLocations = Arrays.asList("initialimport");
		MockPropertySource initialPropertySource = new MockPropertySource();
		initialPropertySource.setProperty("spring.config.import", "secondimport");
		Map<ConfigDataLocation, ConfigData> initialImported = new LinkedHashMap<>();
		initialImported.put(new TestConfigDataLocation("a"), new ConfigData(Arrays.asList(initialPropertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), eq(initialLocations)))
				.willReturn(initialImported);
		List<String> secondLocations = Arrays.asList("secondimport");
		MockPropertySource secondPropertySource = new MockPropertySource();
		Map<ConfigDataLocation, ConfigData> secondImported = new LinkedHashMap<>();
		secondImported.put(new TestConfigDataLocation("b"), new ConfigData(Arrays.asList(secondPropertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), eq(secondLocations)))
				.willReturn(secondImported);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor
				.ofInitialImport("initialimport");
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(contributor));
		contributors.withProcessedImports(this.importer, this.activationContext);
		verify(this.importer).resolveAndLoad(any(), this.locationResolverContext.capture(), eq(secondLocations));
		ConfigDataLocationResolverContext context = this.locationResolverContext.getValue();
		assertThat(context.getParent()).hasToString("a");
	}

	@Test
	void getBinderProvidesBinder() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("test", "springboot");
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(contributor));
		Binder binder = contributors.getBinder(this.activationContext);
		assertThat(binder.bind("test", String.class).get()).isEqualTo("springboot");
	}

	@Test
	void getBinderWhenHasMultipleSourcesPicksFirst() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("test", "one");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("test", "two");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 0, this.activationContext);
		ConfigDataEnvironmentContributor secondContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 1, this.activationContext);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(firstContributor, secondContributor));
		Binder binder = contributors.getBinder(this.activationContext);
		assertThat(binder.bind("test", String.class).get()).isEqualTo("one");
	}

	@Test
	void getBinderWhenHasInactiveIgnoresInactive() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("test", "one");
		firstPropertySource.setProperty("spring.config.activate.on-profile", "production");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("test", "two");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 0, this.activationContext);
		ConfigDataEnvironmentContributor secondContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 1, this.activationContext);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(firstContributor, secondContributor));
		Binder binder = contributors.getBinder(this.activationContext);
		assertThat(binder.bind("test", String.class).get()).isEqualTo("two");
	}

	@Test
	void getBinderWhenHasPlaceholderResolvesPlaceholder() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("test", "${other}");
		propertySource.setProperty("other", "springboot");
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(contributor));
		Binder binder = contributors.getBinder(this.activationContext);
		assertThat(binder.bind("test", String.class).get()).isEqualTo("springboot");
	}

	@Test
	void getBinderWhenHasPlaceholderAndInactiveResolvesPlaceholderOnlyFromActive() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("other", "one");
		firstPropertySource.setProperty("spring.config.activate.on-profile", "production");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("other", "two");
		secondPropertySource.setProperty("test", "${other}");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 0, this.activationContext);
		ConfigDataEnvironmentContributor secondContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 1, this.activationContext);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(firstContributor, secondContributor));
		Binder binder = contributors.getBinder(this.activationContext);
		assertThat(binder.bind("test", String.class).get()).isEqualTo("two");
	}

	@Test
	void getBinderWhenFailOnBindToInactiveSourceWithFirstInactiveThrowsException() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("test", "one");
		firstPropertySource.setProperty("spring.config.activate.on-profile", "production");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("test", "two");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 0, this.activationContext);
		ConfigDataEnvironmentContributor secondContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 1, this.activationContext);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(firstContributor, secondContributor));
		Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
				.satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
	}

	@Test
	void getBinderWhenFailOnBindToInactiveSourceWithLastInactiveThrowsException() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("test", "one");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("spring.config.activate.on-profile", "production");
		secondPropertySource.setProperty("test", "two");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 0, this.activationContext);
		ConfigDataEnvironmentContributor secondContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 1, this.activationContext);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(firstContributor, secondContributor));
		Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
				.satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
	}

	@Test
	void getBinderWhenFailOnBindToInactiveSourceWithResolveToInactiveThrowsException() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("other", "one");
		firstPropertySource.setProperty("spring.config.activate.on-profile", "production");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("test", "${other}");
		secondPropertySource.setProperty("other", "one");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 0, this.activationContext);
		ConfigDataEnvironmentContributor secondContributor = ConfigDataEnvironmentContributor.ofImported(null,
				configData, 1, this.activationContext);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				Arrays.asList(firstContributor, secondContributor));
		Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
				.satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
	}

	private static class TestConfigDataLocation extends ConfigDataLocation {

		private final String value;

		TestConfigDataLocation(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

}
