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

/**
 * {@link ApplicationEvent} sent when the {@link ReadinessState} of the application
 * changes.
 * <p>
 * Any application component can send such events to update the state of the application.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public class ReadinessStateChangedEvent extends ApplicationEvent {

	ReadinessStateChangedEvent(ReadinessState state) {
		super(state);
	}

	public ReadinessState getReadinessState() {
		return (ReadinessState) getSource();
	}

	/**
	 * Create a new {@code ApplicationEvent} signaling that the {@link ReadinessState} is
	 * ready.
	 * @return the application event
	 */
	public static ReadinessStateChangedEvent ready() {
		return new ReadinessStateChangedEvent(ReadinessState.READY);
	}

	/**
	 * Create a new {@code ApplicationEvent} signaling that the {@link ReadinessState} is
	 * unready.
	 * @return the application event
	 */
	public static ReadinessStateChangedEvent unready() {
		return new ReadinessStateChangedEvent(ReadinessState.UNREADY);
	}

}
