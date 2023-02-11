package com.github.tobi.laa.reflective.fluent.builders.service.impl;

import com.github.tobi.laa.reflective.fluent.builders.model.BuilderMetadata;
import com.github.tobi.laa.reflective.fluent.builders.model.Setter;
import com.github.tobi.laa.reflective.fluent.builders.model.Visibility;
import com.github.tobi.laa.reflective.fluent.builders.service.api.BuilderMetadataService;
import com.github.tobi.laa.reflective.fluent.builders.service.api.SetterService;
import com.github.tobi.laa.reflective.fluent.builders.service.api.VisibilityService;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants.PACKAGE_PLACEHOLDER;
import static com.github.tobi.laa.reflective.fluent.builders.model.Visibility.PACKAGE_PRIVATE;
import static com.github.tobi.laa.reflective.fluent.builders.model.Visibility.PUBLIC;
import static java.util.function.Predicate.not;

/**
 * <p>
 * Standard implementation of {@link BuilderMetadataService}.
 * </p>
 */
@RequiredArgsConstructor
public class BuilderMetadataServiceImpl implements BuilderMetadataService {

    @lombok.NonNull
    private final VisibilityService visibilityService;

    @lombok.NonNull
    private final SetterService setterService;

    @lombok.NonNull
    private final String builderPackage;

    @lombok.NonNull
    private final String builderSuffix;

    @Override
    public BuilderMetadata collectBuilderMetadata(final Class<?> clazz) {
        Objects.requireNonNull(clazz);
        return BuilderMetadata.builder() //
                .packageName(resolveBuilderPackage(clazz)) //
                .name(clazz.getSimpleName() + builderSuffix) //
                .builtType(BuilderMetadata.BuiltType.builder() //
                        .type(clazz) //
                        .accessibleNonArgsConstructor(hasAccessibleNonArgsConstructor(clazz)) //
                        .setters(gatherAndFilterAccessibleSettersAndAvoidNameCollisions(clazz))
                        .build()) //
                .build();
    }

    private String resolveBuilderPackage(final Class<?> clazz) {
        return builderPackage.replace(PACKAGE_PLACEHOLDER, clazz.getPackageName());
    }

    private boolean hasAccessibleNonArgsConstructor(final Class<?> clazz) {
        return Arrays //
                .stream(clazz.getConstructors()) //
                .filter(this::isAccessible) //
                .mapToInt(Constructor::getParameterCount) //
                .anyMatch(count -> count == 0);
    }

    private boolean isAccessible(final Constructor<?> constructor) {
        return isAccessible(constructor.getDeclaringClass(), constructor.getModifiers());
    }

    private SortedSet<Setter> gatherAndFilterAccessibleSettersAndAvoidNameCollisions(final Class<?> clazz) {
        final var setters = setterService.gatherAllSetters(clazz) //
                .stream() //
                .filter(setter -> isAccessible(clazz, setter.getVisibility()))
                .collect(Collectors.toUnmodifiableSet());
        return avoidNameCollisions(setters);
    }

    private SortedSet<Setter> avoidNameCollisions(final Set<Setter> setters) {
        final SortedSet<Setter> noNameCollisions = new TreeSet<>();
        for (final var setter : setters) {
            if (noNameCollisions.stream().map(Setter::getParamName).noneMatch(setter.getParamName()::equals)) {
                noNameCollisions.add(setter);
            } else {
                for (int i = 0; true; i++) {
                    final var paramName = setter.getParamName() + i;
                    if (noNameCollisions.stream().map(Setter::getParamName).noneMatch(paramName::equals)) {
                        noNameCollisions.add(setter.withParamName(paramName));
                        break;
                    }
                }
            }
        }
        return noNameCollisions;
    }

    @Override
    public Set<Class<?>> filterOutNonBuildableClasses(final Set<Class<?>> classes) {
        Objects.requireNonNull(classes);
        return classes //
                .stream() //
                .filter(not(Class::isInterface)) //
                .filter(not(this::isAbstract)) //
                .filter(not(Class::isAnonymousClass)) //
                .filter(not(Class::isEnum)) //
                .filter(not(Class::isPrimitive)) //
                .filter(not(Class::isMemberClass)) //
                .filter(this::isAccessible) //
                .collect(Collectors.toSet());
    }

    private boolean isAbstract(final Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    private boolean isAccessible(final Class<?> clazz) {
        return isAccessible(clazz, clazz.getModifiers());
    }

    private boolean isAccessible(final Class<?> clazz, final int modifiers) {
        final var visibility = visibilityService.toVisibility(modifiers);
        return isAccessible(clazz, visibility);
    }

    private boolean isAccessible(final Class<?> clazz, final Visibility visibility) {
        return visibility == PUBLIC || visibility == PACKAGE_PRIVATE && placeBuildersInSamePackage(clazz);
    }

    private boolean placeBuildersInSamePackage(final Class<?> clazz) {
        return PACKAGE_PLACEHOLDER.equals(builderPackage) || builderPackage.equals(clazz.getPackageName());
    }
}
