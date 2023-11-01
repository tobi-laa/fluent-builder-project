package io.github.tobi.laa.reflective.fluent.builders.model;

import io.github.tobi.laa.reflective.fluent.builders.model.method.MapSetter;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.ClassWithCollections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapSetterTest {

    @ParameterizedTest
    @ValueSource(strings = {"otherName", "yetAnotherName"})
    void testWithParamName(final String paramName) {
        // Arrange
        final var mapSetter = MapSetter.builder() //
                .methodName("getSth") //
                .paramType(Map.class) //
                .paramName("aName") //
                .visibility(Visibility.PRIVATE) //
                .declaringClass(ClassWithCollections.class) //
                .keyType(Object.class) //
                .valueType(Object.class) //
                .build();
        // Act
        final var withParamName = mapSetter.withParamName(paramName);
        // Assert
        assertThat(withParamName).usingRecursiveComparison().isEqualTo(MapSetter.builder() //
                .methodName("getSth") //
                .paramType(Map.class) //
                .paramName(paramName) //
                .visibility(Visibility.PRIVATE) //
                .declaringClass(ClassWithCollections.class) //
                .keyType(Object.class) //
                .valueType(Object.class) //
                .build());
    }
}