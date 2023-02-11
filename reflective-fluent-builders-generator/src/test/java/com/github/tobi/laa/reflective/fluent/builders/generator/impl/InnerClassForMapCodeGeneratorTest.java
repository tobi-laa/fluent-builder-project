package com.github.tobi.laa.reflective.fluent.builders.generator.impl;

import com.github.tobi.laa.reflective.fluent.builders.exception.CodeGenerationException;
import com.github.tobi.laa.reflective.fluent.builders.generator.api.BuilderClassNameGenerator;
import com.github.tobi.laa.reflective.fluent.builders.generator.api.MapInitializerCodeGenerator;
import com.github.tobi.laa.reflective.fluent.builders.generator.model.CollectionClassSpec;
import com.github.tobi.laa.reflective.fluent.builders.model.*;
import com.github.tobi.laa.reflective.fluent.builders.service.api.SetterService;
import com.github.tobi.laa.reflective.fluent.builders.test.models.simple.SimpleClass;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.cartesian.ArgumentSets;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InnerClassForMapCodeGeneratorTest {

    private InnerClassForMapCodeGenerator generator;

    @Mock
    private BuilderClassNameGenerator builderClassNameGenerator;

    @Mock
    private SetterService setterService;

    @Mock
    private MapInitializerCodeGenerator initializerGeneratorA;

    @Mock
    private MapInitializerCodeGenerator initializerGeneratorB;

    @BeforeEach
    void init() {
        generator = new InnerClassForMapCodeGenerator(builderClassNameGenerator, setterService, List.of(initializerGeneratorA, initializerGeneratorB));
    }

    @Test
    void testIsApplicableNull() {
        // Arrange
        final Setter setter = null;
        // Act
        final Executable isApplicable = () -> generator.isApplicable(setter);
        // Assert
        assertThrows(NullPointerException.class, isApplicable);
        verifyNoInteractions(builderClassNameGenerator, setterService, initializerGeneratorA, initializerGeneratorB);
    }

    @CartesianTest
    @CartesianTest.MethodFactory("testIsApplicableTrue")
    void testIsApplicableTrue(final Setter setter, final boolean genAApplicable) {
        // Arrange
        if (genAApplicable) {
            when(initializerGeneratorA.isApplicable(any())).thenReturn(true);
        } else {
            when(initializerGeneratorB.isApplicable(any())).thenReturn(true);
        }
        // Act
        final boolean actual = generator.isApplicable(setter);
        // Assert
        assertTrue(actual);
    }

    static ArgumentSets testIsApplicableTrue() {
        return ArgumentSets
                .argumentsForFirstParameter(testIsApplicableFalseNoInitializerGeneratorApplicable()) //
                .argumentsForNextParameter(true, false);
    }

    @ParameterizedTest
    @MethodSource
    void testIsApplicableFalseWrongType(final Setter setter) {
        // Act
        final boolean actual = generator.isApplicable(setter);
        // Assert
        assertFalse(actual);
        verifyNoInteractions(initializerGeneratorA, initializerGeneratorB);
    }

    private static Stream<Setter> testIsApplicableFalseWrongType() {
        return testGenerateCodeGenerationExceptionWrongType().map(args -> args.get()[1]).map(Setter.class::cast);
    }

    @ParameterizedTest
    @MethodSource
    void testIsApplicableFalseNoInitializerGeneratorApplicable(final Setter setter) {
        // Act
        final boolean actual = generator.isApplicable(setter);
        // Assert
        assertFalse(actual);
    }

    private static Stream<Setter> testIsApplicableFalseNoInitializerGeneratorApplicable() {
        return Stream.of( //
                MapSetter.builder() //
                        .methodName("setMap") //
                        .paramName("map") //
                        .paramType(Map.class) //
                        .keyType(String.class)
                        .valueType(TypeUtils.wildcardType().withUpperBounds(Object.class).build()) //
                        .visibility(Visibility.PRIVATE) //
                        .build(), //
                MapSetter.builder() //
                        .methodName("setSortedMap") //
                        .paramName("sortedMap") //
                        .paramType(SortedMap.class) //
                        .keyType(Integer.class) //
                        .valueType(Object.class) //
                        .visibility(Visibility.PRIVATE) //
                        .build());
    }

    @ParameterizedTest
    @MethodSource
    void testGenerateCodeNull(final BuilderMetadata builderMetadata, final Setter setter) {
        // Act
        final Executable generate = () -> generator.generate(builderMetadata, setter);
        // Assert
        assertThrows(NullPointerException.class, generate);
        verifyNoInteractions(builderClassNameGenerator, setterService, initializerGeneratorA, initializerGeneratorB);
    }

    private static Stream<Arguments> testGenerateCodeNull() {
        return Stream.of( //
                Arguments.of(null, null),
                Arguments.of( //
                        BuilderMetadata.builder() //
                                .packageName("ignored") //
                                .name("Ignored") //
                                .builtType(BuilderMetadata.BuiltType.builder() //
                                        .type(SimpleClass.class) //
                                        .accessibleNonArgsConstructor(true) //
                                        .build()) //
                                .build(), //
                        null), //
                Arguments.of( //
                        null, //
                        MapSetter.builder() //
                                .methodName("setMap") //
                                .paramName("map") //
                                .paramType(Map.class) //
                                .keyType(String.class)
                                .valueType(TypeUtils.wildcardType().withUpperBounds(Object.class).build()) //
                                .visibility(Visibility.PRIVATE) //
                                .build()));
    }

    @ParameterizedTest
    @MethodSource
    void testGenerateCodeGenerationExceptionWrongType(final BuilderMetadata builderMetadata, final Setter setter) {
        // Act
        final ThrowingCallable generate = () -> generator.generate(builderMetadata, setter);
        // Assert
        assertThatThrownBy(generate) //
                .isInstanceOf(CodeGenerationException.class) //
                .hasMessageMatching("Generation of inner map class for .+ is not supported.") //
                .hasMessageContaining(setter.getParamType().toString());
        verifyNoInteractions(builderClassNameGenerator, setterService, initializerGeneratorB, initializerGeneratorB);
    }

    private static Stream<Arguments> testGenerateCodeGenerationExceptionWrongType() {
        return Stream.of( //
                Arguments.of( //
                        BuilderMetadata.builder() //
                                .packageName("ignored") //
                                .name("Ignored") //
                                .builtType(BuilderMetadata.BuiltType.builder() //
                                        .type(SimpleClass.class) //
                                        .accessibleNonArgsConstructor(true) //
                                        .build()) //
                                .build(), //
                        SimpleSetter.builder() //
                                .methodName("setAnInt") //
                                .paramName("anInt") //
                                .paramType(int.class) //
                                .visibility(Visibility.PUBLIC) //
                                .build()), //
                Arguments.of( //
                        BuilderMetadata.builder() //
                                .packageName("ignored") //
                                .name("Ignored") //
                                .builtType(BuilderMetadata.BuiltType.builder() //
                                        .type(SimpleClass.class) //
                                        .accessibleNonArgsConstructor(true) //
                                        .build()) //
                                .build(), //
                        ArraySetter.builder() //
                                .methodName("setFloats") //
                                .paramName("floats") //
                                .paramType(float[].class) //
                                .paramComponentType(float.class) //
                                .visibility(Visibility.PRIVATE) //
                                .build()), //
                Arguments.of( //
                        BuilderMetadata.builder() //
                                .packageName("ignored") //
                                .name("Ignored") //
                                .builtType(BuilderMetadata.BuiltType.builder() //
                                        .type(SimpleClass.class) //
                                        .accessibleNonArgsConstructor(true) //
                                        .build()) //
                                .build(), //
                        CollectionSetter.builder() //
                                .methodName("setDeque") //
                                .paramName("deque") //
                                .paramType(Deque.class) //
                                .paramTypeArg(TypeUtils.wildcardType().withUpperBounds(Object.class).build()) //
                                .visibility(Visibility.PRIVATE) //
                                .build()));
    }

    @ParameterizedTest
    @MethodSource
    void testGenerateCodeGenerationExceptionNoInitializerGeneratorApplicable(final BuilderMetadata builderMetadata, final Setter setter) {
        // Arrange
        when(builderClassNameGenerator.generateClassName(any())).thenReturn(ClassName.get(MockType.class));
        when(setterService.dropSetterPrefix(any())).thenReturn(setter.getParamName());
        // Act
        final ThrowingCallable generate = () -> generator.generate(builderMetadata, setter);
        // Assert
        assertThatThrownBy(generate) //
                .isInstanceOf(CodeGenerationException.class) //
                .hasMessageMatching("Could not generate initializer for .+") //
                .hasMessageContaining(setter.getParamType().toString());
    }

    private static Stream<Arguments> testGenerateCodeGenerationExceptionNoInitializerGeneratorApplicable() {
        return testIsApplicableFalseNoInitializerGeneratorApplicable() //
                .map(setter -> Arguments.of( //
                        BuilderMetadata.builder() //
                                .packageName("ignored") //
                                .name("Ignored") //
                                .builtType(BuilderMetadata.BuiltType.builder() //
                                        .type(SimpleClass.class) //
                                        .accessibleNonArgsConstructor(true) //
                                        .build()) //
                                .build(), //
                        setter));
    }

    @ParameterizedTest
    @MethodSource
    void testGenerate(final BuilderMetadata builderMetadata, final MapSetter setter, final String expectedGetter, final String expectedInnerClass) {
        // Arrange
        when(builderClassNameGenerator.generateClassName(any())).thenReturn(ClassName.get(MockType.class));
        when(setterService.dropSetterPrefix(any())).thenReturn(setter.getParamName());
        when(initializerGeneratorA.isApplicable(any())).thenReturn(true);
        when(initializerGeneratorA.generateMapInitializer(any())).thenReturn(CodeBlock.of("new MockMap<>()"));
        // Act
        final CollectionClassSpec actual = generator.generate(builderMetadata, setter);
        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.getGetter().toString()).isEqualToNormalizingNewlines(expectedGetter);
        assertThat(actual.getInnerClass().toString()).isEqualToNormalizingNewlines(expectedInnerClass);
        verify(builderClassNameGenerator).generateClassName(builderMetadata);
        verify(setterService).dropSetterPrefix(setter.getMethodName());
        verify(initializerGeneratorA).generateMapInitializer(setter);
    }

    private static Stream<Arguments> testGenerate() {
        final var mockTypeName = MockType.class.getName().replace('$', '.');
        return Stream.of( //
                Arguments.of( //
                        BuilderMetadata.builder() //
                                .packageName("ignored") //
                                .name("Ignored") //
                                .builtType(BuilderMetadata.BuiltType.builder() //
                                        .type(SimpleClass.class) //
                                        .accessibleNonArgsConstructor(true) //
                                        .build()) //
                                .build(), //
                        MapSetter.builder() //
                                .methodName("setMap") //
                                .paramName("map") //
                                .paramType(Map.class) //
                                .keyType(String.class)
                                .valueType(TypeUtils.wildcardType().withUpperBounds(Object.class).build()) //
                                .visibility(Visibility.PRIVATE) //
                                .build(), //
                        """
                                public %1$s.MapMap map(
                                    ) {
                                  return new %1$s.MapMap();
                                }
                                """.formatted(mockTypeName), //
                        """
                                public class MapMap {
                                  public %1$s.MapMap put(
                                      final java.lang.String key, final ? value) {
                                    if (%1$s.this.fieldValue.map == null) {
                                      %1$s.this.fieldValue.map = new MockMap<>();
                                    }
                                    %1$s.this.fieldValue.map.put(key, value);
                                    %1$s.this.callSetterFor.map = true;
                                    return this;
                                  }
                                                                
                                  public %1$s and(
                                      ) {
                                    return %1$s.this;
                                  }
                                }
                                """.formatted(mockTypeName)), //
                Arguments.of( //
                        BuilderMetadata.builder() //
                                .packageName("ignored") //
                                .name("Ignored") //
                                .builtType(BuilderMetadata.BuiltType.builder() //
                                        .type(SimpleClass.class) //
                                        .accessibleNonArgsConstructor(true) //
                                        .build()) //
                                .build(), //
                        MapSetter.builder() //
                                .methodName("setSortedMap") //
                                .paramName("sortedMap") //
                                .paramType(SortedMap.class) //
                                .keyType(Integer.class) //
                                .valueType(Object.class) //
                                .visibility(Visibility.PRIVATE) //
                                .build(), //
                        """
                                public %1$s.MapSortedMap sortedMap(
                                    ) {
                                  return new %1$s.MapSortedMap();
                                }
                                """.formatted(mockTypeName),
                        """
                                public class MapSortedMap {
                                  public %1$s.MapSortedMap put(
                                      final java.lang.Integer key, final java.lang.Object value) {
                                    if (%1$s.this.fieldValue.sortedMap == null) {
                                      %1$s.this.fieldValue.sortedMap = new MockMap<>();
                                    }
                                    %1$s.this.fieldValue.sortedMap.put(key, value);
                                    %1$s.this.callSetterFor.sortedMap = true;
                                    return this;
                                  }
                                                                
                                  public %1$s and(
                                      ) {
                                    return %1$s.this;
                                  }
                                }
                                """.formatted(mockTypeName)));
    }

    private static class MockType {
        // no content
    }
}