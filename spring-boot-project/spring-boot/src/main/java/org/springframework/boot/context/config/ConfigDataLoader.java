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

import java.io.IOException;

import org.apache.commons.logging.Log;

/**
 * Strategy class that can be used used to load {@link ConfigData} instances from a
 * {@link ConfigDataLocation location}. Implementations should be added as a
 * {@code spring.factories} entries. The following constructor parameter types are
 * supported:
 * <ul>
 * <li>{@link Log} - if the resolver needs deferred logging</li>
 * </ul>
 * <p>
 * Multiple loaders cannot claim the same location.
 *
 * @param <L> the location type
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public interface ConfigDataLoader<L extends ConfigDataLocation> {

	/**
	 * Returns if the specified location can be loaded by this instance.
	 * @param location the location to check.
	 * @return if the location is supported by this loader
	 */
	default boolean isLoadable(L location) {
		return true;
	}

	/**
	 * Load {@link ConfigData} for the given location.
	 * @param location the location to load
	 * @return the loaded config data or {@code null} if the location should be skipped
	 * @throws IOException on IO error
	 */
	ConfigData load(L location) throws IOException;

}
