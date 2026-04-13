package com.dni.apitest.utils;

import java.util.UUID;

/**
 * General-purpose test utilities shared across the suite.
 */
public final class TestUtils {

    private TestUtils() {}

    /**
     * Returns a project name guaranteed to be unique within the current test run by
     * appending a random UUID. Use this in tests that need a fresh name on every execution
     * (e.g. duplicate-detection tests, connection-reuse tests).
     *
     * <p>Example: {@code uniqueProjectName("DNI Duplicate")} → {@code "DNI Duplicate 3fa85f64-..."}
     */
    public static String uniqueProjectName(String prefix) {
        return prefix + " " + UUID.randomUUID();
    }

    /**
     * Returns a string of exactly {@code length} characters by repeating {@code filler}.
     * Useful for building boundary-value strings in-line without pre-computing constants.
     */
    public static String repeat(String filler, int length) {
        if (filler.isEmpty()) {
            throw new IllegalArgumentException("filler must not be empty");
        }
        StringBuilder sb = new StringBuilder(length);
        while (sb.length() < length) {
            sb.append(filler);
        }
        return sb.substring(0, length);
    }
}
