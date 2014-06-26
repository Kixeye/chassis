package com.kixeye.chassis.bootstrap.spring;

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
