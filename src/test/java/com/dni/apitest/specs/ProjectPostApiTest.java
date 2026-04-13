package com.dni.apitest.specs;

import com.dni.apitest.base.BaseApiTest;
import com.dni.apitest.config.TestConfig;
import com.dni.apitest.constants.ApiPaths;
import com.dni.apitest.testdata.LyticsProjectTestData;
import com.dni.apitest.testdata.LyticsProjectPayloadBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectPostApiTest extends BaseApiTest {

    @Test(
            priority = 1,
            description = "POST /projects with a valid project name — expect 201 Created or 400 duplicate name")
    public void TC_001_Send_POST_request_with_valid_project_name() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with full valid JSON payload");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 2,
            description = "POST /projects without `name` — expect 400 and aggregated name validation errors")
    public void TC_002_Send_POST_request_without_project_name_field() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with payload missing the project name field");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithoutName())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and three distinct error codes on errors.name");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> nameErrors = response.jsonPath().getList("errors.name");
        assertThat(nameErrors).hasSize(3);

        assertThat(nameErrors)
                .extracting(m -> m.get("code"))
                .containsExactlyInAnyOrder(
                        "lytics.PROJECTS.MAX_CHAR_LIMIT",
                        "lytics.PROJECTS.NOT_EMPTY",
                        "lytics.PROJECTS.NOT_STRING");

        reportStep("Assert MAX_CHAR_LIMIT entry exposes maxCharacters = configured max length");
        Map<String, Object> maxCharError =
                nameErrors.stream()
                        .filter(m -> "lytics.PROJECTS.MAX_CHAR_LIMIT".equals(m.get("code")))
                        .findFirst()
                        .orElseThrow();

        assertThat(((Number) Objects.requireNonNull(maxCharError.get("maxCharacters"))).intValue())
                .isEqualTo(LyticsProjectTestData.PROJECT_NAME_MAX_LENGTH);
    }

    @Test(
            priority = 3,
            description = "POST /projects with empty string name — expect 400 NOT_EMPTY on errors.name")
    public void TC_003_Send_POST_request_with_empty_string_as_project_name() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with name = \"\"");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithEmptyName())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400, envelope fields, and single NOT_EMPTY name error");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(response.jsonPath().getList("errors.name")).hasSize(1);
        assertThat(response.jsonPath().getString("errors.name[0].code"))
                .isEqualTo("lytics.PROJECTS.NOT_EMPTY");
    }

    @Test(
            priority = 4,
            description = "POST /projects with whitespace-only name — expect 400 NOT_EMPTY")
    public void TC_004_Send_POST_request_with_only_spaces_in_project_name() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with name containing only spaces");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithSpaceOnlyName())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400 and NOT_EMPTY on errors.name");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(response.jsonPath().getList("errors.name")).hasSize(1);
        assertThat(response.jsonPath().getString("errors.name[0].code"))
                .isEqualTo("lytics.PROJECTS.NOT_EMPTY");
    }

    @Test(
            priority = 5,
            description = "POST /projects with name length exactly 200 — expect 201 or 400 duplicate")
    public void TC_005_Send_POST_request_with_project_name_length_exactly_200_characters() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Precondition: generated max-length name string is exactly 200 characters");
        assertThat(LyticsProjectTestData.PROJECT_NAME_EXACTLY_MAX_LENGTH.length())
                .isEqualTo(LyticsProjectTestData.PROJECT_NAME_MAX_LENGTH);

        reportStep("POST " + ApiPaths.PROJECTS + " with full payload using max-length name");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithMaxLengthName())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 or 400 (duplicate), mirroring TC_001");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture uid and assert field parity with payload");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.PROJECT_NAME_EXACTLY_MAX_LENGTH);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert DUPLICATE_PROJECT_NAME");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 6,
            description = "POST /projects with name longer than 200 chars — expect 400 MAX_CHAR_LIMIT")
    public void TC_006_Send_POST_request_with_project_name_length_more_than_200_characters() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Precondition: over-max name length is max+1 characters");
        assertThat(LyticsProjectTestData.PROJECT_NAME_ONE_OVER_MAX_LENGTH.length())
                .isEqualTo(LyticsProjectTestData.PROJECT_NAME_MAX_LENGTH + 1);

        reportStep("POST " + ApiPaths.PROJECTS + " with over-length name in payload");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameOverMaxLength())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400, error code MAX_CHAR_LIMIT, and maxCharacters in error payload");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> nameErrors = response.jsonPath().getList("errors.name");
        assertThat(nameErrors).hasSize(1);

        Map<String, Object> nameError = nameErrors.get(0);
        assertThat(nameError.get("code")).isEqualTo("lytics.PROJECTS.MAX_CHAR_LIMIT");
        assertThat(((Number) Objects.requireNonNull(nameError.get("maxCharacters"))).intValue())
                .isEqualTo(LyticsProjectTestData.PROJECT_NAME_MAX_LENGTH);
    }

    @Test(
            priority = 7,
            description =
                    "POST /projects to create a project, then POST again with the same name — expect 400 DUPLICATE_PROJECT_NAME")
    public void TC_007_Send_POST_request_with_duplicate_project_name() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        String uniqueName = "DNI Duplicate POST " + UUID.randomUUID();

        reportStep("First POST: create project with unique name so duplicate is deterministic");
        Response createResponse =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        createResponse.prettyPrint();
        reportResponseBody(createResponse);

        reportStep("Assert first POST returns 201 and capture uid for cleanup");
        assertThat(createResponse.getStatusCode()).isEqualTo(201);
        projectUidToCleanup = createResponse.jsonPath().getString("uid");
        assertThat(projectUidToCleanup).isNotNull();

        reportStep("Second POST: same payload (duplicate name) — expect 400 with error envelope");
        Response duplicateResponse =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        duplicateResponse.prettyPrint();
        reportResponseBody(duplicateResponse);

        reportStep("Assert HTTP 400, message, status, and single DUPLICATE_PROJECT_NAME on errors.name");
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(400);
        assertThat(duplicateResponse.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(duplicateResponse.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(duplicateResponse.jsonPath().getList("errors.name")).hasSize(1);
        assertThat(duplicateResponse.jsonPath().getString("errors.name[0].code"))
                .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
    }

    @Test(
            priority = 8,
            description =
                    "POST /projects with project name containing special characters (Proj@#123) — expect 201 Created or 400 duplicate name (same as TC_001)")
    public void TC_008_Send_POST_request_with_project_name_having_special_characters() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep(
                "Build request: POST "
                        + ApiPaths.PROJECTS
                        + " with full valid JSON payload (name includes @ and #)");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(
                                        LyticsProjectTestData.PROJECT_NAME_WITH_SPECIAL_CHARS))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.PROJECT_NAME_WITH_SPECIAL_CHARS);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 9,
            description =
                    "POST /projects with project name containing numbers (DNI123) — expect 201 Created or 400 duplicate name (same as TC_001)")
    public void TC_009_Send_POST_request_with_project_name_having_numbers() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep(
                "Build request: POST "
                        + ApiPaths.PROJECTS
                        + " with full valid JSON payload (name includes digits)");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(
                                        LyticsProjectTestData.PROJECT_NAME_WITH_NUMBERS))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.PROJECT_NAME_WITH_NUMBERS);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 10,
            description =
                    "POST /projects with project name in different letter case but same value (e.g. \"dni test\" vs \"Dni Test\") — expect 201 Created, 400 duplicate name, or 500 server error (same as TC_001)")
    public void TC_010_Send_POST_request_with_project_name_different_case_same_value() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep(
                "Build request: POST "
                        + ApiPaths.PROJECTS
                        + " with full valid JSON payload (all-lowercase name, same value as title-cased \"Dni Test\")");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(
                                        LyticsProjectTestData.PROJECT_NAME_LOWERCASE_DNI_TEST))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created), 400 (duplicate project name), or 500 (server error)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created), 400 (duplicate project name), or 500 (server error)")
                .isIn(201, 400, 500);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.PROJECT_NAME_LOWERCASE_DNI_TEST);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else if (statusCode == 400) {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        } else {
            reportStep("500 path: Internal Server Error; no project uid for cleanup");
            assertThat(statusCode).isEqualTo(500);
        }
    }

    @Test(
            priority = 11,
            description =
                    "POST /projects with project name having leading and trailing spaces (\" DNI Test \") — expect 201 Created, 400 duplicate name, or 500 server error (same as TC_001)")
    public void TC_011_Send_POST_request_with_project_name_leading_trailing_spaces() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep(
                "Build request: POST "
                        + ApiPaths.PROJECTS
                        + " with full valid JSON payload (name with leading/trailing spaces)");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(
                                        LyticsProjectTestData.PROJECT_NAME_LEADING_TRAILING_SPACES))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created), 400 (duplicate project name), or 500 (server error)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created), 400 (duplicate project name), or 500 (server error)")
                .isIn(201, 400, 500);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep(
                    "Assert response fields match payload; API trims name so stored value equals trim(request name)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.PROJECT_NAME_LEADING_TRAILING_SPACES.trim());

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else if (statusCode == 400) {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        } else {
            reportStep("500 path: Internal Server Error; no project uid for cleanup");
            assertThat(statusCode).isEqualTo(500);
        }
    }

    @Test(
            priority = 12,
            description = "POST /projects with `name` explicitly null — expect 400 validation on errors.name")
    public void TC_012_Send_POST_request_with_null_as_project_name() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with name = null in JSON body");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithNullName())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and name validation errors (same shape as missing name)");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> nameErrors = response.jsonPath().getList("errors.name");
        assertThat(nameErrors).hasSize(3);

        assertThat(nameErrors)
                .extracting(m -> m.get("code"))
                .containsExactlyInAnyOrder(
                        "lytics.PROJECTS.MAX_CHAR_LIMIT",
                        "lytics.PROJECTS.NOT_EMPTY",
                        "lytics.PROJECTS.NOT_STRING");

        Map<String, Object> maxCharError =
                nameErrors.stream()
                        .filter(m -> "lytics.PROJECTS.MAX_CHAR_LIMIT".equals(m.get("code")))
                        .findFirst()
                        .orElseThrow();

        assertThat(((Number) Objects.requireNonNull(maxCharError.get("maxCharacters"))).intValue())
                .isEqualTo(LyticsProjectTestData.PROJECT_NAME_MAX_LENGTH);
    }

    @Test(
            priority = 13,
            description =
                    "POST /projects with very long name (200 chars) including spaces and symbols — expect 201 or 400 duplicate (same as TC_001)")
    public void TC_013_Send_POST_request_with_very_long_name_spaces_and_symbols_within_max_length() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Precondition: generated name is exactly max length and contains spaces and symbols");
        assertThat(LyticsProjectTestData.PROJECT_NAME_LONG_SPACES_SYMBOLS_MAX.length())
                .isEqualTo(LyticsProjectTestData.PROJECT_NAME_MAX_LENGTH);
        assertThat(LyticsProjectTestData.PROJECT_NAME_LONG_SPACES_SYMBOLS_MAX).contains(" ");
        assertThat(LyticsProjectTestData.PROJECT_NAME_LONG_SPACES_SYMBOLS_MAX).contains("@");

        reportStep("POST " + ApiPaths.PROJECTS + " with full payload using long spaces/symbols name");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithLongSpacesSymbolsName())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 or 400 (duplicate), mirroring TC_001");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture uid and assert field parity with payload");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.PROJECT_NAME_LONG_SPACES_SYMBOLS_MAX);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert DUPLICATE_PROJECT_NAME");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 14,
            description =
                    "POST /projects with valid domain (e.g. example.com) — expect 201 Created or 400 duplicate name (same as TC_001)")
    public void TC_014_Send_POST_request_with_valid_domain_example_com() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with full valid payload, domain = example.com");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameAndDomain(
                                        LyticsProjectTestData.VALID_PROJECT_NAME,
                                        LyticsProjectTestData.EXAMPLE_COM_DOMAIN))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.EXAMPLE_COM_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 15,
            description = "POST /projects without `domain` — expect 400 and aggregated domain validation errors")
    public void TC_015_Send_POST_request_without_domain_field() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with payload missing the domain field");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithoutDomain())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and four distinct error codes on errors.domain");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> domainErrors = response.jsonPath().getList("errors.domain");
        assertThat(domainErrors).hasSize(4);

        assertThat(domainErrors)
                .extracting(m -> m.get("code"))
                .containsExactlyInAnyOrder(
                        "lytics.PROJECTS.MAX_CHAR_LIMIT",
                        "lytics.PROJECTS.INVALID_DOMAIN",
                        "lytics.PROJECTS.NOT_EMPTY",
                        "lytics.PROJECTS.NOT_STRING");

        reportStep("Assert MAX_CHAR_LIMIT entry exposes maxCharacters = configured domain max length");
        Map<String, Object> maxCharError =
                domainErrors.stream()
                        .filter(m -> "lytics.PROJECTS.MAX_CHAR_LIMIT".equals(m.get("code")))
                        .findFirst()
                        .orElseThrow();

        assertThat(((Number) Objects.requireNonNull(maxCharError.get("maxCharacters"))).intValue())
                .isEqualTo(LyticsProjectTestData.PROJECT_DOMAIN_MAX_LENGTH);
    }

    @Test(
            priority = 16,
            description =
                    "POST /projects with empty string domain — expect 400 INVALID_DOMAIN and NOT_EMPTY on errors.domain")
    public void TC_016_Send_POST_request_with_empty_string_as_domain() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain = \"\"");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithEmptyDomain())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and two domain validation errors");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> domainErrors = response.jsonPath().getList("errors.domain");
        assertThat(domainErrors).hasSize(2);

        assertThat(domainErrors)
                .extracting(m -> m.get("code"))
                .containsExactlyInAnyOrder(
                        "lytics.PROJECTS.INVALID_DOMAIN",
                        "lytics.PROJECTS.NOT_EMPTY");
    }

    @Test(
            priority = 17,
            description =
                    "POST /projects with whitespace-only domain — expect 400 INVALID_DOMAIN and NOT_EMPTY on errors.domain")
    public void TC_017_Send_POST_request_with_only_spaces_in_domain() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain containing only spaces");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithSpaceOnlyDomain())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and two domain validation errors");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> domainErrors = response.jsonPath().getList("errors.domain");
        assertThat(domainErrors).hasSize(2);

        assertThat(domainErrors)
                .extracting(m -> m.get("code"))
                .containsExactlyInAnyOrder(
                        "lytics.PROJECTS.INVALID_DOMAIN",
                        "lytics.PROJECTS.NOT_EMPTY");
    }

    @Test(
            priority = 18,
            description =
                    "POST /projects with domain length exactly 200 — expect 201 or 400 duplicate (same as TC_001)")
    public void TC_018_Send_POST_request_with_domain_length_exactly_200_characters() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Precondition: generated max-length domain string is exactly 200 characters");
        assertThat(LyticsProjectTestData.DOMAIN_EXACTLY_MAX_LENGTH.length())
                .isEqualTo(LyticsProjectTestData.PROJECT_DOMAIN_MAX_LENGTH);

        reportStep("POST " + ApiPaths.PROJECTS + " with full payload using max-length domain");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithMaxLengthDomain())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);
    }

    @Test(
            priority = 19,
            description =
                    "POST /projects with domain longer than 200 chars — expect 400 MAX_CHAR_LIMIT on errors.domain")
    public void TC_019_Send_POST_request_with_domain_length_more_than_200_characters() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Precondition: over-max domain length is max+1 characters");
        assertThat(LyticsProjectTestData.DOMAIN_ONE_OVER_MAX_LENGTH.length())
                .isEqualTo(LyticsProjectTestData.PROJECT_DOMAIN_MAX_LENGTH + 1);

        reportStep("POST " + ApiPaths.PROJECTS + " with over-length domain in payload");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithDomainOverMaxLength())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);
    }

    @Test(
            priority = 20,
            description =
                    "POST /projects with invalid domain format (abc) — expect 400 INVALID_DOMAIN on errors.domain")
    public void TC_020_Send_POST_request_with_invalid_domain_format_abc() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain = \"abc\"");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithInvalidDomainFormatAbc())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and single INVALID_DOMAIN error");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(response.jsonPath().getList("errors.domain")).hasSize(1);
        assertThat(response.jsonPath().getString("errors.domain[0].code"))
                .isEqualTo("lytics.PROJECTS.INVALID_DOMAIN");
    }

    @Test(
            priority = 21,
            description =
                    "POST /projects with domain missing TLD (e.g. example) — expect 400 INVALID_DOMAIN on errors.domain")
    public void TC_021_Send_POST_request_with_domain_missing_tld() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain without TLD (single label)");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDomainMissingTld())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and single INVALID_DOMAIN error");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(response.jsonPath().getList("errors.domain")).hasSize(1);
        assertThat(response.jsonPath().getString("errors.domain[0].code"))
                .isEqualTo("lytics.PROJECTS.INVALID_DOMAIN");
    }

    @Test(
            priority = 22,
            description =
                    "POST /projects with domain containing special characters (exa$mple.com) — expect 400 INVALID_DOMAIN on errors.domain")
    public void TC_022_Send_POST_request_with_domain_having_special_characters() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain = \"exa$mple.com\"");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDomainSpecialCharacters())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and single INVALID_DOMAIN error");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(response.jsonPath().getList("errors.domain")).hasSize(1);
        assertThat(response.jsonPath().getString("errors.domain[0].code"))
                .isEqualTo("lytics.PROJECTS.INVALID_DOMAIN");
    }

    @Test(
            priority = 23,
            description =
                    "POST /projects with domain containing spaces (example .com) — expect 400 INVALID_DOMAIN on errors.domain")
    public void TC_023_Send_POST_request_with_domain_having_spaces() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain = \"example .com\"");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDomainHavingSpaces())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and single INVALID_DOMAIN error");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(response.jsonPath().getList("errors.domain")).hasSize(1);
        assertThat(response.jsonPath().getString("errors.domain[0].code"))
                .isEqualTo("lytics.PROJECTS.INVALID_DOMAIN");
    }

    @Test(
            priority = 24,
            description =
                    "POST /projects with valid subdomain (sub.example.com) — expect 201 Created or 400 duplicate name (same as TC_001)")
    public void TC_024_Send_POST_request_with_valid_subdomain_example_com() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with full valid payload, domain = sub.example.com");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithValidSubdomain())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.SUBDOMAIN_EXAMPLE_COM);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 25,
            description =
                    "POST /projects with uppercase domain (EXAMPLE.COM) — expect 201 Created or 400 duplicate name (same as TC_001)")
    public void TC_025_Send_POST_request_with_uppercase_domain_example_com() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with full valid payload, domain = EXAMPLE.COM");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithUppercaseDomain())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);

            assertThat(response.jsonPath().getString("domain"))
                    .as("Response domain may match request casing or be normalized (e.g. example.com)")
                    .isEqualToIgnoringCase(LyticsProjectTestData.EXAMPLE_COM_DOMAIN_UPPERCASE);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 26,
            description =
                    "POST /projects with domain having leading and trailing spaces (\" example.com \") — expect 201 Created or 400 duplicate name (same as TC_001)")
    public void TC_026_Send_POST_request_with_domain_leading_trailing_spaces() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep(
                "Build request: POST "
                        + ApiPaths.PROJECTS
                        + " with full valid payload (domain with leading/trailing spaces)");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithDomainLeadingTrailingSpaces())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep(
                    "Assert response fields match payload; API trims domain so stored value equals trim(request domain)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.DOMAIN_LEADING_TRAILING_SPACES.trim());

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 27,
            description =
                    "POST /projects with `domain` explicitly null — expect 400 INVALID_DOMAIN on errors.domain")
    public void TC_027_Send_POST_request_with_null_as_domain() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain = null in JSON body");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithNullDomain())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and four distinct error codes on errors.domain");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> domainErrors = response.jsonPath().getList("errors.domain");
        assertThat(domainErrors).hasSize(4);

        assertThat(domainErrors)
                .extracting(m -> m.get("code"))
                .containsExactlyInAnyOrder(
                        "lytics.PROJECTS.MAX_CHAR_LIMIT",
                        "lytics.PROJECTS.INVALID_DOMAIN",
                        "lytics.PROJECTS.NOT_EMPTY",
                        "lytics.PROJECTS.NOT_STRING");

        reportStep("Assert MAX_CHAR_LIMIT entry exposes maxCharacters = configured domain max length");
        Map<String, Object> maxCharError =
                domainErrors.stream()
                        .filter(m -> "lytics.PROJECTS.MAX_CHAR_LIMIT".equals(m.get("code")))
                        .findFirst()
                        .orElseThrow();

        assertThat(((Number) Objects.requireNonNull(maxCharError.get("maxCharacters"))).intValue())
                .isEqualTo(LyticsProjectTestData.PROJECT_DOMAIN_MAX_LENGTH);
    }

    @Test(
            priority = 28,
            description =
                    "POST /projects with numeric domain (123.com) — expect 201 Created or 400 duplicate name (same as TC_001)")
    public void TC_028_Send_POST_request_with_numeric_domain_123_com() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with full valid payload, domain = 123.com");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNumericDomain())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.NUMERIC_DOMAIN_123_COM);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 29,
            description =
                    "POST /projects with hyphenated domain (my-site.com) — expect 201 Created or 400 duplicate name (same as TC_001)")
    public void TC_029_Send_POST_request_with_hyphenated_domain_my_site_com() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with full valid payload, domain = my-site.com");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithHyphenatedDomain())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.HYPHENATED_DOMAIN_MY_SITE_COM);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 30,
            description =
                    "POST /projects with domain starting with hyphen (-example.com) — expect 400 INVALID_DOMAIN on errors.domain")
    public void TC_030_Send_POST_request_with_domain_starting_with_hyphen() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain = \"-example.com\"");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDomainStartingWithHyphen())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and single INVALID_DOMAIN error");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(response.jsonPath().getList("errors.domain")).hasSize(1);
        assertThat(response.jsonPath().getString("errors.domain[0].code"))
                .isEqualTo("lytics.PROJECTS.INVALID_DOMAIN");
    }

    @Test(
            priority = 31,
            description =
                    "POST /projects with domain ending with hyphen in a label (example-.com) — expect 400 INVALID_DOMAIN on errors.domain")
    public void TC_031_Send_POST_request_with_domain_ending_with_hyphen() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain = \"example-.com\"");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDomainEndingWithHyphen())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and single INVALID_DOMAIN error");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(response.jsonPath().getList("errors.domain")).hasSize(1);
        assertThat(response.jsonPath().getString("errors.domain[0].code"))
                .isEqualTo("lytics.PROJECTS.INVALID_DOMAIN");
    }

    @Test(
            priority = 32,
            description =
                    "POST /projects with consecutive dots in domain (example..com) — expect 400 INVALID_DOMAIN on errors.domain")
    public void TC_032_Send_POST_request_with_domain_consecutive_dots() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain = \"example..com\"");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDomainConsecutiveDots())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and single INVALID_DOMAIN error");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(response.jsonPath().getList("errors.domain")).hasSize(1);
        assertThat(response.jsonPath().getString("errors.domain[0].code"))
                .isEqualTo("lytics.PROJECTS.INVALID_DOMAIN");
    }

    @Test(
            priority = 33,
            description =
                    "POST /projects with full URL as domain (https://example.com) — expect 400 INVALID_DOMAIN on errors.domain")
    public void TC_033_Send_POST_request_with_https_url_as_domain() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with domain = \"https://example.com\"");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDomainAsHttpsUrl())
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 400, message, status, and single INVALID_DOMAIN error");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        assertThat(response.jsonPath().getList("errors.domain")).hasSize(1);
        assertThat(response.jsonPath().getString("errors.domain[0].code"))
                .isEqualTo("lytics.PROJECTS.INVALID_DOMAIN");
    }

    @Test(
            priority = 34,
            description =
                    "POST /projects without `description` — expect 201 Created or 400 duplicate name (same envelope as TC_001)")
    public void TC_034_Send_POST_request_without_description_field() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        String uniqueName = "DNI Desc Omit " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with name, domain, connections; description field omitted");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithoutDescriptionField(
                                        uniqueName, LyticsProjectTestData.VALID_DOMAIN))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields; description absent or empty when omitted");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description")).isNullOrEmpty();

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 35,
            description =
                    "POST /projects with valid description (e.g. Sample project description) — expect 201 or 400 duplicate (same as TC_001)")
    public void TC_035_Send_POST_request_with_valid_description() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        String uniqueName = "DNI Desc Valid " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with description = Sample project description");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.SAMPLE_PROJECT_DESCRIPTION))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.SAMPLE_PROJECT_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 36,
            description =
                    "POST /projects with empty string description — expect 201 or 400 duplicate (same as TC_001)")
    public void TC_036_Send_POST_request_with_empty_string_as_description() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        String uniqueName = "DNI Desc Empty " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with description = \"\"");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.EMPTY_DESCRIPTION))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.EMPTY_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 37,
            description =
                    "POST /projects with whitespace-only description — expect 201 or 400 duplicate (same as TC_001)")
    public void TC_037_Send_POST_request_with_only_spaces_in_description() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        String uniqueName = "DNI Desc Spaces " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with description containing only spaces");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.SPACE_ONLY_DESCRIPTION))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            reportStep("Assert description: API normalizes whitespace-only input to empty (same as omit)");
            assertThat(response.jsonPath().getString("description")).isNullOrEmpty();

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 38,
            description =
                    "POST /projects with description length exactly 255 — expect 201 or 400 duplicate (same as TC_001)")
    public void TC_038_Send_POST_request_with_description_length_exactly_255_characters() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        assertThat(LyticsProjectTestData.DESCRIPTION_EXACTLY_MAX_LENGTH.length())
                .isEqualTo(LyticsProjectTestData.PROJECT_DESCRIPTION_MAX_LENGTH);

        String uniqueName = "DNI Desc 255 " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with description exactly 255 characters");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.DESCRIPTION_EXACTLY_MAX_LENGTH))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.DESCRIPTION_EXACTLY_MAX_LENGTH);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 39,
            description =
                    "POST /projects with description longer than 255 chars — expect 400 MAX_CHAR_LIMIT on errors.description")
    public void TC_039_Send_POST_request_with_description_length_more_than_255_characters() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Precondition: over-max description length is max+1 characters");
        assertThat(LyticsProjectTestData.DESCRIPTION_ONE_OVER_MAX_LENGTH.length())
                .isEqualTo(LyticsProjectTestData.PROJECT_DESCRIPTION_MAX_LENGTH + 1);

        String uniqueName = "DNI Desc Over " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with description length 256+");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.DESCRIPTION_ONE_OVER_MAX_LENGTH))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400, error code MAX_CHAR_LIMIT, and maxCharacters = 255 on errors.description");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> descriptionErrors = response.jsonPath().getList("errors.description");
        assertThat(descriptionErrors).hasSize(1);

        Map<String, Object> descriptionError = descriptionErrors.get(0);
        assertThat(descriptionError.get("code")).isEqualTo("lytics.PROJECTS.MAX_CHAR_LIMIT");
        assertThat(((Number) Objects.requireNonNull(descriptionError.get("maxCharacters"))).intValue())
                .isEqualTo(LyticsProjectTestData.PROJECT_DESCRIPTION_MAX_LENGTH);
    }

    @Test(
            priority = 40,
            description =
                    "POST /projects with `description` explicitly null — expect 201 or 400 duplicate (same envelope as TC_034)")
    public void TC_040_Send_POST_request_with_null_description() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        String uniqueName = "DNI Desc Null " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with description = null");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                                        uniqueName, LyticsProjectTestData.VALID_DOMAIN, null))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            reportStep("Assert description absent or empty when JSON null (same as omit)");
            assertThat(response.jsonPath().getString("description")).isNullOrEmpty();

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 41,
            description =
                    "POST /projects with special characters in description — expect 201 or 400 duplicate (same as TC_001)")
    public void TC_041_Send_POST_request_with_special_characters_in_description() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        String uniqueName = "DNI Desc SpecCh " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with description = @#$$%^&*()");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.DESCRIPTION_SPECIAL_CHARS))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.DESCRIPTION_SPECIAL_CHARS);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 42,
            description =
                    "POST /projects with numeric description — expect 201 or 400 duplicate (same as TC_001)")
    public void TC_042_Send_POST_request_with_numeric_values_in_description() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        String uniqueName = "DNI Desc Num " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with description = 123456");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.DESCRIPTION_NUMERIC_ONLY))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.DESCRIPTION_NUMERIC_ONLY);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 43,
            description =
                    "POST /projects with alphanumeric + special characters in description — expect 201 or 400 duplicate (same as TC_001)")
    public void TC_043_Send_POST_request_with_alphanumeric_and_special_characters_in_description() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        String uniqueName = "DNI Desc AlphaSpec " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with alphanumeric + special description");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.DESCRIPTION_ALPHANUMERIC_SPECIAL))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.DESCRIPTION_ALPHANUMERIC_SPECIAL);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 44,
            description =
                    "POST /projects with very long description including spaces within 255 chars — expect 201 or 400 duplicate (same as TC_001 / TC_038)")
    public void TC_044_Send_POST_request_with_very_long_description_with_spaces_within_255_characters() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        assertThat(LyticsProjectTestData.DESCRIPTION_VERY_LONG_WITH_SPACES_MAX.length())
                .isEqualTo(LyticsProjectTestData.PROJECT_DESCRIPTION_MAX_LENGTH);

        String uniqueName = "DNI Desc LongSp255 " + UUID.randomUUID();

        reportStep("POST " + ApiPaths.PROJECTS + " with description exactly 255 chars (spaced prose)");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.DESCRIPTION_VERY_LONG_WITH_SPACES_MAX))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.DESCRIPTION_VERY_LONG_WITH_SPACES_MAX);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 45,
            description =
                    "POST /projects without `connections` — expect 201 Created or 400 duplicate; response includes empty connection arrays")
    public void TC_045_Send_POST_request_without_connections_field() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        String uniqueName = "DNI NoConn " + UUID.randomUUID();
        String description = "no connection";

        reportStep("POST " + ApiPaths.PROJECTS + " with name, domain, description; connections field omitted");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                                        uniqueName, LyticsProjectTestData.VALID_DOMAIN, description))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description")).isEqualTo(description);

            assertThat(response.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(response.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 46,
            description =
                    "POST /projects with empty `connections` object {} — API returns 500 Internal Server Error (current behavior)")
    public void TC_046_Send_POST_request_with_empty_connections_object() {
        reportStep("Reset cleanup uid (no project should be created on 500)");
        projectUidToCleanup = null;

        String uniqueName = "DNI EmptyConn " + UUID.randomUUID();
        String description = "no connection";

        reportStep("POST " + ApiPaths.PROJECTS + " with name, domain, description, and connections: {}");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithEmptyConnectionsObject(
                                        uniqueName, LyticsProjectTestData.VALID_DOMAIN, description))
                        .when()
                        .post(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description")).isEqualTo(description);

            assertThat(response.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(response.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 47,
            description =
                    "POST /projects with valid stackApiKeys, launchProjectUids, personalizeProjectUids (one value each) — same full payload as TC_001; expect 201 Created or 400 duplicate")
    public void TC_047_Send_POST_request_with_valid_connections_single_value_each() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI FullConn " + UUID.randomUUID();
        reportStep(
                "Build request: POST "
                        + ApiPaths.PROJECTS
                        + " with full valid JSON payload (single stack, launch, personalize IDs per field, same shape as TC_001)");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, connections, CDP status)");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getList("connections.stackApiKeys"))
                    .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getList("connections.launchProjectUids"))
                    .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids"))
                    .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 48,
            description =
                    "POST /projects with each connection array containing the same ID twice — expect 201 and a single deduplicated value per field in the response")
    public void TC_048_Send_POST_request_with_duplicate_connection_ids_deduplicated_in_response() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI TC047 " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with stackApiKeys, launchProjectUids, personalizeProjectUids each listing the same ID twice");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithDuplicateConnectionEntries(uniqueName))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert name, domain, description match payload");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            reportStep("Assert each connections array has exactly one entry (duplicate request values deduplicated)");
            assertThat(response.jsonPath().getList("connections.stackApiKeys"))
                    .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getList("connections.launchProjectUids"))
                    .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids"))
                    .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 49,
            description =
                    "POST /projects with valid stackApiKeys only (empty launch and personalize arrays) — expect 201 or 400 (duplicate name or stack key already connected); response echoes stack key and empty arrays on 201")
    public void TC_049_Send_POST_request_with_only_valid_stackApiKeys_in_connections() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI StackOnly " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: stackApiKeys populated, launchProjectUids and personalizeProjectUids empty");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithConnectionsOnlyStackApiKeys(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate name or duplicate connection)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate name or stack key already linked)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert name, domain, description and connections match payload");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getList("connections.stackApiKeys"))
                    .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();

            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep(
                    "400 path: duplicate project name on errors.name, or stack key already linked on errors.connections.stackApiKeys");
            assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
            assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

            if (response.jsonPath().getList("errors.name") != null
                    && !response.jsonPath().getList("errors.name").isEmpty()) {
                assertThat(response.jsonPath().getString("errors.name[0].code"))
                        .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
            } else {
                assertThat(response.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                        .isEqualTo("lytics.PROJECTS.DUPLICATE_CONNECTION");
            }
        }
    }

    @Test(
            priority = 50,
            description =
                    "POST /projects with valid launchProjectUids only (stackApiKeys and personalizeProjectUids omitted) — expect 201 or 400 (duplicate name or launch already connected); response echoes launch uid on 201")
    public void TC_050_Send_POST_request_with_only_valid_launchProjectUids_in_connections() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI LaunchOnly " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: launchProjectUids populated; stackApiKeys and personalizeProjectUids absent");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithConnectionsOnlyLaunchProjectUids(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate name or duplicate connection)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate name or launch project already linked)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert name, domain, description and connections match payload");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getList("connections.launchProjectUids"))
                    .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(response.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();

            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep(
                    "400 path: duplicate project name on errors.name, or launch project already linked on errors.connections.launchProjectUids");
            assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
            assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

            if (response.jsonPath().getList("errors.name") != null
                    && !response.jsonPath().getList("errors.name").isEmpty()) {
                assertThat(response.jsonPath().getString("errors.name[0].code"))
                        .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
            } else {
                assertThat(response.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                        .isEqualTo("lytics.PROJECTS.DUPLICATE_CONNECTION");
            }
        }
    }

    @Test(
            priority = 51,
            description =
                    "POST /projects with valid personalizeProjectUids only (stackApiKeys and launchProjectUids omitted) — expect 201 or 400 (duplicate name or personalize already connected); response echoes personalize uid on 201")
    public void TC_051_Send_POST_request_with_only_valid_personalizeProjectUids_in_connections() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI PersonalizeOnly " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: personalizeProjectUids populated; stackApiKeys and launchProjectUids absent");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithConnectionsOnlyPersonalizeProjectUids(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate name or duplicate connection)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate name or personalize project already linked)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert name, domain, description and connections match payload");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getList("connections.personalizeProjectUids"))
                    .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
            assertThat(response.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(response.jsonPath().getList("connections.launchProjectUids")).isEmpty();

            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep(
                    "400 path: duplicate project name on errors.name, or personalize project already linked on errors.connections.personalizeProjectUids");
            assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
            assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

            if (response.jsonPath().getList("errors.name") != null
                    && !response.jsonPath().getList("errors.name").isEmpty()) {
                assertThat(response.jsonPath().getString("errors.name[0].code"))
                        .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
            } else {
                assertThat(response.jsonPath().getString("errors['connections.personalizeProjectUids'][0].code"))
                        .isEqualTo("lytics.PROJECTS.DUPLICATE_CONNECTION");
            }
        }
    }

    @Test(
            priority = 52,
            description =
                    "POST /projects with stack + personalize and launchProjectUids mixing valid UID and empty string")
    public void TC_052_Send_POST_request_with_stack_personalize_and_empty_string_in_launchProjectUids() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI LaunchMixEmpty " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: valid stackApiKeys and personalizeProjectUids; launchProjectUids contains"
                        + " valid UID plus empty string");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithStackPersonalizeAndLaunchIncludingEmptyString(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();
        response.prettyPrint();
        reportResponseBody(response);
        reportStep("Assert status is 201 (created) or 400 (duplicate name or duplicate connection)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate name or launch project already linked)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();
            reportStep("Assert response fields match payload (name, domain, description, connections, CDP status)");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);
            assertThat(response.jsonPath().getList("connections.stackApiKeys"))
                    .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids"))
                    .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
            assertThat(response.jsonPath().getList("connections.launchProjectUids"))
                    .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep(
                    "400 path: duplicate project name on errors.name, or launch validation on errors.connections.launchProjectUids"
                            + " (empty uid may be CONNECTION_NOT_FOUND; duplicate use DUPLICATE_CONNECTION)");
            assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
            assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

            if (response.jsonPath().getList("errors.name") != null
                    && !response.jsonPath().getList("errors.name").isEmpty()) {
                assertThat(response.jsonPath().getString("errors.name[0].code"))
                        .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
            } else {
                assertThat(response.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                        .isIn(
                                "lytics.PROJECTS.DUPLICATE_CONNECTION",
                                "lytics.PROJECTS.CONNECTION_NOT_FOUND");
            }
        }
    }

    @Test(
            priority = 53,
            description =
                    "POST /projects with valid launch + personalize + empty stackApiKeys [] — expect 201 or 400 (DUPLICATE_PROJECT_NAME, DUPLICATE_CONNECTION, or CONNECTION_NOT_FOUND)")
    public void TC_053_Send_POST_request_with_launch_personalize_and_empty_stackApiKeys() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI StackMixEmpty " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: launch and personalize UIDs; stackApiKeys empty array []");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithLaunchPersonalizeAndEmptyStackApiKeys(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();
        response.prettyPrint();
        reportResponseBody(response);

        reportStep(
                "Assert status is 201 (created) or 400 (duplicate name, duplicate connection, or connection not found)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as(
                        "Expected 201 (created) or 400 (duplicate name, connection in use, invalid/not found"
                                + " connection)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep(
                    "Assert response fields match successful project create (empty stackApiKeys; launch and personalize"
                            + " echo request)");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);
            assertThat(response.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids"))
                    .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
            assertThat(response.jsonPath().getList("connections.launchProjectUids"))
                    .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep(
                    "400 path: DUPLICATE_PROJECT_NAME on errors.name, or launch / personalize connection errors"
                            + " (DUPLICATE_CONNECTION or CONNECTION_NOT_FOUND)");
            assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
            assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

            if (response.jsonPath().getList("errors.name") != null
                    && !response.jsonPath().getList("errors.name").isEmpty()) {
                assertThat(response.jsonPath().getString("errors.name[0].code"))
                        .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
            } else {
                String stackCode =
                        response.jsonPath().getString("errors['connections.stackApiKeys'][0].code");
                String launchCode =
                        response.jsonPath().getString("errors['connections.launchProjectUids'][0].code");
                String personalizeCode =
                        response.jsonPath().getString("errors['connections.personalizeProjectUids'][0].code");
                String connectionErrorCode =
                        stackCode != null ? stackCode : (launchCode != null ? launchCode : personalizeCode);
                assertThat(connectionErrorCode)
                        .isIn(
                                "lytics.PROJECTS.DUPLICATE_CONNECTION",
                                "lytics.PROJECTS.CONNECTION_NOT_FOUND");
            }
        }
    }

    @Test(
            priority = 54,
            description =
                    "POST /projects with stack + launch + personalizeProjectUids mixing valid UID and empty string — expect 201 or 400 (DUPLICATE_PROJECT_NAME, DUPLICATE_CONNECTION, or CONNECTION_NOT_FOUND)")
    public void TC_054_Send_POST_request_with_stack_launch_and_empty_string_in_personalizeProjectUids() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI PersonalizeMixEmpty " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: valid stackApiKeys and launchProjectUids; personalizeProjectUids contains"
                        + " valid UID plus empty string");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithStackLaunchAndPersonalizeIncludingEmptyString(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();
        response.prettyPrint();
        reportResponseBody(response);

        reportStep(
                "Assert status is 201 (created) or 400 (duplicate name, duplicate connection, or connection not found)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as(
                        "Expected 201 (created) or 400 (duplicate name, connection in use, invalid/not found"
                                + " connection)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep(
                    "Assert response fields match successful project create (stack and launch echo request;"
                            + " personalize list strips empty string; single valid personalize UID)");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);
            assertThat(response.jsonPath().getList("connections.stackApiKeys"))
                    .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getList("connections.launchProjectUids"))
                    .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids"))
                    .isEmpty();
            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else {
            reportStep(
                    "400 path: DUPLICATE_PROJECT_NAME on errors.name, or stack / launch / personalize connection errors"
                            + " (DUPLICATE_CONNECTION or CONNECTION_NOT_FOUND)");
            assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
            assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

            if (response.jsonPath().getList("errors.name") != null
                    && !response.jsonPath().getList("errors.name").isEmpty()) {
                assertThat(response.jsonPath().getString("errors.name[0].code"))
                        .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
            } else {
                String stackCode =
                        response.jsonPath().getString("errors['connections.stackApiKeys'][0].code");
                String launchCode =
                        response.jsonPath().getString("errors['connections.launchProjectUids'][0].code");
                String personalizeCode =
                        response.jsonPath().getString("errors['connections.personalizeProjectUids'][0].code");
                String connectionErrorCode =
                        stackCode != null ? stackCode : (launchCode != null ? launchCode : personalizeCode);
                assertThat(connectionErrorCode)
                        .isIn(
                                "lytics.PROJECTS.DUPLICATE_CONNECTION",
                                "lytics.PROJECTS.CONNECTION_NOT_FOUND");
            }
        }
    }

    @Test(
            priority = 55,
            description =
                    "POST /projects with connections.stackApiKeys, launchProjectUids, personalizeProjectUids each [\"\"]"
                            + " — expect 400 Bad request and CONNECTION_NOT_FOUND on all three")
    public void TC_055_Send_POST_request_with_all_connection_fields_empty_strings() {
        reportStep("Reset cleanup uid; this negative test must not create a project");
        projectUidToCleanup = null;

        String uniqueName = "DNI AllEmptyConn " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with name, domain, description and connections: each array is a single empty string");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithAllConnectionArraysContainingOnlyEmptyStrings(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();
        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400 Bad request with CONNECTION_NOT_FOUND for stack, launch, and personalize");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(response.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
        assertThat(response.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
        assertThat(response.jsonPath().getString("errors['connections.personalizeProjectUids'][0].code"))
                .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
    }

    @Test(
            priority = 56,
            description =
                    "POST /projects with invalid stackApiKeys value (unknown key) and valid launch + personalize UIDs"
                            + " — expect 400 Bad request; CONNECTION_NOT_FOUND on connections.stackApiKeys, or"
                            + " DUPLICATE_PROJECT_NAME if the name collides")
    public void TC_056_Send_POST_request_with_invalid_stackApiKeys_and_valid_launch_personalize() {
        reportStep("Reset cleanup uid; this negative test must not create a project");
        projectUidToCleanup = null;

        String uniqueName = "DNI InvalidStackKey " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: stackApiKeys [invalid], valid launchProjectUids and personalizeProjectUids");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithInvalidStackApiKeyAndValidLaunchPersonalize(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();
        response.prettyPrint();
        reportResponseBody(response);

        reportStep(
                "Assert 400 Bad request: DUPLICATE_PROJECT_NAME on errors.name, or CONNECTION_NOT_FOUND on"
                        + " connections.stackApiKeys");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        if (response.jsonPath().getList("errors.name") != null
                && !response.jsonPath().getList("errors.name").isEmpty()) {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        } else {
            assertThat(response.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                    .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
        }
    }

    @Test(
            priority = 57,
            description =
                    "POST /projects with invalid launchProjectUids (unknown uid, e.g. abc) and valid stack + personalize"
                            + " — expect 400 Bad request; CONNECTION_NOT_FOUND on connections.launchProjectUids, or"
                            + " DUPLICATE_PROJECT_NAME if the name collides")
    public void TC_057_Send_POST_request_with_invalid_launchProjectUids_and_valid_stack_personalize() {
        reportStep("Reset cleanup uid; this negative test must not create a project");
        projectUidToCleanup = null;

        String uniqueName = "DNI InvalidLaunchUid " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: launchProjectUids [invalid], valid stackApiKeys and personalizeProjectUids"
                        + " (same shape as curl with launchProjectUids: [\"abc\"])");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithValidStackPersonalizeAndInvalidLaunchProjectUid(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();
        response.prettyPrint();
        reportResponseBody(response);

        reportStep(
                "Assert 400 Bad request: DUPLICATE_PROJECT_NAME on errors.name, or CONNECTION_NOT_FOUND on"
                        + " connections.launchProjectUids");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        if (response.jsonPath().getList("errors.name") != null
                && !response.jsonPath().getList("errors.name").isEmpty()) {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        } else {
            assertThat(response.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                    .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
        }
    }

    @Test(
            priority = 58,
            description =
                    "POST /projects with invalid personalizeProjectUids (unknown uid, e.g. abc) and valid stack + launch"
                            + " — expect 400 Bad request; CONNECTION_NOT_FOUND on connections.personalizeProjectUids, or"
                            + " DUPLICATE_PROJECT_NAME if the name collides")
    public void TC_058_Send_POST_request_with_invalid_personalizeProjectUids_and_valid_stack_launch() {
        reportStep("Reset cleanup uid; this negative test must not create a project");
        projectUidToCleanup = null;

        String uniqueName = "DNI InvalidPersonalizeUid " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: personalizeProjectUids [invalid], valid stackApiKeys and launchProjectUids"
                        + " (same shape as curl with personalizeProjectUids: [\"abc\"])");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithValidStackLaunchAndInvalidPersonalizeProjectUid(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();
        response.prettyPrint();
        reportResponseBody(response);

        reportStep(
                "Assert 400 Bad request: DUPLICATE_PROJECT_NAME on errors.name, or CONNECTION_NOT_FOUND on"
                        + " connections.personalizeProjectUids");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);

        if (response.jsonPath().getList("errors.name") != null
                && !response.jsonPath().getList("errors.name").isEmpty()) {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        } else {
            assertThat(response.jsonPath().getString("errors['connections.personalizeProjectUids'][0].code"))
                    .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
        }
    }

    @Test(
            priority = 59,
            description =
                    "POST /projects with duplicate values in stackApiKeys array only (same key twice; launch and"
                            + " personalize single ID each) — expect 201 with deduplicated stack key, or 400"
                            + " DUPLICATE_PROJECT_NAME")
    public void TC_059_Send_POST_request_with_duplicate_values_in_stackApiKeys_array_only() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI DupStackOnly " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: stackApiKeys lists the same API key twice; launchProjectUids and"
                        + " personalizeProjectUids each a single UID");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDuplicateStackApiKeysOnly(uniqueName))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert name, domain, description match payload");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            reportStep("Assert stackApiKeys deduplicated to one entry; launch and personalize unchanged");
            assertThat(response.jsonPath().getList("connections.stackApiKeys"))
                    .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getList("connections.launchProjectUids"))
                    .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids"))
                    .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else  if (response.jsonPath().getList("errors.name") != null
                && !response.jsonPath().getList("errors.name").isEmpty()) {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        } else {
            assertThat(response.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                    .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
        } 
    }

    @Test(
            priority = 60,
            description =
                    "POST /projects with non-string connection values (JSON numbers in stackApiKeys, launchProjectUids,"
                            + " personalizeProjectUids arrays) — expect 400 Bad request")
    public void TC_060_Send_POST_request_with_non_string_values_in_connection_arrays() {
        reportStep("Reset cleanup uid; this negative test must not create a project");
        projectUidToCleanup = null;

        String uniqueName = "DNI NonStringConn " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: each array holds a JSON number (not a string ID); equivalent to invalid"
                        + " non-string types where curl uses numbers like [567878]");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithNonStringConnectionValues(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400 Bad request");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
    }

    @Test(
            priority = 61,
            description =
                    "POST /projects with extra unknown field inside connections (e.g. automate) — expect 201"
                            + " Created and response connections without unknown field, or 400 DUPLICATE_PROJECT_NAME")
    public void TC_061_Send_POST_request_with_extra_unknown_field_inside_connections() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI ExtraConnField " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections: stack, launch, personalize plus unknown key automate (curl shape)");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(
                                LyticsProjectPayloadBuilder.projectCreatePayloadWithExtraUnknownFieldInConnections(
                                        uniqueName,
                                        LyticsProjectTestData.VALID_DOMAIN,
                                        LyticsProjectTestData.VALID_DESCRIPTION))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert name, domain, description match payload");
            assertThat(response.jsonPath().getString("name")).isEqualTo(uniqueName);
            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            reportStep("Assert connections has only known fields; unknown automate key not echoed");
            assertThat(response.jsonPath().getString("connections.automate")).isNull();
            assertThat(response.jsonPath().getList("connections.stackApiKeys"))
                    .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(response.jsonPath().getList("connections.launchProjectUids"))
                    .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(response.jsonPath().getList("connections.personalizeProjectUids"))
                    .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

            assertThat(response.jsonPath().getString("cdp.status")).isEqualTo("active");
        } else if (response.jsonPath().getList("errors.name") != null
                && !response.jsonPath().getList("errors.name").isEmpty()) {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        } else {
            assertThat(response.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                    .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
        } 
    }

    @Test(
            priority = 62,
            description =
                    "POST /projects twice with different project names but identical stack, launch, and personalize"
                            + " connections — second request expect 400 DUPLICATE_CONNECTION on connections.stackApiKeys")
    public void TC_062_Send_second_POST_with_same_connections_different_project_name_expect_duplicate_connection() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String firstProjectName = "DNI DupConn A " + UUID.randomUUID();
        reportStep(
                "First POST "
                        + ApiPaths.PROJECTS
                        + " — create project with full connections (stack, launch, personalize)");
        Response first =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(firstProjectName))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        first.prettyPrint();
        reportResponseBody(first);

        reportStep("Assert first request returns 201 so the same connections are bound to this project");
        assertThat(first.getStatusCode())
                .as(
                        "Expected 201 to attach connections to the first project; cannot assert duplicate-connection"
                                + " without a successful create (check env or duplicate project name)")
                .isEqualTo(201);

        projectUidToCleanup = first.jsonPath().getString("uid");
        assertThat(projectUidToCleanup).isNotNull();

        String secondProjectName = "DNI DupConn B " + UUID.randomUUID();
        reportStep(
                "Second POST "
                        + ApiPaths.PROJECTS
                        + " — different name, same stack/launch/personalize connections as first project");
        Response second =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(secondProjectName))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        second.prettyPrint();
        reportResponseBody(second);

        reportStep("Assert 400 Bad request and DUPLICATE_CONNECTION on connections.stackApiKeys");
        assertThat(second.getStatusCode()).isEqualTo(400);
        assertThat(second.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(second.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(second.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                .isEqualTo("lytics.PROJECTS.DUPLICATE_CONNECTION");
    }

    @Test(
            priority = 63,
            description =
                    "POST /projects with stack, launch, and personalize UIDs from another organization — expect 400"
                            + " CONNECTION_NOT_FOUND on each connections field")
    public void TC_063_Send_POST_with_connection_uids_from_different_organization_expect_not_found_each_field() {
        reportStep("Reset cleanup uid; this scenario must not create a project");
        projectUidToCleanup = null;

        String uniqueName = "DNI OtherOrgConn " + UUID.randomUUID();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with connections that belong to a different organization (not resolvable for test org)");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDifferentOrganizationConnections(uniqueName))
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400 Bad request and CONNECTION_NOT_FOUND on stack, launch, and personalize");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(response.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
        assertThat(response.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
        assertThat(response.jsonPath().getString("errors['connections.personalizeProjectUids'][0].code"))
                .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
    }

    @Test(
            priority = 64,
            description =
                    "POST /projects with explicit valid headers x-cs-api-version, organization_uid, and authtoken"
                            + " — expect 201 Created or 400 duplicate name")
    public void TC_064_Send_POST_request_with_all_valid_lytics_headers() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with full valid JSON and headers: x-cs-api-version, organization_uid, authtoken (from"
                        + " TestConfig)");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                        .header("organization_uid", TestConfig.lyticsOrganizationUid())
                        .header("authtoken", TestConfig.lyticsAuthToken())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 65,
            description = "POST /projects without x-cs-api-version header — expect 404 Not Found")
    public void TC_065_Send_POST_request_without_x_cs_api_version_header() {
        reportStep("Reset cleanup uid; missing API version must not create a project");
        projectUidToCleanup = null;

        reportStep("POST " + ApiPaths.PROJECTS + " with organization_uid and authtoken only (no x-cs-api-version)");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("organization_uid", TestConfig.lyticsOrganizationUid())
                        .header("authtoken", TestConfig.lyticsAuthToken())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 404 when x-cs-api-version is omitted");
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test(
            priority = 66,
            description =
                    "POST /projects without organization_uid header — expect 400 with missing required header body")
    public void TC_066_Send_POST_request_without_organization_uid_header() {
        reportStep("Reset cleanup uid; missing organization must not create a project");
        projectUidToCleanup = null;

        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with x-cs-api-version and authtoken only (no organization_uid)");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                        .header("authtoken", TestConfig.lyticsAuthToken())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400 and envelope for missing organization_uid header");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message"))
                .isEqualTo("Missing required header: organization_uid");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(response.jsonPath().getString("error")).isEqualTo("Bad Request");
    }

    @Test(
            priority = 67,
            description = "POST /projects without authtoken header — expect 401 with auth error envelope")
    public void TC_067_Send_POST_request_without_authtoken_header() {
        reportStep("Reset cleanup uid; unauthenticated request must not create a project");
        projectUidToCleanup = null;

        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with x-cs-api-version and organization_uid only (no authtoken)");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                        .header("organization_uid", TestConfig.lyticsOrganizationUid())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 401 Unauthorized and authtoken validation body");
        assertThat(response.getStatusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("error_message"))
                .isEqualTo("You're not allowed in here unless you're logged in.");
        assertThat(response.jsonPath().getInt("error_code")).isEqualTo(105);
        assertThat(response.jsonPath().getString("errors.authtoken[0]")).isEqualTo("is not valid.");
    }

    @Test(
            priority = 68,
            description =
                    "POST /projects with invalid authtoken header — expect 401 Unauthorized with auth error envelope")
    public void TC_068_Send_POST_request_with_invalid_authtoken_header() {
        reportStep("Reset cleanup uid; invalid token must not create a project");
        projectUidToCleanup = null;

        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with valid version and organization but authtoken set to a non-valid value");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                        .header("organization_uid", TestConfig.lyticsOrganizationUid())
                        .header("authtoken", "invalid-authtoken-" + UUID.randomUUID())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 401 Unauthorized and authtoken validation body");
        assertThat(response.getStatusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("error_message"))
                .isEqualTo("You're not allowed in here unless you're logged in.");
        assertThat(response.jsonPath().getInt("error_code")).isEqualTo(105);
        assertThat(response.jsonPath().getString("errors.authtoken[0]")).isEqualTo("is not valid.");
    }

    @Test(
            priority = 69,
            description =
                    "POST /projects with expired authtoken header — expect 401 Unauthorized with auth error envelope")
    public void TC_069_Send_POST_request_with_expired_authtoken_header() {
        Optional<String> expiredToken = TestConfig.lyticsExpiredAuthToken();
        if (expiredToken.isEmpty()) {
            throw new SkipException(
                    "Set lytics.auth.token.expired in config.properties or -Dlytics.auth.token.expired=... to run this"
                            + " test");
        }

        reportStep("Reset cleanup uid; expired token must not create a project");
        projectUidToCleanup = null;

        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with valid version and organization but authtoken from lytics.auth.token.expired");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                        .header("organization_uid", TestConfig.lyticsOrganizationUid())
                        .header("authtoken", expiredToken.get())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 401 Unauthorized and authtoken validation body");
        assertThat(response.getStatusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("error_message"))
                .isEqualTo("You're not allowed in here unless you're logged in.");
        assertThat(response.jsonPath().getInt("error_code")).isEqualTo(105);
        assertThat(response.jsonPath().getString("errors.authtoken[0]")).isEqualTo("is not valid.");
    }

    @Test(
            enabled = false,
            priority = 70,
            description =
                    "POST /projects with invalid organization_uid header (wrong format) — expect 403 Forbidden")
    public void TC_070_Send_POST_request_with_invalid_organization_uid_wrong_format() {
        reportStep("Reset cleanup uid; malformed organization must not create a project");
        projectUidToCleanup = null;

        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with organization_uid that is not a valid UID format (valid version and authtoken)");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                        .header("organization_uid", "test12209876543210")
                        .header("authtoken", TestConfig.lyticsAuthToken())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 403 Forbidden when organization_uid is present but malformed");
        assertThat(response.getStatusCode()).isEqualTo(403);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Forbidden");
    }

    @Test(
            enabled = false,
            priority = 71,
            description =
                    "POST /projects with non-existing organization_uid (valid UID format) — expect 403 Forbidden")
    public void TC_071_Send_POST_request_with_non_existing_organization_uid() {
        reportStep("Reset cleanup uid; unknown organization must not create a project");
        projectUidToCleanup = null;

        String nonExistingOrgUid = TestConfig.lyticsNonExistingOrganizationUid();
        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with organization_uid in valid format but not present in the system ("
                        + nonExistingOrgUid
                        + ")");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                        .header("organization_uid", nonExistingOrgUid)
                        .header("authtoken", TestConfig.lyticsAuthToken())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 403 Forbidden when organization_uid is well-formed but does not exist");
        assertThat(response.getStatusCode()).isEqualTo(403);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Forbidden");
    }

    @Test(
            priority = 72,
            description =
                    "POST /projects with invalid x-cs-api-version header — expect 404 Not Found"
                            + " (Cannot POST /projects envelope)")
    public void TC_072_Send_POST_request_with_invalid_x_cs_api_version_header() {
        reportStep("Reset cleanup uid; invalid API version must not create a project");
        projectUidToCleanup = null;

        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with x-cs-api-version set to an unsupported value (valid organization_uid and authtoken)");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("x-cs-api-version", "999")
                        .header("organization_uid", TestConfig.lyticsOrganizationUid())
                        .header("authtoken", TestConfig.lyticsAuthToken())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 404 and Cannot POST /projects body (no route for unsupported API version)");
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Cannot POST /projects");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(404);
        assertThat(response.jsonPath().getString("error")).isEqualTo("Not Found");
    }

    @Test(
            priority = 73,
            description =
                    "POST /projects with empty x-cs-api-version header — expect 404 Not Found"
                            + " (Cannot POST /projects envelope)")
    public void TC_073_Send_POST_request_with_empty_x_cs_api_version_header() {
        reportStep("Reset cleanup uid; empty API version must not create a project");
        projectUidToCleanup = null;

        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with x-cs-api-version empty string (valid organization_uid and authtoken)");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("x-cs-api-version", "")
                        .header("organization_uid", TestConfig.lyticsOrganizationUid())
                        .header("authtoken", TestConfig.lyticsAuthToken())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 404 and Cannot POST /projects body (empty API version treated as no valid route)");
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("message")).isEqualTo("Cannot POST /projects");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(404);
        assertThat(response.jsonPath().getString("error")).isEqualTo("Not Found");
    }

    @Test(
            priority = 74,
            description =
                    "POST /projects with non-canonical header name casing for x-cs-api-version, organization_uid,"
                            + " and authtoken (HTTP treats names as case-insensitive) — expect 201 or 400 duplicate"
                            + " name")
    public void TC_074_Send_POST_request_with_incorrect_header_name_casing_for_all_three_lytics_headers() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with same values as valid create but header names use uppercase / mixed casing"
                        + " (X-CS-API-VERSION, Organization_Uid, Authtoken)");
        Response response =
                given()
                        .baseUri(TestConfig.lyticsBaseUri())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("X-CS-API-VERSION", TestConfig.lyticsApiVersion())
                        .header("Organization_Uid", TestConfig.lyticsOrganizationUid())
                        .header("Authtoken", TestConfig.lyticsAuthToken())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name); casing must not block auth");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

    @Test(
            priority = 75,
            description =
                    "POST /projects with additional unknown (non-Lytics) headers — expect 201 Created or 400"
                            + " duplicate name; unknown headers must be ignored")
    public void TC_075_Send_POST_request_with_additional_unknown_headers() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        reportStep(
                "POST "
                        + ApiPaths.PROJECTS
                        + " with valid JSON plus extra custom headers (X-Unknown-Test-Header, X-Client-Opaque-Id)"
                        + " that are not part of the Lytics contract");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .header("X-Unknown-Test-Header", "arbitrary-value-" + UUID.randomUUID())
                        .header("X-Client-Opaque-Id", UUID.randomUUID().toString())
                        .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
                                .when()
                        .post(ApiPaths.PROJECTS)
                                .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name); unknown headers must not break create");
        int statusCode = response.getStatusCode();
        assertThat(statusCode)
                .as("Expected 201 (created) or 400 (duplicate project name)")
                .isIn(201, 400);

        if (statusCode == 201) {
            reportStep("201 path: capture project uid for post-test DELETE cleanup");
            projectUidToCleanup = response.jsonPath().getString("uid");
            assertThat(projectUidToCleanup).isNotNull();

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(response.jsonPath().getString("name"))
                    .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);

            assertThat(response.jsonPath().getString("domain"))
                    .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);

            assertThat(response.jsonPath().getString("description"))
                    .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);

            assertThat(response.jsonPath().getString("connections.stackApiKeys[0]"))
                    .isEqualTo(LyticsProjectTestData.STACK_API_KEY);

            assertThat(response.jsonPath().getString("cdp.status"))
                    .isEqualTo("active");
        } else {
            assertThat(response.jsonPath().getString("errors.name[0].code"))
                    .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        }
    }

}
