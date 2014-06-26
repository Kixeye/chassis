package com.kixeye.chassis.bootstrap.spring;

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
