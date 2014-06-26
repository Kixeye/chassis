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

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.kixeye.chassis.bootstrap.SpringApplicationDefinition;

/**
 * Spring ApplicationContainer that runs a Spring Web clientApplication context.  Actual servlet mapping and
 * servlet container is NOT included and is expected to be configured via Spring Java Config.
 *
 * @author dturner@kixeye.com
 */
public class SpringWebApplicationContainer extends AbstractSpringApplicationContainer<AnnotationConfigWebApplicationContext> {

    public SpringWebApplicationContainer(SpringApplicationDefinition definition) {
        super(new AnnotationConfigWebApplicationContext(), definition);
    }

    @Override
    protected void registerConfigClasses(Class<?>[] configurationClasses) {
        springContext.register(configurationClasses);
    }
}
