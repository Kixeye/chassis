package com.mangofactory.swagger.configuration;

import java.util.List;
import java.util.concurrent.Future;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.mangofactory.swagger.SwaggerConfiguration;
import com.mangofactory.swagger.filters.Filter;
import com.mangofactory.swagger.models.TypeProcessingRule;
import com.mangofactory.swagger.models.WildcardType;
import com.wordnik.swagger.core.Documentation;
import com.wordnik.swagger.core.DocumentationEndPoint;
import com.wordnik.swagger.core.DocumentationError;
import com.wordnik.swagger.core.DocumentationOperation;
import com.wordnik.swagger.core.DocumentationParameter;

public class ExtensibilityModule {
    public static class GenericTypeProcessor implements TypeProcessingRule {
        private final Class<?> genericType;

        public GenericTypeProcessor(Class<?> genericType) {
            this.genericType = genericType;
        }

        public boolean isIgnorable() { return false; }
        public boolean hasAlternateType() { return true; }

        public ResolvedType originalType() {
            return new TypeResolver().resolve(genericType, WildcardType.class);
        }

        public ResolvedType alternateType(ResolvedType parameterType) {
            return parameterType.getTypeBindings().getBoundType(0);
        }
    }

    public SwaggerConfiguration apply(SwaggerConfiguration configuration) {
        customizeDocumentationFilters(configuration.getDocumentationFilters());
        customizeEndpointFilters(configuration.getEndpointFilters());
        customizeOperationFilters(configuration.getOperationFilters());
        customizeParameterFilters(configuration.getParameterFilters());
        customizeErrorFilters(configuration.getErrorFilters());
        customizeTypeProcessingRules(configuration.getTypeProcessingRules());
        customizeExcludedResources(configuration.getExcludedResources());
        return configuration;
    }

    protected void customizeExcludedResources(List<String> excludedResources) {
    }

    protected void customizeTypeProcessingRules(List<TypeProcessingRule> typeProcessingRules) {
        typeProcessingRules.add(new GenericTypeProcessor(ResponseEntity.class));
        typeProcessingRules.add(new GenericTypeProcessor(DeferredResult.class));
        typeProcessingRules.add(new GenericTypeProcessor(Future.class));
        typeProcessingRules.add(new GenericTypeProcessor(scala.concurrent.Future.class));
    }

    protected void customizeErrorFilters(List<Filter<List<DocumentationError>>> errorFilters) {
    }

    protected void customizeParameterFilters(List<Filter<DocumentationParameter>> parameterFilters) {
    }

    protected void customizeOperationFilters(List<Filter<DocumentationOperation>> operationFilters) {
    }

    protected void customizeEndpointFilters(List<Filter<DocumentationEndPoint>> endpointFilters) {
    }

    protected void customizeDocumentationFilters(List<Filter<Documentation>> documentationFilters) {
    }
}