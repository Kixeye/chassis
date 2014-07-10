package com.kixeye.chassis.transport.swagger;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedConstructor;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.mangofactory.swagger.models.AccessorsProvider;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Extension of AccessorProvider which considers constructors as accessors;
 *
 * @author dturner@kixeye.com
 */
public class CustomAccessorsProvider extends AccessorsProvider {
    private TypeResolver typeResolver;

    /**
     * Constructor
     *
     * @param typeResolver the typeResolver
     */
    public CustomAccessorsProvider(TypeResolver typeResolver) {
        super(typeResolver);
        this.typeResolver = typeResolver;
    }

    /**
     * Finds the constructors in the given type
     *
     * @param resolvedType the type to search
     */
    public com.google.common.collect.ImmutableList<ResolvedConstructor> constructorsIn(ResolvedType resolvedType) {
        MemberResolver resolver = new MemberResolver(typeResolver);
        resolver.setIncludeLangObject(false);
        if (resolvedType.getErasedType() == Object.class) {
            return ImmutableList.of();
        }
        ResolvedTypeWithMembers typeWithMembers = resolver.resolve(resolvedType, null, null);
        return FluentIterable
                .from(newArrayList(typeWithMembers.getConstructors())).toList();
    }
}
