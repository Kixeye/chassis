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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedConstructor;
import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.mangofactory.swagger.models.DefaultModelPropertiesProvider;
import com.mangofactory.swagger.models.ModelContext;
import com.mangofactory.swagger.models.ModelPropertiesProvider;
import com.mangofactory.swagger.models.ModelProperty;
import com.mangofactory.swagger.models.alternates.AlternateTypeProvider;
import com.wordnik.swagger.model.AllowableValues;
import scala.Option;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Provides support for Jackson's @JsonCreator. This provider
 * will consider any @JsonProperty parameters within a @JsonCreator constructor.
 * <p/>
 * for example:
 * <pre>
 * @code
 * class Message{
 *
 *     private String data
 *
 *     @JsonCreator
 *     public MyClass(@JsonProperty(value="data", required=true) String data){
 *         this.data = data;
 *     }
 *
 *     @JsonProperty("data")
 *     public String getData(){
 *         return this.data;
 *     }
 * }
 * </pre>
 * Swagger model attributes will be read from the getter @ApiModelProperty annotation (as above).
 */
public class CustomModelPropertiesProvider implements ModelPropertiesProvider {

    private CustomAccessorsProvider accessorsProvider;
    private AlternateTypeProvider alternateTypeProvider;
    private DefaultModelPropertiesProvider defaultModelPropertiesProvider;

    public CustomModelPropertiesProvider(
            DefaultModelPropertiesProvider defaultModelPropertiesProvider,
            AlternateTypeProvider alternateTypeProvider,
            CustomAccessorsProvider accessorsProvider) {
        this.accessorsProvider = accessorsProvider;
        this.alternateTypeProvider = alternateTypeProvider;
        this.defaultModelPropertiesProvider = defaultModelPropertiesProvider;
    }

    @Override
    public Iterable<? extends ModelProperty> propertiesForSerialization(ResolvedType type) {
        List<ModelProperty> serializableProperties = newArrayList();
        Iterables.addAll(serializableProperties,defaultModelPropertiesProvider.propertiesForSerialization(type));
        if(type.isInstanceOf(scala.Product.class)){
           //special case for Scala Case Classes.  fields are defined in the constructor
           addConstructors(serializableProperties, type);
        }
        return serializableProperties;
    }

    @Override
    public Iterable<? extends ModelProperty> propertiesForDeserialization(ResolvedType type) {
        List<ModelProperty> deserializableProperties = newArrayList();
        Iterables.addAll(deserializableProperties,defaultModelPropertiesProvider.propertiesForDeserialization(type));
        wrapModelProperties(deserializableProperties);
        return addConstructors(deserializableProperties, type);
    }

    private Iterable<? extends ModelProperty> addConstructors(List<ModelProperty> properties, ResolvedType type) {
        for (ResolvedConstructor constructor : accessorsProvider.constructorsIn(type)) {
            properties.addAll(ConstructorParameterModelProperty.getModelProperties(constructor, alternateTypeProvider));
        }
        return properties;
    }

    private Iterable<? extends ModelProperty> wrapModelProperties(List<ModelProperty> deserializableProperties) {
        return Iterables.transform(deserializableProperties, new Function<ModelProperty, ModelProperty>() {
            @Override
            public ModelProperty apply(ModelProperty modelProperty) {
                /*TODO
                  submitted pull request to have getters added to ModelProperty classes (code below).
                  When accepted, we'll wrap the ModelProperty objects to override the "isRequired()" behavior
                  to use @JsonProperty(required=..) instead of @ApiModelProperty(required=..)

                  https://github.com/martypitt/swagger-springmvc/pull/358
                 */
//                if(modelProperty instanceof BeanModelProperty){
//                    return new ModelPropertyWrapper(modelProperty, ((BeanModelProperty) modelProperty).getMethod());
//                } else if (modelProperty instanceof FieldModelProperty){
//                    return new ModelPropertyWrapper(modelProperty, ((FieldModelProperty) modelProperty).getChildField());
//                }
                return modelProperty;
            }
        });
    }

    private class ModelPropertyWrapper implements ModelProperty {

        private final ModelProperty wrapped;
        private final ResolvedMember resolvedMember;

        public ModelPropertyWrapper(ModelProperty wrapped, ResolvedMember resolvedMember) {
            this.wrapped = wrapped;
            this.resolvedMember = resolvedMember;
        }

        @Override
        public String getName() {
            return wrapped.getName();
        }

        @Override
        public ResolvedType getType() {
            return wrapped.getType();
        }

        @Override
        public String typeName(ModelContext modelContext) {
            return wrapped.typeName(modelContext);
        }

        @Override
        public String qualifiedTypeName() {
            return wrapped.qualifiedTypeName();
        }

        @Override
        public AllowableValues allowableValues() {
            return wrapped.allowableValues();
        }

        @Override
        public Option<String> propertyDescription() {
            return wrapped.propertyDescription();
        }

        @Override
        public boolean isRequired() {
            JsonProperty jsonProperty = resolvedMember.get(JsonProperty.class);
            if (jsonProperty != null) {
                return jsonProperty.required();
            }
            return wrapped.isRequired();
        }
    }
}
