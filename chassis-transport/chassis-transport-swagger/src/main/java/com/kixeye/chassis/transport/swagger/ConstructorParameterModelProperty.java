package com.kixeye.chassis.transport.swagger;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedConstructor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.introspect.AnnotationMap;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mangofactory.swagger.models.ModelContext;
import com.mangofactory.swagger.models.ModelProperty;
import com.mangofactory.swagger.models.ResolvedTypes;
import com.mangofactory.swagger.models.alternates.AlternateTypeProvider;
import com.wordnik.swagger.model.AllowableValues;
import scala.Option;

import java.lang.annotation.Annotation;

import static com.mangofactory.swagger.models.ResolvedTypes.simpleQualifiedTypeName;

/**
 * Represents a ModelProperty resolved by a @JsonProperty argument contained within a @JsonCreator constructor.
 *
 * @author dturner@kixeye.com
 */
public class ConstructorParameterModelProperty implements ModelProperty {

    private ResolvedType resolvedParameterType;
    private String typeName;
    private Option<String> propertyDescription = Option.apply(null);
    private AllowableValues allowableValues;
    private String qualifiedTypeName;
    private JsonProperty jsonProperty;

    private static AnnotationMap annotationMap(Annotation [] annotations){
        AnnotationMap annotationMap = new AnnotationMap();
        if(annotations == null){
            return annotationMap;
        }
        for(Annotation annotation:annotations){
            annotationMap.add(annotation);
        }
        return annotationMap;
    }

    /**
     * Creates a collection of ConstructorParameterModelProperty objects from the arguments of the given ResolvedConstructor.
     * Only args annoated with @JsonProperty are included.
     *
     * @param resolvedConstructor the constructor to get
     * @param alternateTypeProvider for resolving alternative types for the found arguments
     * @return the collection of ConstructorParameterModelProperty objects
     */
    public static ImmutableList<ConstructorParameterModelProperty> getModelProperties(ResolvedConstructor resolvedConstructor, AlternateTypeProvider alternateTypeProvider){
        Builder<ConstructorParameterModelProperty> listBuilder = new Builder<>();
        if(resolvedConstructor.getRawMember().getAnnotation(JsonCreator.class) == null){
            return listBuilder.build();
        }
        for(int i=0;i<resolvedConstructor.getArgumentCount();i++){
            AnnotationMap annotationMap = annotationMap(resolvedConstructor.getRawMember().getParameterAnnotations()[i]);
            ResolvedType parameterType = resolvedConstructor.getArgumentType(i);
            if(annotationMap.get(JsonProperty.class) != null){
               listBuilder.add(new ConstructorParameterModelProperty(parameterType, alternateTypeProvider, annotationMap));
            }
        }
        return listBuilder.build();
    }

    /**
     * Creates a ConstructorParameterModelProperty which provides a ModelProperty
     * for constructor parameters.
     *
     * @param resolvedParameterType the parameter type
     * @param alternateTypeProvider provider for resolving alternatives for the given param type
     * @param annotationMap map of annotations for the given parameter. it must contain a @JsonProperty annotation.
     */
    public ConstructorParameterModelProperty(
            ResolvedType resolvedParameterType,
            AlternateTypeProvider alternateTypeProvider,
            AnnotationMap annotationMap) {

        this.resolvedParameterType = alternateTypeProvider.alternateFor(resolvedParameterType);

        if (this.resolvedParameterType == null) {
            this.resolvedParameterType = resolvedParameterType;
        }

        setJsonProperty(annotationMap.get(JsonProperty.class));
        setTypeName();
        setAllowableValues();
        setQualifiedTypeName();
    }

    /**
     * @see com.mangofactory.swagger.models.ModelProperty#getName()
     */
    @Override
    public String getName() {
        return jsonProperty.value();
    }

    /**
     * @see com.mangofactory.swagger.models.ModelProperty#getType()
     */
    @Override
    public ResolvedType getType() {
        return resolvedParameterType;
    }

    /**
     * @see ModelProperty#typeName(com.mangofactory.swagger.models.ModelContext)
     * @param modelContext the ModelContext for the parent type
     */
    @Override
    public String typeName(ModelContext modelContext) {
        return typeName;
    }

    /**
     * @see com.mangofactory.swagger.models.ModelProperty#qualifiedTypeName()
     */
    @Override
    public String qualifiedTypeName() {
        return qualifiedTypeName;
    }

    /**
     * @see com.mangofactory.swagger.models.ModelProperty#allowableValues()
     */
    @Override
    public AllowableValues allowableValues() {
        return allowableValues;
    }

    /**
     * @see com.mangofactory.swagger.models.ModelProperty#propertyDescription()
     */
    @Override
    public Option<String> propertyDescription() {
        return propertyDescription;
    }

    /**
     * @see com.mangofactory.swagger.models.ModelProperty#isRequired()
     */
    @Override
    public boolean isRequired() {
        return jsonProperty.required();
    }

    private void setJsonProperty(JsonProperty jsonProperty) {
        Preconditions.checkNotNull(jsonProperty, "ConstructorParameterModelProperty objects must be annotated with JsonProperty");
        this.jsonProperty = jsonProperty;
    }

    private void setTypeName() {
        this.typeName = ResolvedTypes.typeName(getType());
    }

    private void setQualifiedTypeName() {
        if (getType().getTypeParameters().size() > 0) {
            this.qualifiedTypeName = getType().toString();
            return;
        }
        this.qualifiedTypeName = simpleQualifiedTypeName(getType());
    }

    private void setAllowableValues() {
        this.allowableValues = ResolvedTypes.allowableValues(getType());
    }
}
