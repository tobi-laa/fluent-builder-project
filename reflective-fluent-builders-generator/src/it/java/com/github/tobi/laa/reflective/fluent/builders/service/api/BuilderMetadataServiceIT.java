package com.github.tobi.laa.reflective.fluent.builders.service.api;

import com.github.tobi.laa.reflective.fluent.builders.test.models.complex.ClassWithBuilderExisting;
import com.github.tobi.laa.reflective.fluent.builders.test.models.simple.SimpleClass;
import com.google.inject.Guice;
import lombok.SneakyThrows;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BuilderMetadataServiceIT {

    private BuilderMetadataService builderMetadataService;

    @BeforeEach
    @SneakyThrows
    void init() {
        final var classloader = getClass().getClassLoader();
        final var injector = Guice.createInjector(
                new WireModule(
                        new SpaceModule(new URLClassSpace(classloader))));
        builderMetadataService = (BuilderMetadataService) injector.getInstance(Class.forName("com.github.tobi.laa.reflective.fluent.builders.service.impl.BuilderMetadataServiceImpl"));
    }

    @Test
    void testFilterOutConfiguredExcludesWithDefaultConfig() {
        // Act
        final var filteredClasses = builderMetadataService.filterOutConfiguredExcludes(Set.of( //
                SimpleClass.class, //
                ClassWithBuilderExisting.class, //
                ClassWithBuilderExisting.ClassWithBuilderExistingBuilder.class, //
                HasTheSuffixBuilderImpl.class));
        // Assert
        assertThat(filteredClasses).contains(SimpleClass.class, ClassWithBuilderExisting.class);
    }

    static class HasTheSuffixBuilderImpl {
        // no content
    }
}