package com.kixeye.chassis.transport;

import com.kixeye.chassis.transport.http.ListenableFutureReturnValueHandler;
import com.kixeye.chassis.transport.http.ObserableReturnValueHandler;
import com.kixeye.chassis.transport.http.SerDeHttpMessageConverter;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import com.kixeye.chassis.transport.swagger.SwaggerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Configures Spring MVC.
 *
 * @author ebahtijaragic
 */
@Configuration
@Import(SwaggerConfiguration.class)
public class SpringMvcConfiguration extends DelegatingWebMvcConfiguration {
    @Autowired
    private Set<MessageSerDe> serDes;

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (MessageSerDe serDe : serDes) {
            converters.add(new SerDeHttpMessageConverter(serDe));
        }

        addDefaultHttpMessageConverters(converters);
    }

    @Override
    protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // default to JSON
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * Return a {@link RequestMappingHandlerMapping} ordered at 0 for mapping
     * requests to annotated controllers.
     */
    @Bean
    @Override
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        PathMatchConfigurer configurer = new PathMatchConfigurer();
        configurePathMatch(configurer);
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
        handlerMapping.setOrder(0);
        handlerMapping.setDetectHandlerMethodsInAncestorContexts(true);
        handlerMapping.setInterceptors(getInterceptors());
        handlerMapping.setContentNegotiationManager(mvcContentNegotiationManager());
        if (configurer.isUseSuffixPatternMatch() != null) {
            handlerMapping.setUseSuffixPatternMatch(configurer.isUseSuffixPatternMatch());
        }
        if (configurer.isUseRegisteredSuffixPatternMatch() != null) {
            handlerMapping.setUseRegisteredSuffixPatternMatch(configurer.isUseRegisteredSuffixPatternMatch());
        }
        if (configurer.isUseTrailingSlashMatch() != null) {
            handlerMapping.setUseTrailingSlashMatch(configurer.isUseTrailingSlashMatch());
        }
        if (configurer.getPathMatcher() != null) {
            handlerMapping.setPathMatcher(configurer.getPathMatcher());
        }
        if (configurer.getUrlPathHelper() != null) {
            handlerMapping.setUrlPathHelper(configurer.getUrlPathHelper());
        }
        return handlerMapping;
    }

    /**
     * Gets the message converters.
     *
     * @return
     */
    public final List<HttpMessageConverter<?>> getHttpMessageConverters() {
        return Collections.unmodifiableList(getMessageConverters());
    }

    @Bean
    public List<HandlerMethodReturnValueHandler> returnValueHandlers(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        final List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>(requestMappingHandlerAdapter.getReturnValueHandlers());
        handlers.add(0, new ObserableReturnValueHandler());
        handlers.add(0, new ListenableFutureReturnValueHandler());
        requestMappingHandlerAdapter.setReturnValueHandlers(handlers);
        return handlers;
    }
}
