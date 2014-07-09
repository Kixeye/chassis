/*
 * #%L
 * Chassis Transport Swagger
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
package com.kixeye.chassis.transport.swagger;

import static com.google.common.collect.Lists.newArrayList;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotationMap;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.mangofactory.swagger.configuration.JacksonSwaggerSupport;
import com.mangofactory.swagger.models.AccessorsProvider;
import com.mangofactory.swagger.models.BeanModelProperty;
import com.mangofactory.swagger.models.DefaultModelPropertiesProvider;
import com.mangofactory.swagger.models.ModelPropertiesProvider;
import com.mangofactory.swagger.models.ModelProperty;
import com.mangofactory.swagger.models.alternates.AlternateTypeProvider;

/**
 * A hack which provides support for Jackson's @JsonCreator. This provider
 * will consider any @JsonProperty parameters within a @JsonCreator constructor
 * to be valid model properties if there is an associated getter method with the
 * same property value.
 *
 * for example:
 *
 * class Message{
 *
 *     private String data
 *
 *     public MyClass(@JsonProperty("data") String data){
 *         this.data = data;
 *     }
 *
 *     @ApiModelProperty(required = true)
 *     @JsonProperty("data")
 *     public String getData(){
 *         return this.data;
 *     }
 * }
 *
 * Swagger model attributes will be read from the getter @ApiModelProperty annotation (as above).
 *
 * @author dturner@kixeye.com
 */
public class CustomModelPropertiesProvider implements ModelPropertiesProvider {

    private static Pattern getter = Pattern.compile("^get([a-zA-Z_0-9].*)");
    private static Pattern isGetter = Pattern.compile("^is([a-zA-Z_0_9].*)");

    private AccessorsProvider accessorsProvider;
    private AlternateTypeProvider alternateTypeProvider;
    private DeserializationConfig deserializationConfig;
    private TypeResolver typeResolver;
    private DefaultModelPropertiesProvider defaultModelPropertiesProvider;

    public CustomModelPropertiesProvider(
            DefaultModelPropertiesProvider defaultModelPropertiesProvider,
            TypeResolver typeResolver,
            AlternateTypeProvider alternateTypeProvider,
            AccessorsProvider accessorsProvider,
            JacksonSwaggerSupport jacksonSwaggerSupport) {
        this.accessorsProvider = accessorsProvider;
        this.alternateTypeProvider = alternateTypeProvider;
        this.typeResolver = typeResolver;
        this.defaultModelPropertiesProvider = defaultModelPropertiesProvider;
        this.deserializationConfig = jacksonSwaggerSupport.getSpringsMessageConverterObjectMapper().getDeserializationConfig();
    }

    public boolean isGetter(Method method) {
        if (method.getParameterTypes().length == 0) {
            if (getter.matcher(method.getName()).find() &&
                    !method.getReturnType().equals(void.class)) {
                return true;
            }
            if (isGetter.matcher(method.getName()).find() &&
                    method.getReturnType().equals(boolean.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<? extends ModelProperty> propertiesForSerialization(ResolvedType type) {
        return defaultModelPropertiesProvider.propertiesForSerialization(type);
    }

    @Override
    public Iterable<? extends ModelProperty> propertiesForDeserialization(ResolvedType type) {
        List<ModelProperty> deserializableProperties = newArrayList();
        deserializableProperties.addAll(defaultModelPropertiesProvider.deserializableProperties(type));

        BeanDescription beanDescription = deserializationConfig.introspect(TypeFactory.defaultInstance()
                .constructType(type.getErasedType()));

        List<AnnotatedConstructor> annotatedConstructors = beanDescription.getConstructors();

        if (annotatedConstructors.isEmpty()) {
            return deserializableProperties;
        }

        for (AnnotatedConstructor annotatedConstructor : annotatedConstructors) {
            if (annotatedConstructor.getAnnotation(JsonCreator.class) == null) {
                continue;
            }
            Collection<JsonProperty> jsonProperties = findJsonProperties(annotatedConstructor);
            for (JsonProperty jsonProperty : jsonProperties) {
                ResolvedMethod getter = findGetterByJsonPropertyValue(jsonProperty.value(), type);
                if (getter == null) {
                    continue;
                }
                deserializableProperties.add(new BeanModelProperty(jsonProperty.value(),
                        getter, true, typeResolver, alternateTypeProvider));
            }
        }

        return deserializableProperties;
    }

    private ResolvedMethod findGetterByJsonPropertyValue(final String value, ResolvedType resolvedType) {
        return Iterables.tryFind(accessorsProvider.in(resolvedType), new Predicate<ResolvedMethod>() {
            @Override
            public boolean apply(ResolvedMethod resolvedMethod) {
                JsonProperty jsonProperty = resolvedMethod.getRawMember().getAnnotation(JsonProperty.class);
                return jsonProperty != null && jsonProperty.value().endsWith(value) && isGetter(resolvedMethod.getRawMember());
            }
        }).orNull();
    }

    private Collection<JsonProperty> findJsonProperties(AnnotatedConstructor annotatedConstructor) {
        Set<JsonProperty> properties = new HashSet<>();

        for(int i = 0; i< annotatedConstructor.getParameterCount();i++){
            AnnotationMap map = annotatedConstructor.getParameterAnnotations(i);
            JsonProperty prop = map.get(JsonProperty.class);
            if(prop != null){
                properties.add(prop);
            }
        }

        return properties;
    }
}
