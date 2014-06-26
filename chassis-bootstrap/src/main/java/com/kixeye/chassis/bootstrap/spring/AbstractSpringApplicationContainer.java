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

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.kixeye.chassis.bootstrap.ApplicationContainer;
import com.kixeye.chassis.bootstrap.BootstrapException;
import com.kixeye.chassis.bootstrap.SpringApplicationDefinition;
import com.kixeye.chassis.bootstrap.annotation.OnStart;
import com.kixeye.chassis.bootstrap.annotation.OnStop;
import com.kixeye.chassis.bootstrap.annotation.SpringApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * A ApplicationContainer responsible for running Spring. Classes found in @App.configurationClasses() will used
 * as Spring @Configuration classes and will join them with the main Spring context.
 *
 * @author dturner@kixeye.com
 */
//TODO need to scan spring context and dissallow bean of type   . This would mess up configuration ordering.
public abstract class AbstractSpringApplicationContainer<T extends AbstractApplicationContext> implements ApplicationContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSpringApplicationContainer.class);

    private SpringApplicationDefinition definition;

    protected T springContext;

    public AbstractSpringApplicationContainer(T springContext, SpringApplicationDefinition definition) {
        Preconditions.checkNotNull(springContext);
        Preconditions.checkNotNull(definition);

        this.springContext = springContext;
        this.definition = definition;
    }

    public AbstractApplicationContext getApplicationContext(){
        return springContext;
    }

    @Override
    public void onStart() {
        LOGGER.info("starting spring app \"" + definition.getAppName() + "\"");

        invokeConfigStartMethods();
        registerConfigClasses(getConfigurationClasses());
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new ArchaiusSpringPropertySource("archaius"));
        springContext.setEnvironment(environment);
        springContext.refresh();
    }

    protected abstract void registerConfigClasses(Class<?>[] configurationClasses);

    @Override
    public void onStop() {
    	if (springContext.isActive()) {
    		springContext.stop();
    	}
        while(springContext.isRunning()){
            try {
                LOGGER.debug("Waiting for Spring to shutdown...");
                Thread.sleep(300);
            } catch (InterruptedException e) {
                LOGGER.warn("Spring thread interrupted",e);
            }
        }
        invokeConfigStopMethods();
    }
    
    private void invokeConfigStartMethods() {
        Class<?>[] configClasses = getConfigurationClasses();
        
        try {
	    	for (Class<?> configClass : configClasses) {
	    		if (!configClass.isAnnotationPresent(SpringApp.class)) {
		        	for (Method method : configClass.getMethods()) {
		        		if (method.isAnnotationPresent(OnStart.class)) {
		        			if (Modifier.isStatic(method.getModifiers())) {
		        				method.invoke(null);
		        			} else {
		        				LOGGER.warn("Method [{}] must be static. It will be ignored.");
		        			}
		        		}
		        	}
	    		}
	        }
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }

    private void invokeConfigStopMethods() {
    	Class<?>[] configClasses = getConfigurationClasses();

        try {
	    	for (Class<?> configClass : configClasses) {
	    		if (!configClass.isAnnotationPresent(SpringApp.class)) {
		        	for (Method method : configClass.getMethods()) {
		        		if (method.isAnnotationPresent(OnStop.class)) {
		        			if (Modifier.isStatic(method.getModifiers())) {
		        				method.invoke(null);
		        			} else {
		        				LOGGER.warn("Method [{}] must be static. It will be ignored.");
		        			}
		        		}
		        	}
	    		}
	        }
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }
    
    //grab the spring @Configuration classes from the @App.
    //add the BootstrapConfiguration.class
    private Class<?>[] getConfigurationClasses() {
        Set<Class<?>> set = Sets.newHashSet();

        if (definition.getConfigurationClasses().length == 0) {
            throw new BootstrapException("Found no Spring configuration classes to load.");
        }
        for (Class<?> clazz : definition.getConfigurationClasses()) {
            if (clazz == null) {
                continue;
            }
            //make sure all given classes are Spring Configuration classes.
            if (clazz.getAnnotation(org.springframework.context.annotation.Configuration.class) == null) {
                throw new BootstrapException("Encountered a configuration class that is not annotated as a Spring configuration class (" + org.springframework.context.annotation.Configuration.class + "). Class: " + clazz);
            }
            set.add(clazz);
        }
        set.add(BootstrapConfiguration.class);
        return set.toArray(new Class<?>[set.size()]);
    }

}

