package com.dni.apitest.assertions;

import io.restassured.response.Response;

import java.time.Instant;
import java.time.format.DateTimeParseException;
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

    // -------------------------------------------------------------------------
    // GET /projects — list item schema
    // -------------------------------------------------------------------------

    /**
     * Asserts one project object from {@code GET /projects} exposes the expected fields,
     * types, and organization scope (list-item contract).
     *
     * @param project                  deserialized project map (array element)
     * @param expectedOrganizationUid organization from {@code TestConfig} / request headers
     */
    public static void assertListedProjectMatchesSchema(
            Map<String, Object> project, String expectedOrganizationUid) {
        assertThat(project)
                .as("GET /projects list item must expose core project fields")
                .containsKeys("uid", "name", "domain", "organizationUid", "connections", "cdp");

        assertThat(project.get("uid")).as("project.uid").isNotNull().isInstanceOf(String.class);
        assertThat((String) project.get("uid")).as("project.uid").isNotBlank();

        assertThat(project.get("name")).as("project.name").isNotNull().isInstanceOf(String.class);

        assertThat(project.get("domain")).as("project.domain").isNotNull().isInstanceOf(String.class);

        assertThat(project.get("organizationUid"))
                .as("project.organizationUid")
                .isEqualTo(expectedOrganizationUid);

        assertThat(project.get("connections")).as("project.connections").isInstanceOf(Map.class);

        assertThat(project.get("cdp")).as("project.cdp").isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> cdp = (Map<String, Object>) project.get("cdp");
        assertThat(cdp.get("status"))
                .as("project.cdp.status")
                .isInstanceOf(String.class);
        assertThat((String) cdp.get("status")).as("project.cdp.status").isNotBlank();
    }

    /**
     * Asserts every {@code uid} in a non-empty {@code GET /projects} array is unique.
     *
     * @param projects deserialized root array (each element must expose {@code uid})
     */
    public static void assertListedProjectUidsAreUnique(List<Map<String, Object>> projects) {
        List<String> uids = projects.stream()
                .map(p -> (String) Objects.requireNonNull(p.get("uid"), "project.uid"))
                .toList();
        assertThat(uids)
                .as("GET /projects: each project uid must appear exactly once")
                .doesNotHaveDuplicates();
    }

    /**
     * Asserts {@code connections} on a {@code GET /projects} list item matches the list contract:
     * {@code stackApiKeys}, {@code launchProjectUids}, and {@code personalizeProjectUids} are JSON
     * arrays of non-blank strings (empty arrays are allowed).
     *
     * @param project one deserialized project object from the root array
     */
    /**
     * Asserts {@code cdp} on a {@code GET /projects} list item matches the CDP contract:
     * {@code aid} (positive number), {@code orgId}, {@code accountId}, {@code status}
     * ({@code active}), and {@code syncedAt} (non-blank ISO-8601 instant).
     *
     * @param project one deserialized project object from the root array
     */
    public static void assertListedProjectCdpMatchesSchema(Map<String, Object> project) {
        String projectUid = Objects.toString(project.get("uid"), "?");
        Object cdpObj = project.get("cdp");
        assertThat(cdpObj)
                .as("project [%s] cdp must be a JSON object", projectUid)
                .isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> cdp = (Map<String, Object>) cdpObj;
        assertThat(cdp)
                .as("project [%s] cdp must expose aid, orgId, accountId, status, syncedAt", projectUid)
                .containsKeys("aid", "orgId", "accountId", "status", "syncedAt");

        Object aid = cdp.get("aid");
        assertThat(aid)
                .as("project [%s] cdp.aid must be a JSON number", projectUid)
                .isInstanceOf(Number.class);
        assertThat(((Number) aid).intValue())
                .as("project [%s] cdp.aid must be positive", projectUid)
                .isPositive();

        assertListedCdpNonBlankString(cdp, "orgId", projectUid);
        assertListedCdpNonBlankString(cdp, "accountId", projectUid);

        assertThat(cdp.get("status"))
                .as("project [%s] cdp.status must be a string", projectUid)
                .isInstanceOf(String.class);
        assertThat((String) cdp.get("status"))
                .as("project [%s] cdp.status", projectUid)
                .isEqualTo("active");

        Object syncedRaw = cdp.get("syncedAt");
        assertThat(syncedRaw)
                .as("project [%s] cdp.syncedAt must be a string (ISO-8601 timestamp)", projectUid)
                .isInstanceOf(String.class);
        String syncedAt = (String) syncedRaw;
        assertThat(syncedAt).as("project [%s] cdp.syncedAt", projectUid).isNotBlank();
        try {
            Instant.parse(syncedAt);
        } catch (DateTimeParseException e) {
            throw new AssertionError(
                    "project [%s] cdp.syncedAt must be parseable as ISO-8601 instant: %s"
                            .formatted(projectUid, syncedAt),
                    e);
        }
    }

    private static void assertListedCdpNonBlankString(
            Map<String, Object> cdp, String field, String projectUid) {
        Object raw = cdp.get(field);
        assertThat(raw)
                .as("project [%s] cdp.%s must be a string", projectUid, field)
                .isInstanceOf(String.class);
        assertThat((String) raw)
                .as("project [%s] cdp.%s", projectUid, field)
                .isNotBlank();
    }

    public static void assertListedProjectConnectionsMatchSchema(Map<String, Object> project) {
        String projectUid = Objects.toString(project.get("uid"), "?");
        Object connObj = project.get("connections");
        assertThat(connObj)
                .as("project [%s] connections must be a JSON object", projectUid)
                .isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> connections = (Map<String, Object>) connObj;
        assertConnectionsShape(projectUid, connections);
    }

    /**
     * Asserts {@code connections} on a {@code GET /projects/{uid}} (single resource) body matches
     * the same string-array contract as list items.
     */
    public static void assertGetProjectResponseConnectionsMatchSchema(Response response) {
        String projectUid = response.jsonPath().getString("uid");
        assertThat(projectUid).as("GET /projects/{uid} response uid").isNotBlank();

        Map<String, Object> connections = response.jsonPath().getMap("connections");
        assertThat(connections)
                .as("project [%s] connections must be present", projectUid)
                .isNotNull();
        assertConnectionsShape(projectUid, connections);
    }

    private static void assertConnectionsShape(String projectUid, Map<String, Object> connections) {
        assertThat(connections)
                .as("project [%s] connections must expose stackApiKeys, launchProjectUids, personalizeProjectUids", projectUid)
                .containsKeys("stackApiKeys", "launchProjectUids", "personalizeProjectUids");

        assertListedConnectionStringArray(connections, "stackApiKeys", projectUid);
        assertListedConnectionStringArray(connections, "launchProjectUids", projectUid);
        assertListedConnectionStringArray(connections, "personalizeProjectUids", projectUid);
    }

    private static void assertListedConnectionStringArray(
            Map<String, Object> connections, String field, String projectUid) {
        Object raw = connections.get(field);
        assertThat(raw)
                .as("project [%s] connections.%s must be a JSON array", projectUid, field)
                .isInstanceOf(List.class);
        List<?> list = (List<?>) raw;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            assertThat(item)
                    .as("project [%s] connections.%s[%d] must be a string", projectUid, field, i)
                    .isInstanceOf(String.class);
            assertThat((String) item)
                    .as("project [%s] connections.%s[%d] must not be blank", projectUid, field, i)
                    .isNotBlank();
        }
    }

    // -------------------------------------------------------------------------
    // GET /projects/{uid} — timestamp strings
    // -------------------------------------------------------------------------

    /**
     * Asserts {@code createdAt}, {@code updatedAt}, and {@code cdp.syncedAt} on a
     * {@code GET /projects/{uid}} body are non-blank strings parseable as ISO-8601 instants
     * (for example {@code 2026-04-13T05:12:04.717Z}).
     */
    public static void assertGetProjectResponseTimestampFields(Response response) {
        String uid = response.jsonPath().getString("uid");
        assertThat(uid).as("GET /projects/{uid} response uid").isNotBlank();

        assertIso8601InstantString(
                response.jsonPath().getString("createdAt"),
                "GET /projects/%s createdAt".formatted(uid));
        assertIso8601InstantString(
                response.jsonPath().getString("updatedAt"),
                "GET /projects/%s updatedAt".formatted(uid));
        assertIso8601InstantString(
                response.jsonPath().getString("cdp.syncedAt"),
                "GET /projects/%s cdp.syncedAt".formatted(uid));
    }

    private static void assertIso8601InstantString(String value, String label) {
        assertThat(value).as(label).isNotNull().isNotBlank();
        try {
            Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new AssertionError("%s must be parseable as ISO-8601 instant: %s".formatted(label, value), e);
        }
    }
}
