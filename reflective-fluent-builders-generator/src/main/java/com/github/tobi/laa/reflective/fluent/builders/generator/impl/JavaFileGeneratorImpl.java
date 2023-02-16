package com.github.tobi.laa.reflective.fluent.builders.generator.impl;

import com.github.tobi.laa.reflective.fluent.builders.generator.api.*;
import com.github.tobi.laa.reflective.fluent.builders.model.BuilderMetadata;
import com.github.tobi.laa.reflective.fluent.builders.model.Setter;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Generated;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.lang.model.element.Modifier;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

import static com.google.common.collect.ImmutableSortedSet.copyOf;

/**
 * <p>
 * Standard implementation of {@link JavaFileGenerator}.
 * </p>
 */
@Singleton
@Named
class JavaFileGeneratorImpl implements JavaFileGenerator {

    @lombok.NonNull
    private final Clock clock;

    @lombok.NonNull
    private final BuilderClassNameGenerator builderClassNameGenerator;

    @lombok.NonNull
    private final SortedSet<FieldCodeGenerator> fieldCodeGenerators;

    @lombok.NonNull
    private final SortedSet<MethodCodeGenerator> methodCodeGenerators;

    @lombok.NonNull
    private final SortedSet<EncapsulatingClassCodeGenerator> encapsulatingClassCodeGenerators;

    @lombok.NonNull
    private final SortedSet<CollectionClassCodeGenerator> collectionClassCodeGenerators;

    @lombok.NonNull
    private final SetterCodeGenerator setterCodeGenerator;

    @lombok.NonNull
    private final BuildMethodCodeGenerator buildMethodCodeGenerator;

    @Inject
    @SuppressWarnings("unused")
    JavaFileGeneratorImpl( //
                           final Clock clock, //
                           final BuilderClassNameGenerator builderClassNameGenerator, //
                           final Set<FieldCodeGenerator> fieldCodeGenerators, //
                           final Set<MethodCodeGenerator> methodCodeGenerators, //
                           final Set<EncapsulatingClassCodeGenerator> encapsulatingClassCodeGenerators, //
                           final Set<CollectionClassCodeGenerator> collectionClassCodeGenerators, //
                           final SetterCodeGenerator setterCodeGenerator, //
                           final BuildMethodCodeGenerator buildMethodCodeGenerator) {

        this.clock = Objects.requireNonNull(clock);
        this.builderClassNameGenerator = Objects.requireNonNull(builderClassNameGenerator);
        this.setterCodeGenerator = Objects.requireNonNull(setterCodeGenerator);
        this.buildMethodCodeGenerator = Objects.requireNonNull(buildMethodCodeGenerator);
        Objects.requireNonNull(fieldCodeGenerators);
        Objects.requireNonNull(methodCodeGenerators);
        Objects.requireNonNull(encapsulatingClassCodeGenerators);
        Objects.requireNonNull(collectionClassCodeGenerators);
        // to ensure deterministic outputs, sets are sorted on construction
        final var compareByClassName = Comparator.comparing(o -> o.getClass().getName());
        this.fieldCodeGenerators = copyOf(compareByClassName, fieldCodeGenerators);
        this.methodCodeGenerators = copyOf(compareByClassName, methodCodeGenerators);
        this.encapsulatingClassCodeGenerators = copyOf(compareByClassName, encapsulatingClassCodeGenerators);
        this.collectionClassCodeGenerators = copyOf(compareByClassName, collectionClassCodeGenerators);
    }

    @Override
    public JavaFile generateJavaFile(final BuilderMetadata builderMetadata) {
        Objects.requireNonNull(builderMetadata);
        final var builderClassName = builderClassNameGenerator.generateClassName(builderMetadata);
        final var builderTypeSpec = generateModifiersAndAnnotations(builderClassName);
        generateFields(builderMetadata, builderTypeSpec);
        generateConstructorsAndMethods(builderMetadata, builderTypeSpec);
        generateEncapsulatingClasses(builderMetadata, builderTypeSpec);
        generateCollectionClasses(builderMetadata, builderTypeSpec);
        generateSetters(builderMetadata, builderTypeSpec);
        generateBuildMethod(builderMetadata, builderTypeSpec);
        return generateJavaFile(builderClassName, builderTypeSpec);
    }

    private TypeSpec.Builder generateModifiersAndAnnotations(ClassName builderClassName) {
        return TypeSpec.classBuilder(builderClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", getClass().getName())
                        .addMember("date", "$S", ZonedDateTime.now(clock).toString())
                        .build());
    }

    private void generateFields(final BuilderMetadata builderMetadata, final TypeSpec.Builder builderTypeSpec) {
        for (final FieldCodeGenerator generator : fieldCodeGenerators) {
            builderTypeSpec.addField(generator.generate(builderMetadata));
        }
    }

    private void generateConstructorsAndMethods(final BuilderMetadata builderMetadata, final TypeSpec.Builder builderTypeSpec) {
        for (final MethodCodeGenerator generator : methodCodeGenerators) {
            generator.generate(builderMetadata).ifPresent(builderTypeSpec::addMethod);
        }
    }

    private void generateEncapsulatingClasses(final BuilderMetadata builderMetadata, final TypeSpec.Builder builderTypeSpec) {
        for (final EncapsulatingClassCodeGenerator generator : encapsulatingClassCodeGenerators) {
            final var encapsulatingClassSpec = generator.generate(builderMetadata);
            builderTypeSpec.addField(encapsulatingClassSpec.getField());
            builderTypeSpec.addType(encapsulatingClassSpec.getInnerClass());
        }
    }

    private void generateCollectionClasses(final BuilderMetadata builderMetadata, final TypeSpec.Builder builderTypeSpec) {
        for (final CollectionClassCodeGenerator generator : collectionClassCodeGenerators) {
            for (final Setter setter : builderMetadata.getBuiltType().getSetters()) {
                if (generator.isApplicable(setter)) {
                    final var collectionClassSpec = generator.generate(builderMetadata, setter);
                    builderTypeSpec.addMethod(collectionClassSpec.getGetter());
                    builderTypeSpec.addType(collectionClassSpec.getInnerClass());
                }
            }
        }
    }

    private void generateSetters(final BuilderMetadata builderMetadata, final TypeSpec.Builder builderTypeSpec) {
        for (final Setter setter : builderMetadata.getBuiltType().getSetters()) {
            builderTypeSpec.addMethod(setterCodeGenerator.generate(builderMetadata, setter));
        }
    }

    private void generateBuildMethod(final BuilderMetadata builderMetadata, final TypeSpec.Builder builderTypeSpec) {
        builderTypeSpec.addMethod(buildMethodCodeGenerator.generateBuildMethod(builderMetadata));
    }

    private static JavaFile generateJavaFile(final ClassName builderClassName, final TypeSpec.Builder builderTypeSpec) {
        return JavaFile.builder(builderClassName.packageName(), builderTypeSpec.build()).build();
    }
}
