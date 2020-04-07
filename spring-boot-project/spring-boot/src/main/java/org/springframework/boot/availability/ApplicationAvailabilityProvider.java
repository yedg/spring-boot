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

package org.springframework.boot.availability;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;

/**
 * Holds the availability state of the application.
 * <p>
 * Other application components can get the current state information from the
 * {@code ApplicationAvailabilityProvider}, or publish application evens such as
 * {@link ReadinessStateChangedEvent} and {@link LivenessStateChangedEvent} to update the
 * state of the application.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public class ApplicationAvailabilityProvider implements ApplicationListener<ApplicationEvent> {

	private LivenessState livenessState;

	private ReadinessState readinessState;

	/**
	 * Create a new {@link ApplicationAvailabilityProvider} instance with
	 * {@link LivenessState#BROKEN} and {@link ReadinessState#UNREADY}.
	 */
	public ApplicationAvailabilityProvider() {
		this(LivenessState.BROKEN, ReadinessState.UNREADY);
	}

	/**
	 * Create a new {@link ApplicationAvailabilityProvider} with the given states.
	 * @param livenessState the liveness state
	 * @param readinessState the readiness state
	 */
	public ApplicationAvailabilityProvider(LivenessState livenessState, ReadinessState readinessState) {
		Assert.notNull(livenessState, "LivenessState must not be null");
		Assert.notNull(readinessState, "ReadinessState must not be null");
		this.livenessState = livenessState;
		this.readinessState = readinessState;
	}

	/**
	 * Return the {@link LivenessState} of the application.
	 * @return the liveness state
	 */
	public LivenessState getLivenessState() {
		return this.livenessState;
	}

	/**
	 * Return the {@link ReadinessState} of the application.
	 * @return the readiness state
	 */
	public ReadinessState getReadinessState() {
		return this.readinessState;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof LivenessStateChangedEvent) {
			LivenessStateChangedEvent livenessEvent = (LivenessStateChangedEvent) event;
			this.livenessState = livenessEvent.getLivenessState();
		}
		else if (event instanceof ReadinessStateChangedEvent) {
			ReadinessStateChangedEvent readinessEvent = (ReadinessStateChangedEvent) event;
			this.readinessState = readinessEvent.getReadinessState();
		}
	}

}
