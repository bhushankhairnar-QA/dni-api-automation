package com.dni.apitest.assertions;

import com.dni.apitest.config.TestConfig;
import com.dni.apitest.testdata.LyticsProjectTestData;
import io.restassured.response.Response;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reusable assertion helpers for the Lytics Projects API responses.
 *
 * <p>Centralises repeated assertion patterns so individual test methods stay concise.
 */
public final class ProjectAssertions {

    private ProjectAssertions() {}

    /** Inclusive bounds for {@code cdp.aid}: four-digit decimal integer (1000–9999). */
    private static final int CDP_AID_MIN_INCLUSIVE = 1000;
    private static final int CDP_AID_MAX_INCLUSIVE = 9999;

    /**
     * CDP {@code orgId} and {@code accountId} in API responses (e.g. {@code 930afa61e6720d4223efac8761bd9c39}) are
     * 32-character hexadecimal strings.
     */
    private static final Pattern CDP_HEX_32 = Pattern.compile("^[a-fA-F0-9]{32}$");

    /**
     * Lytics project (and related) resource identifiers are 24-character hexadecimal strings,
     * consistent with the values in {@link LyticsProjectTestData}.
     */
    private static final Pattern LYTICS_HEX_RESOURCE_UID = Pattern.compile("^[a-fA-F0-9]{24}$");

    // -------------------------------------------------------------------------
    // Status + envelope
    // -------------------------------------------------------------------------

    /** Asserts HTTP 400 with the standard "Bad request" message/status envelope. */
    public static void assertBadRequest(Response response) {
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
    }

    /** Asserts HTTP 404 with the standard GET-by-uid "Project not found" envelope. */
    public static void assertProjectNotFound(Response response) {
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Project not found");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(404);
        assertThat(response.jsonPath().getString("error")).isEqualTo("Not Found");
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
     * {@code aid} (JSON number, four-digit decimal integer 1000–9999), {@code orgId} and {@code accountId} (each
     * 32 hex characters as in API responses), {@code status} ({@code active}), and {@code syncedAt} (non-blank
     * ISO-8601 instant).
     *
     * <p>{@code orgId} and {@code accountId} are not sent on {@code POST /projects}; the API assigns them and
     * returns them on create and read responses.
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

        assertCdpAidIsFourDigitJsonNumber(cdp.get("aid"), projectUid);

        assertListedCdpHex32Identifier(cdp, "orgId", projectUid);
        assertListedCdpHex32Identifier(cdp, "accountId", projectUid);

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

    /**
     * Asserts a single-project body (POST 201 or GET /projects/{uid}) includes a {@code cdp} object with
     * {@code aid}, {@code orgId} and {@code accountId} (32 hex chars each), {@code status} ({@code active}), and
     * {@code syncedAt} (ISO-8601), same contract as {@link #assertListedProjectCdpMatchesSchema(Map)}. CDP fields
     * are returned by the API only, not supplied in the create request body.
     */
    public static void assertSingleProjectCdpObjectMatchesSchema(Response response) {
        String uid = response.jsonPath().getString("uid");
        Map<String, Object> cdp = response.jsonPath().getMap("cdp");
        assertThat(cdp).as("response body must include a cdp object").isNotNull();
        Map<String, Object> project = new HashMap<>();
        project.put("uid", uid != null ? uid : "?");
        project.put("cdp", cdp);
        assertListedProjectCdpMatchesSchema(project);
    }

    /**
     * Asserts {@code cdp.aid} on GET equals POST for the same project and is a four-digit decimal integer
     * ({@code 1000}–{@code 9999}), i.e. the stable CDP account id for that project.
     */
    public static void assertGetCdpAidMatchesPost(Response postResponse, Response getResponse) {
        Object postAid = postResponse.jsonPath().get("cdp.aid");
        Object getAid = getResponse.jsonPath().get("cdp.aid");
        assertThat(postAid).as("POST response must include cdp.aid").isNotNull();
        assertThat(getAid).as("GET response must include cdp.aid").isNotNull();
        assertCdpAidIsFourDigitJsonNumber(postAid, "POST body");
        assertCdpAidIsFourDigitJsonNumber(getAid, "GET body");
        assertThat(((Number) getAid).intValue())
                .as("GET cdp.aid must equal POST cdp.aid for the same project uid")
                .isEqualTo(((Number) postAid).intValue());
    }

    /**
     * Asserts {@code cdp.orgId} on the POST 201 response body and on GET /projects/{uid} is a 32-character
     * hexadecimal string (e.g. {@code 930afa61e6720d4223efac8761bd9c39}), and that both values match. {@code
     * cdp.orgId} is not part of the POST request payload; the server sets it when the project is created.
     */
    public static void assertGetCdpOrgIdMatchesPost(Response postResponse, Response getResponse) {
        String postOrgId = postResponse.jsonPath().getString("cdp.orgId");
        String getOrgId = getResponse.jsonPath().getString("cdp.orgId");
        assertCdpHex32String(postOrgId, "orgId", "POST 201 response body");
        assertCdpHex32String(getOrgId, "orgId", "GET response body");
        assertThat(getOrgId)
                .as("GET cdp.orgId must equal POST 201 cdp.orgId for the same project uid")
                .isEqualTo(postOrgId);
    }

    /**
     * Asserts {@code cdp.accountId} on the POST 201 response body and on GET /projects/{uid} is a 32-character
     * hexadecimal string (e.g. {@code 99c7c1fce9bd1d947f7c7d6d88f4deea}), and that both values match. {@code
     * cdp.accountId} is not part of the POST request payload; the server sets it when the project is created.
     */
    public static void assertGetCdpAccountIdMatchesPost(Response postResponse, Response getResponse) {
        String postAccountId = postResponse.jsonPath().getString("cdp.accountId");
        String getAccountId = getResponse.jsonPath().getString("cdp.accountId");
        assertCdpHex32String(postAccountId, "accountId", "POST 201 response body");
        assertCdpHex32String(getAccountId, "accountId", "GET response body");
        assertThat(getAccountId)
                .as("GET cdp.accountId must equal POST 201 cdp.accountId for the same project uid")
                .isEqualTo(postAccountId);
    }

    /**
     * Asserts {@code cdp.status} on the POST 201 response body and on GET /projects/{uid} is {@code active} and
     * that both match. {@code cdp.status} is not supplied in the create request body; the API returns it on
     * create and read.
     */
    public static void assertGetCdpStatusMatchesPost(Response postResponse, Response getResponse) {
        String postStatus = postResponse.jsonPath().getString("cdp.status");
        String getStatus = getResponse.jsonPath().getString("cdp.status");
        assertThat(postStatus)
                .as("POST 201 response body must include cdp.status active")
                .isNotNull()
                .isEqualTo("active");
        assertThat(getStatus)
                .as("GET response body must include cdp.status equal to POST and active")
                .isNotNull()
                .isEqualTo("active")
                .isEqualTo(postStatus);
    }

    /**
     * Asserts {@code cdp.syncedAt} on the POST 201 response body and on GET /projects/{uid} are non-blank strings
     * parseable as ISO-8601 instants (for example {@code 2026-04-13T05:04:23.758Z} with {@link Instant#parse}). {@code
     * cdp.syncedAt} is returned by the API, not supplied in the create request body.
     */
    public static void assertGetCdpSyncedAtFormat(Response postResponse, Response getResponse) {
        String postSynced = postResponse.jsonPath().getString("cdp.syncedAt");
        String getSynced = getResponse.jsonPath().getString("cdp.syncedAt");
        assertIso8601InstantString(postSynced, "POST 201 response body cdp.syncedAt");
        assertIso8601InstantString(getSynced, "GET response body cdp.syncedAt");
    }

    /**
     * Asserts {@code createdBy} on the POST 201 response body and on GET /projects/{uid} are non-blank and equal
     * (for example {@code blt7a752c371e16a089}). The field identifies the principal that created the project; it is
     * not supplied in the create request body.
     */
    public static void assertGetCreatedByMatchesPost(Response postResponse, Response getResponse) {
        String postCreatedBy = postResponse.jsonPath().getString("createdBy");
        String getCreatedBy = getResponse.jsonPath().getString("createdBy");
        assertThat(postCreatedBy)
                .as("POST 201 response body must include non-blank createdBy")
                .isNotBlank();
        assertThat(getCreatedBy)
                .as("GET response body must include non-blank createdBy equal to POST 201")
                .isNotBlank()
                .isEqualTo(postCreatedBy);
    }

    /**
     * Asserts {@code createdAt} on the POST 201 response body and on GET /projects/{uid} are non-blank strings
     * parseable as ISO-8601 instants (for example {@code 2026-04-13T05:04:13.635Z} with {@link Instant#parse}) and
     * that GET equals POST. {@code createdAt} is returned by the API, not supplied in the create request body.
     */
    public static void assertGetCreatedAtMatchesPost(Response postResponse, Response getResponse) {
        String postCreatedAt = postResponse.jsonPath().getString("createdAt");
        String getCreatedAt = getResponse.jsonPath().getString("createdAt");
        assertIso8601InstantString(postCreatedAt, "POST 201 response body createdAt");
        assertIso8601InstantString(getCreatedAt, "GET response body createdAt");
        assertThat(getCreatedAt)
                .as("GET createdAt must equal POST 201 createdAt for the same project uid")
                .isEqualTo(postCreatedAt);
    }

    private static void assertCdpAidIsFourDigitJsonNumber(Object aid, String contextLabel) {
        assertThat(aid)
                .as("%s: cdp.aid must be a JSON number", contextLabel)
                .isInstanceOf(Number.class);
        int value = ((Number) aid).intValue();
        assertThat(value)
                .as(
                        "%s: cdp.aid must be a four-digit decimal integer (%d–%d)",
                        contextLabel, CDP_AID_MIN_INCLUSIVE, CDP_AID_MAX_INCLUSIVE)
                .isBetween(CDP_AID_MIN_INCLUSIVE, CDP_AID_MAX_INCLUSIVE);
    }

    private static void assertListedCdpHex32Identifier(Map<String, Object> cdp, String field, String projectUid) {
        Object raw = cdp.get(field);
        assertThat(raw)
                .as("project [%s] cdp.%s must be a string", projectUid, field)
                .isInstanceOf(String.class);
        assertCdpHex32String((String) raw, field, "project [%s]".formatted(projectUid));
    }

    private static void assertCdpHex32String(String value, String fieldName, String contextDescription) {
        assertThat(value)
                .as(
                        "%s: cdp.%s must be 32 hexadecimal characters (e.g. 930afa61e6720d4223efac8761bd9c39)",
                        contextDescription, fieldName)
                .isNotNull()
                .isNotBlank()
                .matches(CDP_HEX_32);
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

    /**
     * Asserts a {@code GET /projects/{uid}} body carries the same core fields and connection IDs that
     * were sent on {@code POST /projects} for a full valid create payload (default connections from
     * {@link com.dni.apitest.testdata.LyticsProjectPayloadBuilder#validFullProjectCreatePayload()} /
     * {@link com.dni.apitest.testdata.LyticsProjectPayloadBuilder#validFullProjectCreatePayloadWithName(String)}).
     *
     * @param getResponse         successful GET /projects/{uid} response
     * @param expectedUid         project uid (must match path and body {@code uid})
     * @param expectedName        expected {@code name} from the create request
     * @param expectedDomain      expected {@code domain} from the create request
     * @param expectedDescription expected {@code description} from the create request
     */
    public static void assertGetByUidResponseMatchesPostPayload(
            Response getResponse,
            String expectedUid,
            String expectedName,
            String expectedDomain,
            String expectedDescription) {
        assertThat(getResponse.jsonPath().getString("uid"))
                .as("GET uid must match the project created by POST")
                .isEqualTo(expectedUid);

        assertThat(getResponse.jsonPath().getString("name")).isEqualTo(expectedName);
        assertThat(getResponse.jsonPath().getString("domain")).isEqualTo(expectedDomain);
        assertThat(getResponse.jsonPath().getString("description")).isEqualTo(expectedDescription);

        assertThat(getResponse.jsonPath().getString("organizationUid"))
                .as("GET project organization must match configured organization_uid header")
                .isEqualTo(TestConfig.lyticsOrganizationUid());

        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
                .as("connections.stackApiKeys must match POST payload")
                .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
                .as("connections.launchProjectUids must match POST payload")
                .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .as("connections.personalizeProjectUids must match POST payload")
                .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        assertThat(getResponse.jsonPath().getString("cdp.status")).isEqualTo("active");
    }

    /**
     * Asserts GET {@code connections} (stackApiKeys, launchProjectUids, personalizeProjectUids) matches a
     * successful POST /projects response for the same project.
     */
    public static void assertGetConnectionsMatchPost(Response postResponse, Response getResponse) {
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
                .as("GET connections.stackApiKeys must equal POST response")
                .isEqualTo(postResponse.jsonPath().getList("connections.stackApiKeys"));
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
                .as("GET connections.launchProjectUids must equal POST response")
                .isEqualTo(postResponse.jsonPath().getList("connections.launchProjectUids"));
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .as("GET connections.personalizeProjectUids must equal POST response")
                .isEqualTo(postResponse.jsonPath().getList("connections.personalizeProjectUids"));
    }

    /**
     * Asserts GET {@code connections.stackApiKeys} equals the array on a successful POST /projects response.
     */
    public static void assertGetStackApiKeysMatchPost(Response postResponse, Response getResponse) {
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
                .as("GET connections.stackApiKeys must equal POST response")
                .isEqualTo(postResponse.jsonPath().getList("connections.stackApiKeys"));
    }

    /**
     * Asserts GET {@code connections.launchProjectUids} equals the array on a successful POST /projects response.
     */
    public static void assertGetLaunchProjectUidsMatchPost(Response postResponse, Response getResponse) {
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
                .as("GET connections.launchProjectUids must equal POST response")
                .isEqualTo(postResponse.jsonPath().getList("connections.launchProjectUids"));
    }

    /**
     * Asserts GET {@code connections.personalizeProjectUids} equals the array on a successful POST /projects
     * response.
     */
    public static void assertGetPersonalizeProjectUidsMatchPost(Response postResponse, Response getResponse) {
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .as("GET connections.personalizeProjectUids must equal POST response")
                .isEqualTo(postResponse.jsonPath().getList("connections.personalizeProjectUids"));
    }

    /**
     * Asserts {@code connections.stackApiKeys}, {@code connections.launchProjectUids}, and {@code
     * connections.personalizeProjectUids} each contain no duplicate entries (same value must not appear twice).
     */
    public static void assertConnectionsArraysHaveNoDuplicateEntries(Response response) {
        assertConnectionListHasNoDuplicates(response, "connections.stackApiKeys");
        assertConnectionListHasNoDuplicates(response, "connections.launchProjectUids");
        assertConnectionListHasNoDuplicates(response, "connections.personalizeProjectUids");
    }

    private static void assertConnectionListHasNoDuplicates(Response response, String jsonPath) {
        List<?> list = response.jsonPath().getList(jsonPath);
        assertThat(list)
                .as("%s must not contain duplicate entries", jsonPath)
                .doesNotHaveDuplicates();
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
     * Asserts the JSON {@code uid} field is present, non-blank, and matches the Lytics
     * 24-character hexadecimal resource identifier format.
     *
     * @param contextLabel short label for assertion messages (e.g. {@code "POST /projects response"})
     */
    public static void assertLyticsProjectUidField(Response response, String contextLabel) {
        String uid = response.jsonPath().getString("uid");
        assertThat(uid)
                .as("%s: uid must be present", contextLabel)
                .isNotNull();
        assertThat(uid)
                .as("%s: uid must be non-empty", contextLabel)
                .isNotBlank();
        assertThat(uid)
                .as("%s: uid must be 24 hex characters", contextLabel)
                .matches(LYTICS_HEX_RESOURCE_UID);
    }

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
