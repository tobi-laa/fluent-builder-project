package com.github.tobi.laa.fluent.builder.generator.model;

import lombok.Data;

/**
 * <p>
 * Holds metadata about a builder class (as well the class the builder builds).
 * </p>
 */
@lombok.Builder
@Data
public class Builder {

    @lombok.NonNull
    private final String packageName;

    @lombok.NonNull
    private final String name;

    @lombok.NonNull
    private final BuiltType builtType;

    @lombok.Builder
    @Data
    public static class BuiltType {

        @lombok.NonNull
        private final Class<?> type;

        private final boolean accessibleNonArgsConstructor;
    }
}
