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

import java.util.Collections;
import java.util.List;

/**
 * {@link ConfigDataLocationResolver} for volume mounted locations such as Kubernetes
 * ConfigMaps and Secrets.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class VolumeMountConfigDataLocationResolver implements ConfigDataLocationResolver<VolumeMountConfigDataLocation> {

	private static final String PREFIX = "volumemount:";

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, String location) {
		return location.startsWith(PREFIX);
	}

	@Override
	public List<VolumeMountConfigDataLocation> resolve(ConfigDataLocationResolverContext context, String location) {
		VolumeMountConfigDataLocation resolved = new VolumeMountConfigDataLocation(location.substring(PREFIX.length()));
		return Collections.singletonList(resolved);
	}

}
