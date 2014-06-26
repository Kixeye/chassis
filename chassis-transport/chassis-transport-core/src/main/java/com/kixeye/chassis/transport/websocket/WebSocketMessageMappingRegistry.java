package com.kixeye.chassis.transport.websocket;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Finds and registers all websocket mappings.
 * 
 * @author ebahtijaragic
 */
@Component
public class WebSocketMessageMappingRegistry implements BeanFactoryPostProcessor {
	private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageMappingRegistry.class);
	
	public static final String WILDCARD_ACTION = "*";
	
	private final Multimap<String, WebSocketAction> actions = HashMultimap.create();

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] controllerBeanNames = beanFactory.getBeanNamesForAnnotation(WebSocketController.class);
		
		if (controllerBeanNames != null && controllerBeanNames.length > 0) {
			try {
				for (String beanName : controllerBeanNames) {
					BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
					
					Class<?> beanClass = Class.forName(beanDef.getBeanClassName());
					
					for (Method method : beanClass.getMethods()) {
						ActionMapping mapping = method.getAnnotation(ActionMapping.class);
						
						if (mapping != null) {
							if (mapping.value() != null && mapping.value().length > 0) {
								Map<String, String> requirements = new HashMap<>();
								
								if (mapping.propertyRequirements() != null) {
									for (ActionPropertyRequirement requirement : mapping.propertyRequirements()) {
										requirements.put(requirement.name(), requirement.value());
									}
								}
								
								for (String action : mapping.value()) {
									logger.info("Registering destination [{}] with handler [{}].", action, method.toString());
									
									actions.put(action, new WebSocketAction(method, requirements));
								}
							}
						}
					}
				}
			} catch (Exception e) {
				throw new FatalBeanException("Unable to configure bean", e);
			}
		} else {
			logger.warn("No WebSocketController beans defined.");
		}
	}
	
	/**
	 * Gets all the actions.
	 * 
	 * @return
	 */
	public Set<String> getActions() {
		return Collections.unmodifiableSet(actions.keySet());
	}
	
	/**
	 * Gets all the action methods for action.
	 * 
	 * @param action
	 * @return
	 */
	public Set<WebSocketAction> getActionMethods(String action) {
		return Sets.union((Set<WebSocketAction>)actions.get(action), (Set<WebSocketAction>)actions.get(WILDCARD_ACTION));
	}
}
