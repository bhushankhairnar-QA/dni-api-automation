package com.dni.apitest.assertions;

import io.restassured.response.Response;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reusable assertion helpers for the Lytics Projects API responses.
 *
 * <p>Centralises repeated assertion patterns so individual test methods stay concise.
 */
public final class ProjectAssertions {

    private ProjectAssertions() {}

    // -------------------------------------------------------------------------
    // Status + envelope
    // -------------------------------------------------------------------------

    /** Asserts HTTP 400 with the standard "Bad request" message/status envelope. */
    public static void assertBadRequest(Response response) {
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
    }

    // -------------------------------------------------------------------------
    // Field error helpers
    // -------------------------------------------------------------------------

    /**
     * Asserts HTTP 400 + exactly one error on {@code field} with {@code expectedCode}.
     *
     * @param field       JsonPath fragment after {@code errors.} (e.g. {@code "name"})
     * @param expectedCode  e.g. {@code "lytics.PROJECTS.NOT_EMPTY"}
     */
    public static void assertSingleFieldError(Response response, String field, String expectedCode) {
        assertBadRequest(response);
        assertThat(response.jsonPath().getList("errors." + field)).hasSize(1);
        assertThat(response.jsonPath().getString("errors." + field + "[0].code"))
                .isEqualTo(expectedCode);
    }

    /**
     * Asserts HTTP 400 + the named field has errors containing exactly the given codes
     * (order-independent).
     */
    public static void assertFieldErrors(Response response, String field, String... expectedCodes) {
        assertBadRequest(response);
        List<Map<String, Object>> errors = response.jsonPath().getList("errors." + field);
        assertThat(errors).extracting(m -> m.get("code"))
                .containsExactlyInAnyOrder((Object[]) expectedCodes);
    }

    /**
     * Asserts the {@code MAX_CHAR_LIMIT} error for {@code field} is present and exposes the
     * correct {@code maxCharacters} value.
     */
    public static void assertMaxCharError(Response response, String field, int expectedMax) {
        assertBadRequest(response);
        List<Map<String, Object>> errors = response.jsonPath().getList("errors." + field);
        Map<String, Object> maxCharError = errors.stream()
                .filter(e -> "lytics.PROJECTS.MAX_CHAR_LIMIT".equals(e.get("code")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No MAX_CHAR_LIMIT error found on field '" + field + "'"));
        assertThat(((Number) Objects.requireNonNull(maxCharError.get("maxCharacters"))).intValue())
                .isEqualTo(expectedMax);
    }

    /**
     * Asserts an error code on an arbitrary JsonPath (supports bracket notation for
     * connection fields, e.g. {@code "errors['connections.stackApiKeys'][0].code"}).
     */
    public static void assertErrorCode(Response response, String jsonPath, String expectedCode) {
        assertThat(response.jsonPath().getString(jsonPath)).isEqualTo(expectedCode);
    }

    // -------------------------------------------------------------------------
    // Success response helpers
    // -------------------------------------------------------------------------

    /**
     * Asserts HTTP 201 and that all core project fields match the supplied expected values.
     */
    public static void assertProjectCreated(
            Response response,
            String expectedName,
            String expectedDomain,
            String expectedDescription,
            String expectedStackApiKey) {
        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.jsonPath().getString("uid")).isNotNull();
        assertThat(response.jsonPath().getString("name")).isEqualTo(expectedName);
        assertThat(response.jsonPath().getString("domain")).isEqualTo(expectedDomain);
        assertThat(response.jsonPath().getString("description")).isEqualTo(expectedDescription);
        assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                .isEqualTo(expectedStackApiKey);
        assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
    }

    /**
     * Asserts either HTTP 201 (project created) or HTTP 400 with
     * {@code DUPLICATE_PROJECT_NAME} — used for idempotent positive tests that may be
     * retried in an environment that already has the project.
     */
    public static void assertCreatedOrDuplicate(Response response) {
        int status = response.getStatusCode();
        assertThat(status)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);
        if (status == 400) {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }
}
