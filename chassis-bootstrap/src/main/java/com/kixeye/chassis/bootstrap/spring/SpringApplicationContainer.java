package com.kixeye.chassis.bootstrap.spring;

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

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.kixeye.chassis.bootstrap.SpringApplicationDefinition;

/**
 * A ApplicationContainer which uses Spring Java Config (@Configuration) to bootstrap the Spring Framework
 *
 * @author dturner@kixeye.com
 */
public class SpringApplicationContainer extends AbstractSpringApplicationContainer<AnnotationConfigApplicationContext> {
    public SpringApplicationContainer(SpringApplicationDefinition definition) {
        super(new AnnotationConfigApplicationContext(), definition);
    }

    @Override
    protected void registerConfigClasses(Class<?>[] configurationClasses) {
        springContext.register(configurationClasses);
    }
}
