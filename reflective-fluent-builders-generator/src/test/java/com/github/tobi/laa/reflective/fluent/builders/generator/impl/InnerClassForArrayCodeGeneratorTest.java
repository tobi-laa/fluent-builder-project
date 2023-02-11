package com.github.tobi.laa.reflective.fluent.builders.generator.impl;

import com.github.tobi.laa.reflective.fluent.builders.exception.CodeGenerationException;
import com.github.tobi.laa.reflective.fluent.builders.generator.api.BuilderClassNameGenerator;
import com.github.tobi.laa.reflective.fluent.builders.generator.model.CollectionClassSpec;
import com.github.tobi.laa.reflective.fluent.builders.model.*;
import com.github.tobi.laa.reflective.fluent.builders.service.api.SetterService;
import com.github.tobi.laa.reflective.fluent.builders.test.models.simple.SimpleClass;
import com.squareup.javapoet.ClassName;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Deque;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InnerClassForArrayCodeGeneratorTest {

    @InjectMocks
    private InnerClassForArrayCodeGenerator generator;

    @Mock
    private BuilderClassNameGenerator builderClassNameGenerator;

    @Mock
    private SetterService setterService;

    @Test
    void testIsApplicableNull() {
        // Arrange
        final Setter setter = null;
        // Act
        final Executable isApplicable = () -> generator.isApplicable(setter);
        // Assert
        assertThrows(NullPointerException.class, isApplicable);
        verifyNoInteractions(builderClassNameGenerator, setterService);
    }

    @ParameterizedTest
    @MethodSource
    void testIsApplicableTrue(final Setter setter) {
        // Act
        final boolean actual = generator.isApplicable(setter);
        // Assert
        assertTrue(actual);
    }

    private static Stream<Setter> testIsApplicableTrue() {
        return Stream.of( //
                ArraySetter.builder() //
                        .methodName("setFloats") //
                        .paramName("floats") //
                        .paramType(float[].class) //
                        .paramComponentType(float.class) //
                        .visibility(Visibility.PRIVATE) //
                        .build(), //
                ArraySetter.builder() //
                        .methodName("setStrings") //
                        .paramName("strings") //
                        .paramType(String[].class) //
                        .paramComponentType(String.class) //
                        .visibility(Visibility.PRIVATE) //
                        .build());
    }

    @ParameterizedTest
    @MethodSource
    void testIsApplicableFalse(final Setter setter) {
        // Act
        final boolean actual = generator.isApplicable(setter);
        // Assert
        assertFalse(actual);
    }

    private static Stream<Setter> testIsApplicableFalse() {
        return testGenerateCodeGenerationException().map(args -> args.get()[1]).map(Setter.class::cast);
    }

    @ParameterizedTest
    @MethodSource
    void testGenerateCodeNull(final BuilderMetadata builderMetadata, final Setter setter) {
        // Act
        final Executable generate = () -> generator.generate(builderMetadata, setter);
        // Assert
        assertThrows(NullPointerException.class, generate);
        verifyNoInteractions(builderClassNameGenerator, setterService);
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
                        ArraySetter.builder() //
                                .methodName("setFloats") //
                                .paramName("floats") //
                                .paramType(float[].class) //
                                .paramComponentType(float.class) //
                                .visibility(Visibility.PRIVATE) //
                                .build()));
    }

    @ParameterizedTest
    @MethodSource
    void testGenerateCodeGenerationException(final BuilderMetadata builderMetadata, final Setter setter) {
        // Act
        final ThrowingCallable generate = () -> generator.generate(builderMetadata, setter);
        // Assert
        assertThatThrownBy(generate) //
                .isInstanceOf(CodeGenerationException.class) //
                .hasMessageMatching("Generation of inner array class for .+ is not supported.") //
                .hasMessageContaining(setter.getParamType().toString());
        verifyNoInteractions(builderClassNameGenerator, setterService);
    }

    private static Stream<Arguments> testGenerateCodeGenerationException() {
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
                        CollectionSetter.builder() //
                                .methodName("setDeque") //
                                .paramName("deque") //
                                .paramType(Deque.class) //
                                .paramTypeArg(TypeUtils.wildcardType().build()) //
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
                        MapSetter.builder() //
                                .methodName("setMap") //
                                .paramName("map") //
                                .paramType(Map.class) //
                                .keyType(String.class) //
                                .valueType(Object.class) //
                                .visibility(Visibility.PRIVATE) //
                                .build()));
    }

    @ParameterizedTest
    @MethodSource
    void testGenerate(final BuilderMetadata builderMetadata, final Setter setter, final String expectedGetter, final String expectedInnerClass) {
        // Arrange
        when(builderClassNameGenerator.generateClassName(any())).thenReturn(ClassName.get(MockType.class));
        when(setterService.dropSetterPrefix(any())).thenReturn(setter.getParamName());
        // Act
        final CollectionClassSpec actual = generator.generate(builderMetadata, setter);
        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.getGetter().toString()).isEqualToNormalizingNewlines(expectedGetter);
        assertThat(actual.getInnerClass().toString()).isEqualToNormalizingNewlines(expectedInnerClass);
        verify(builderClassNameGenerator).generateClassName(builderMetadata);
        verify(setterService).dropSetterPrefix(setter.getMethodName());
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
                        ArraySetter.builder() //
                                .methodName("setFloats") //
                                .paramName("floats") //
                                .paramType(float[].class) //
                                .paramComponentType(float.class) //
                                .visibility(Visibility.PRIVATE) //
                                .build(), //
                        """
                                public %1$s.ArrayFloats floats(
                                    ) {
                                  return new %1$s.ArrayFloats();
                                }
                                """.formatted(mockTypeName), //
                        """
                                public class ArrayFloats {
                                  private java.util.List<java.lang.Float> list;
                                                                
                                  public %1$s.ArrayFloats add(
                                      final float item) {
                                    if (this.list == null) {
                                      this.list = new java.util.ArrayList<>();
                                    }
                                    this.list.add(item);
                                    %1$s.this.callSetterFor.floats = true;
                                    return this;
                                  }
                                                                
                                  public %1$s and(
                                      ) {
                                    if (this.list != null) {
                                      %1$s.this.fieldValue.floats = list.toArray(new float[0]);
                                    }
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
                        ArraySetter.builder() //
                                .methodName("setStrings") //
                                .paramName("strings") //
                                .paramType(String[].class) //
                                .paramComponentType(String.class) //
                                .visibility(Visibility.PRIVATE) //
                                .build(), //
                        """
                                public %1$s.ArrayStrings strings(
                                    ) {
                                  return new %1$s.ArrayStrings();
                                }
                                """.formatted(mockTypeName),
                        """
                                public class ArrayStrings {
                                  private java.util.List<java.lang.String> list;
                                                                
                                  public %1$s.ArrayStrings add(
                                      final java.lang.String item) {
                                    if (this.list == null) {
                                      this.list = new java.util.ArrayList<>();
                                    }
                                    this.list.add(item);
                                    %1$s.this.callSetterFor.strings = true;
                                    return this;
                                  }
                                                                
                                  public %1$s and(
                                      ) {
                                    if (this.list != null) {
                                      %1$s.this.fieldValue.strings = list.toArray(new java.lang.String[0]);
                                    }
                                    return %1$s.this;
                                  }
                                }
                                """.formatted(mockTypeName)));
    }

    private static class MockType {
        // no content
    }
}