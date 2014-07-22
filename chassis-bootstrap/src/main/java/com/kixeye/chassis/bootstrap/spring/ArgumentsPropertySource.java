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

import com.google.common.collect.Iterators;
import com.kixeye.chassis.bootstrap.AppMain.Arguments;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * Exposes {@link Arguments} as a source of properties to Spring
 *
 * @author dturner@kixeye.com
 */
public class ArgumentsPropertySource extends EnumerablePropertySource<Arguments> {

    public ArgumentsPropertySource(String name, Arguments arguments) {
        super(name, arguments);
    }

    @Override
    public Object getProperty(String name) {
        return source.asPropertyMap().get(name);
    }

    @Override
    public String[] getPropertyNames() {
        return Iterators.toArray(source.asPropertyMap().keySet().iterator(), String.class);
    }

}
