package io.github.tobi.laa.reflective.fluent.builders.generator.impl;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants.CallSetterFor;
import io.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants.FieldValue;
import io.github.tobi.laa.reflective.fluent.builders.exception.CodeGenerationException;
import io.github.tobi.laa.reflective.fluent.builders.generator.api.BuilderClassNameGenerator;
import io.github.tobi.laa.reflective.fluent.builders.generator.api.CollectionClassCodeGenerator;
import io.github.tobi.laa.reflective.fluent.builders.generator.api.MapInitializerCodeGenerator;
import io.github.tobi.laa.reflective.fluent.builders.generator.api.TypeNameGenerator;
import io.github.tobi.laa.reflective.fluent.builders.generator.model.CollectionClassSpec;
import io.github.tobi.laa.reflective.fluent.builders.model.BuilderMetadata;
import io.github.tobi.laa.reflective.fluent.builders.model.MapSetter;
import io.github.tobi.laa.reflective.fluent.builders.model.Setter;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Objects;

import static javax.lang.model.element.Modifier.FINAL;
import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * <p>
 * Implementation of {@link CollectionClassCodeGenerator} for generating inner classes for convenient map construction.
 * </p>
 */
@Named
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class InnerClassForMapCodeGenerator implements CollectionClassCodeGenerator {

    @lombok.NonNull
    private final BuilderClassNameGenerator builderClassNameGenerator;

    @lombok.NonNull
    private final TypeNameGenerator typeNameGenerator;

    @lombok.NonNull
    private final List<MapInitializerCodeGenerator> initializerGenerators;

    @Override
    public boolean isApplicable(final Setter setter) {
        Objects.requireNonNull(setter);
        return setter instanceof MapSetter //
                && initializerGenerators.stream().anyMatch(gen -> gen.isApplicable((MapSetter) setter));
    }

    @Override
    public CollectionClassSpec generate(final BuilderMetadata builderMetadata, final Setter setter) {
        Objects.requireNonNull(builderMetadata);
        Objects.requireNonNull(setter);
        if (setter instanceof MapSetter) {
            final MapSetter mapSetter = (MapSetter) setter;
            return generate(builderMetadata, mapSetter);
        } else {
            throw new CodeGenerationException("Generation of inner map class for " + setter + " is not supported.");
        }
    }

    private CollectionClassSpec generate(final BuilderMetadata builderMetadata, final MapSetter setter) {
        final var builderClassName = builderClassNameGenerator.generateClassName(builderMetadata);
        final var className = builderClassName.nestedClass("Map" + capitalize(setter.getPropertyName()));
        return CollectionClassSpec.builder() //
                .getter(MethodSpec //
                        .methodBuilder(setter.getPropertyName()) //
                        .addModifiers(Modifier.PUBLIC) //
                        .returns(className) //
                        .addStatement("return new $T()", className) //
                        .build()) //
                .innerClass(TypeSpec //
                        .classBuilder(className) //
                        .addModifiers(Modifier.PUBLIC) //
                        .addMethod(MethodSpec.methodBuilder("put") //
                                .addModifiers(Modifier.PUBLIC) //
                                .addParameter(typeNameGenerator.generateTypeNameForParam(setter.getKeyType()), "key", FINAL) //
                                .addParameter(typeNameGenerator.generateTypeNameForParam(setter.getValueType()), "value", FINAL) //
                                .returns(className) //
                                .beginControlFlow("if ($T.this.$L.$L == null)", builderClassName, FieldValue.FIELD_NAME, setter.getPropertyName()) //
                                .addStatement(CodeBlock.builder()
                                        .add("$T.this.$L.$L = ", builderClassName, FieldValue.FIELD_NAME, setter.getPropertyName())
                                        .add(initializerGenerators //
                                                .stream() //
                                                .filter(gen -> gen.isApplicable(setter)) //
                                                .map(gen -> gen.generateMapInitializer(setter)) //
                                                .findFirst() //
                                                .orElseThrow(() -> new CodeGenerationException("Could not generate initializer for " + setter + '.'))) //
                                        .build()) //
                                .endControlFlow() //
                                .addStatement("$T.this.$L.$L.put($L, $L)", builderClassName, FieldValue.FIELD_NAME, setter.getPropertyName(), "key", "value") //
                                .addStatement("$T.this.$L.$L = $L", builderClassName, CallSetterFor.FIELD_NAME, setter.getPropertyName(), true) //
                                .addStatement("return this") //
                                .build()) //
                        .addMethod(MethodSpec.methodBuilder("and") //
                                .addModifiers(Modifier.PUBLIC) //
                                .returns(builderClassName) //
                                .addStatement("return $T.this", builderClassName) //
                                .build()) //
                        .build()) //
                .build();
    }
}
