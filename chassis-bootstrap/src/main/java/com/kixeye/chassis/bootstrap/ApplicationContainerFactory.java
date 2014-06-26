package com.kixeye.chassis.bootstrap;

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
