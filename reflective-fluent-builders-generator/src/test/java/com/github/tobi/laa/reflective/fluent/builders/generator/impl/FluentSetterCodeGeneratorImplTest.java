package com.github.tobi.laa.reflective.fluent.builders.generator.impl;

import com.github.tobi.laa.reflective.fluent.builders.generator.api.BuilderClassNameGenerator;
import com.github.tobi.laa.reflective.fluent.builders.generator.api.SetterTypeNameGenerator;
import com.github.tobi.laa.reflective.fluent.builders.model.*;
import com.github.tobi.laa.reflective.fluent.builders.service.api.SetterService;
import com.github.tobi.laa.reflective.fluent.builders.test.models.simple.SimpleClass;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Deque;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FluentSetterCodeGeneratorImplTest {

    @InjectMocks
    private FluentSetterCodeGeneratorImpl generator;

    @Mock
    private BuilderClassNameGenerator builderClassNameGenerator;

    @Mock
    private SetterTypeNameGenerator setterTypeNameGenerator;

    @Mock
    private SetterService setterService;

    @ParameterizedTest
    @MethodSource
    void testGenerateFluentSetterNull(final BuilderMetadata builderMetadata, final Setter setter) {
        // Act
        final Executable generateFluentSetter = () -> generator.generateFluentSetter(builderMetadata, setter);
        // Assert
        assertThrows(NullPointerException.class, generateFluentSetter);
        verifyNoInteractions(builderClassNameGenerator, setterTypeNameGenerator, setterService);
    }

    private static Stream<Arguments> testGenerateFluentSetterNull() {
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
                        CollectionSetter.builder() //
                                .methodName("setDeque") //
                                .paramName("deque") //
                                .paramType(Deque.class) //
                                .paramTypeArg(TypeUtils.wildcardType().build()) //
                                .visibility(Visibility.PRIVATE) //
                                .build()));
    }

    @ParameterizedTest
    @MethodSource
    void testGenerateFluentSetter(final BuilderMetadata builderMetadata, final Setter setter, final String expected) {
        // Arrange
        when(builderClassNameGenerator.generateClassName(any())).thenReturn(ClassName.get(MockType.class));
        when(setterTypeNameGenerator.generateTypeNameForParam(any())).thenReturn(TypeName.get(MockType.class));
        when(setterService.dropSetterPrefix(any())).thenReturn(setter.getParamName());
        // Act
        final MethodSpec actual = generator.generateFluentSetter(builderMetadata, setter);
        // Assert
        assertThat(actual).hasToString(expected);
        verify(builderClassNameGenerator).generateClassName(builderMetadata);
        verify(setterTypeNameGenerator).generateTypeNameForParam(setter);
        verify(setterService).dropSetterPrefix(setter.getMethodName());
    }

    private static Stream<Arguments> testGenerateFluentSetter() {
        final var mockTypeName = MockType.class.getName().replace('$', '.');
        final var builderMetadata = BuilderMetadata.builder() //
                .packageName("ignored") //
                .name("Ignored") //
                .builtType(BuilderMetadata.BuiltType.builder() //
                        .type(SimpleClass.class) //
                        .accessibleNonArgsConstructor(true) //
                        .build()) //
                .build();
        return Stream.of( //
                Arguments.of( //
                        builderMetadata, //
                        SimpleSetter.builder() //
                                .methodName("setAnInt") //
                                .paramName("anInt") //
                                .paramType(int.class) //
                                .visibility(Visibility.PUBLIC) //
                                .build(), //
                        """
                                public %1$s anInt(
                                    final %1$s anInt) {
                                  fieldValue.anInt = anInt;
                                  callSetterFor.anInt = true;
                                  return this;
                                }
                                """.formatted(mockTypeName)), //
                Arguments.of( //
                        builderMetadata, //
                        ArraySetter.builder() //
                                .methodName("setFloats") //
                                .paramName("floats") //
                                .paramType(float[].class) //
                                .paramComponentType(float.class) //
                                .visibility(Visibility.PRIVATE) //
                                .build(), //
                        """
                                public %1$s floats(
                                    final %1$s floats) {
                                  fieldValue.floats = floats;
                                  callSetterFor.floats = true;
                                  return this;
                                }
                                """.formatted(mockTypeName)), //
                Arguments.of( //
                        builderMetadata, //
                        MapSetter.builder() //
                                .methodName("setSortedMap") //
                                .paramName("sortedMap") //
                                .paramType(SortedMap.class) //
                                .keyType(Integer.class) //
                                .valueType(Object.class) //
                                .visibility(Visibility.PRIVATE) //
                                .build(), //
                        """
                                public %1$s sortedMap(
                                    final %1$s sortedMap) {
                                  fieldValue.sortedMap = sortedMap;
                                  callSetterFor.sortedMap = true;
                                  return this;
                                }
                                """.formatted(mockTypeName)), //
                Arguments.of( //
                        builderMetadata, //
                        CollectionSetter.builder() //
                                .methodName("setList") //
                                .paramName("list") //
                                .paramType(List.class) //
                                .paramTypeArg(String.class) //
                                .visibility(Visibility.PRIVATE) //
                                .build(), //
                        """
                                public %1$s list(
                                    final %1$s list) {
                                  fieldValue.list = list;
                                  callSetterFor.list = true;
                                  return this;
                                }
                                """.formatted(mockTypeName)));
    }

    private static class MockType {
        // no content
    }
}