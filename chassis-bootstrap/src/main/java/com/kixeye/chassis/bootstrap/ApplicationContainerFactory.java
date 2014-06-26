package com.kixeye.chassis.bootstrap;

/*
 * #%L
 * Chassis Bootstrap
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
 * %%
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
 * #L%
 */

import com.google.common.base.Preconditions;
import com.kixeye.chassis.bootstrap.spring.SpringApplicationContainer;
import com.kixeye.chassis.bootstrap.spring.SpringWebApplicationContainer;

/**
 * Build the appropriate ApplicationContainer based on the ApplicationDefinition. Currently supports Spring and custom.
 *
 * @author dturner@kixeye.com
 */
public class ApplicationContainerFactory {
    public ApplicationContainer getContainerAdapter(ApplicationDefinition definition) {
        Preconditions.checkNotNull(definition);
        if (definition instanceof SpringApplicationDefinition) {
            return buildSpringApplicationContainer((SpringApplicationDefinition) definition);
        }
        return new CustomApplicationContainer(definition);
    }

    private ApplicationContainer buildSpringApplicationContainer(SpringApplicationDefinition definition) {
        if (definition.isWebapp()) {
            return new SpringWebApplicationContainer(definition);
        }
        return new SpringApplicationContainer(definition);
    }
}
