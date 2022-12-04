package com.github.tobi.laa.fluent.builder.maven.plugin.service.impl;

import com.github.tobi.laa.fluent.builder.maven.plugin.model.*;
import com.github.tobi.laa.fluent.builder.maven.plugin.service.api.ClassService;
import com.github.tobi.laa.fluent.builder.maven.plugin.service.api.VisibilityService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tobi.laa.fluent.builder.maven.plugin.model.Visibility.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetterServiceImplTest {

    @Mock
    private VisibilityService visibilityService;

    @Mock
    private ClassService classService;

    @Test
    void testDropSetterPrefixNull() {
        // Arrange
        final var setterService = new SetterServiceImpl(visibilityService, classService, "");
        // Act
        final Executable dropSetterPrefix = () -> setterService.dropSetterPrefix(null);
        // Assert
        assertThrows(NullPointerException.class, dropSetterPrefix);
    }

    @ParameterizedTest
    @MethodSource
    void testDropSetterPrefix(final String setterPrefix, final String name, final String expected) {
        // Arrange
        final var setterService = new SetterServiceImpl(visibilityService, classService, setterPrefix);
        // Act
        final String actual = setterService.dropSetterPrefix(name);
        // Assert
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> testDropSetterPrefix() {
        return Stream.of( //
                Arguments.of("set", "set", "set"), //
                Arguments.of("set", "setAge", "age"), //
                Arguments.of("set", "withAge", "withAge"), //
                Arguments.of("set", "setSetAge", "setAge"));
    }

    @Test
    void gatherAllSettersNull() {
        // Arrange
        final var setterService = new SetterServiceImpl(visibilityService, classService, "");
        // Act
        final Executable gatherAllSetters = () -> setterService.gatherAllSetters(null);
        // Assert
        assertThrows(NullPointerException.class, gatherAllSetters);
    }

    @ParameterizedTest
    @MethodSource
    void gatherAllSetters(final String setterPrefix, final Class<?> clazz, final Visibility mockVisibility, final Set<Setter> expected) {
        // Arrange
        final var setterService = new SetterServiceImpl(visibilityService, classService, setterPrefix);
        when(visibilityService.toVisibility(anyInt())).thenReturn(mockVisibility);
        when(classService.collectFullClassHierarchy(clazz)).thenReturn(Set.of(clazz));
        // Act
        final Set<Setter> actual = setterService.gatherAllSetters(clazz);
        // Assert
        assertThat(actual)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withEqualsForType((a, b) -> true, WildcardType.class)
                        .withEqualsForType((a, b) -> a.getTypeName().equals(b.getTypeName()), TypeVariable.class)
                        .build())
                .isEqualTo(expected);
        verify(visibilityService, times(expected.size())).toVisibility(anyInt());
    }

    @Test
    void gatherAllSettersForClassWithHierarchy() {
        // Arrange
        final var setterService = new SetterServiceImpl(visibilityService, classService, "set");
        when(visibilityService.toVisibility(anyInt())).thenReturn(PROTECTED);
        when(classService.collectFullClassHierarchy(any())).thenReturn(Set.of(ClassWithHierarchy.class, FirstSuperClass.class, TopLevelSuperClass.class, AnInterface.class, AnotherInterface.class));
        // Act
        final Set<Setter> actual = setterService.gatherAllSetters(ClassWithHierarchy.class);
        // Assert
        assertThat(actual)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withEqualsForType((a, b) -> true, WildcardType.class)
                        .withEqualsForType((a, b) -> a.getTypeName().equals(b.getTypeName()), TypeVariable.class)
                        .build())
                .isEqualTo(Stream.of("setOne", "setTwo", "setThree", "setFour", "setFive") //
                        .map(name -> SimpleSetter.builder().methodName(name).paramName(StringUtils.uncapitalize(name.substring(3))).paramType(int.class).visibility(PROTECTED).build()) //
                        .collect(Collectors.toSet()));
        verify(visibilityService, times(5)).toVisibility(anyInt());
    }

    private static Stream<Arguments> gatherAllSetters() {
        return Stream.of( //
                Arguments.of("set", SimpleClass.class, PUBLIC, //
                        Set.of( //
                                SimpleSetter.builder().methodName("setAnInt").paramName("anInt").paramType(int.class).visibility(PUBLIC).build(), //
                                SimpleSetter.builder().methodName("setAString").paramName("aString").paramType(String.class).visibility(PUBLIC).build(), //
                                SimpleSetter.builder().methodName("setBooleanField").paramName("booleanField").paramType(boolean.class).visibility(PUBLIC).build(), //
                                SimpleSetter.builder().methodName("setSetClass").paramName("setClass").paramType(Class.class).visibility(PUBLIC).build())), //
                Arguments.of("", SimpleClassNoSetPrefix.class, PACKAGE_PRIVATE, //
                        Set.of( //
                                SimpleSetter.builder().methodName("anInt").paramName("anInt").paramType(int.class).visibility(PACKAGE_PRIVATE).build(), //
                                SimpleSetter.builder().methodName("aString").paramName("aString").paramType(String.class).visibility(PACKAGE_PRIVATE).build())), //
                Arguments.of("set", ClassWithCollections.class, PRIVATE, //
                        Set.of( //
                                CollectionSetter.builder().methodName("setInts").paramName("ints").paramType(Collection.class).paramTypeArg(Integer.class).visibility(PRIVATE).build(), //
                                CollectionSetter.builder().methodName("setList").paramName("list").paramType(List.class).paramTypeArg(Object.class).visibility(PRIVATE).build(),
                                CollectionSetter.builder().methodName("setSet").paramName("set").paramType(Set.class).paramTypeArg(List.class).visibility(PRIVATE).build(),
                                CollectionSetter.builder().methodName("setDeque").paramName("deque").paramType(Deque.class).paramTypeArg(TypeUtils.wildcardType().build()).visibility(PRIVATE).build(),
                                ArraySetter.builder().methodName("setFloats").paramName("floats").paramType(float[].class).paramComponentType(float.class).visibility(PRIVATE).build(),
                                MapSetter.builder().methodName("setMap").paramName("map").paramType(Map.class).keyType(String.class).valueType(Object.class).visibility(PRIVATE).build(),
                                MapSetter.builder().methodName("setMapWildT").paramName("mapWildT").paramType(Map.class).keyType(TypeUtils.wildcardType().build()).valueType(typeVariableT()).visibility(PRIVATE).build(),
                                MapSetter.builder().methodName("setMapNoTypeArgs").paramName("mapNoTypeArgs").paramType(Map.class).keyType(Object.class).valueType(Object.class).visibility(PRIVATE).build())));
    }

    private static TypeVariable<?> typeVariableT() {
        return ClassWithCollections.class.getTypeParameters()[0];
    }

    @SuppressWarnings("unused")
    @lombok.Setter
    static class SimpleClass {
        int anInt;
        String aString;
        boolean booleanField;
        Class<?> setClass;

        void anInt(final int anInt) {
            this.anInt = anInt;
        }

        void aString(final String aString) {
            this.aString = aString;
        }
    }

    @SuppressWarnings("unused")
    static class SimpleClassNoSetPrefix {
        int anInt;
        String aString;

        void anInt(final int anInt) {
            this.anInt = anInt;
        }

        void aString(final String aString) {
            this.aString = aString;
        }
    }

    @SuppressWarnings("rawtypes")
    @lombok.Setter
    static class ClassWithCollections<T> {
        Collection<Integer> ints;
        List list;
        java.util.Set<List> set;
        Deque<?> deque;
        float[] floats;
        Map<String, Object> map;
        Map<?, T> mapWildT;
        Map mapNoTypeArgs;
    }

    @lombok.Setter
    static class ClassWithHierarchy extends FirstSuperClass implements AnInterface {
        int one;
    }

    @lombok.Setter
    static class FirstSuperClass extends TopLevelSuperClass {
        int two;
    }

    static abstract class TopLevelSuperClass implements AnotherInterface {
        @lombok.Setter
        int three;
    }

    @SuppressWarnings("unused")
    interface AnInterface {
        default void setFour(final int four) {
        }
    }

    @SuppressWarnings("unused")
    interface AnotherInterface {
        default void setFive(final int five) {
        }
    }
}