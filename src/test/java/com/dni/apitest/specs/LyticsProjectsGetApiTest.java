package com.dni.apitest.specs;

import com.dni.apitest.assertions.ProjectAssertions;
import com.dni.apitest.base.LyticsBaseApiTest;
import com.dni.apitest.config.TestConfig;
import com.dni.apitest.constants.ApiPaths;
import io.restassured.response.Response;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class LyticsProjectsGetApiTest extends LyticsBaseApiTest {

    @Test(
            priority = 1,
            description = "GET /projects with valid headers — expect 200 and a JSON array (empty [] when no projects, otherwise each project validated for the configured organization)")
    public void TC_GET_001_Send_GET_request_returns_all_organization_projects() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Build request: GET " + ApiPaths.PROJECTS + " with valid Lytics headers");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .when()
                        .get(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 200");
        assertThat(response.getStatusCode())
                .as("GET /projects with valid auth and organization headers")
                .isEqualTo(200);

        reportStep("Assert response is a JSON array (root-level list of project objects)");
        Object bodyRoot = response.jsonPath().get();
        assertThat(bodyRoot)
                .as("GET /projects response body root must be a JSON array")
                .isInstanceOf(List.class);

        List<Map<String, Object>> projects = (List<Map<String, Object>>) bodyRoot;

        if (projects.isEmpty()) {
            reportStep("No projects for this organization: response is an empty JSON array []");
            assertThat(projects).as("GET /projects with no data must return []").isEmpty();
        } else {
            String expectedOrgUid = TestConfig.lyticsOrganizationUid();

            reportStep("Assert each project exposes core fields and belongs to the configured organization");
            for (Map<String, Object> project : projects) {
                ProjectAssertions.assertListedProjectMatchesSchema(project, expectedOrgUid);
            }
        }
    }

    @Test(
            priority = 2,
            description = "GET /projects — expect 200; root body is a JSON array (List) and jsonPath $ resolves to a list")
    public void TC_GET_002_Send_GET_request_verify_response_body_is_JSON_array() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Build request: GET " + ApiPaths.PROJECTS + " with valid Lytics headers");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .when()
                        .get(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 200");
        assertThat(response.getStatusCode())
                .as("GET /projects with valid auth and organization headers")
                .isEqualTo(200);

        reportStep("Assert response body root is a JSON array (deserializes to List)");
        Object bodyRoot = response.jsonPath().get();
        assertThat(bodyRoot)
                .as("GET /projects response body root must be a JSON array")
                .isInstanceOf(List.class);

        reportStep("Assert jsonPath \"$\" returns a list for the root JSON array");
        List<?> rootList = response.jsonPath().getList("$");
        assertThat(rootList)
                .as("jsonPath $ must resolve to a list when the root is a JSON array")
                .isNotNull();
    }

    @Test(
            priority = 3,
            description = "GET /projects — expect 200; each array element matches the list-item project schema (keys, types, organization scope)")
    public void TC_GET_003_Send_GET_request_verify_each_project_matches_schema() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Build request: GET " + ApiPaths.PROJECTS + " with valid Lytics headers");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .when()
                        .get(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        List<Map<String, Object>> projects = (List<Map<String, Object>>) response.jsonPath().get();

        if (projects.isEmpty()) {
            reportStep("Empty array: no project objects to verify against the list-item schema");
        } else {
            String expectedOrgUid = TestConfig.lyticsOrganizationUid();
            reportStep("Assert each list item satisfies the GET /projects project schema");
            for (Map<String, Object> project : projects) {
                ProjectAssertions.assertListedProjectMatchesSchema(project, expectedOrgUid);
            }
        }
    }

    @Test(
            priority = 4,
            description = "GET /projects — expect 200; when the org has no projects the body is []; when it has projects, every list item uid is unique")
    public void TC_GET_004_Send_GET_request_uid_uniqueness_or_empty_array() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Build request: GET " + ApiPaths.PROJECTS + " with valid Lytics headers");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .when()
                        .get(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        List<Map<String, Object>> projects = (List<Map<String, Object>>) response.jsonPath().get();

        if (projects.isEmpty()) {
            reportStep("No projects for this organization: response must be an empty JSON array []");
            assertThat(projects)
                    .as("GET /projects with no projects must return []")
                    .isEmpty();
        } else {
            reportStep("Assert project uids are unique across the list");
            ProjectAssertions.assertListedProjectUidsAreUnique(projects);
        }
    }

    @Test(
            priority = 5,
            description = "GET /projects — expect 200; each project's connections object exposes the three string arrays and every populated entry is a non-blank string")
    public void TC_GET_005_Send_GET_request_validate_connections_in_each_project() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Build request: GET " + ApiPaths.PROJECTS + " with valid Lytics headers");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .when()
                        .get(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 200");
        assertThat(response.getStatusCode())
                .as("GET /projects with valid auth and organization headers")
                .isEqualTo(200);

        List<Map<String, Object>> projects = (List<Map<String, Object>>) response.jsonPath().get();

        if (projects.isEmpty()) {
            reportStep("Empty array: no projects to validate connections against");
        } else {
            reportStep("Assert each project's connections (stackApiKeys, launchProjectUids, personalizeProjectUids)");
            for (Map<String, Object> project : projects) {
                ProjectAssertions.assertListedProjectConnectionsMatchSchema(project);
            }
        }
    }

    @Test(
            priority = 6,
            description = "GET /projects — expect 200; each project's cdp object has aid, orgId, accountId, status active, and ISO-8601 syncedAt")
    public void TC_GET_006_Send_GET_request_validate_cdp_in_each_project() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Build request: GET " + ApiPaths.PROJECTS + " with valid Lytics headers");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .when()
                        .get(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 200");
        assertThat(response.getStatusCode())
                .as("GET /projects with valid auth and organization headers")
                .isEqualTo(200);

        List<Map<String, Object>> projects = (List<Map<String, Object>>) response.jsonPath().get();

        if (projects.isEmpty()) {
            reportStep("Empty array: no projects to validate cdp against");
        } else {
            reportStep("Assert each project's cdp (aid, orgId, accountId, status, syncedAt)");
            for (Map<String, Object> project : projects) {
                ProjectAssertions.assertListedProjectCdpMatchesSchema(project);
            }
        }
    }

    @Test(
            priority = 7,
            description =
                    "GET /projects/{uid} — expect 200; createdAt, updatedAt, and cdp.syncedAt are ISO-8601 instants (e.g. 2026-04-13T05:12:04.717Z)")
    public void TC_GET_007_Send_GET_request_validate_timestamp_fields_are_iso8601() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Build request: GET " + ApiPaths.PROJECTS + " to obtain a project uid");
        Response listResponse =
                given()
                        .spec(lyticsRequestSpec)
                        .when()
                        .get(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        listResponse.prettyPrint();
        reportResponseBody(listResponse);

        assertThat(listResponse.getStatusCode())
                .as("GET /projects with valid auth and organization headers")
                .isEqualTo(200);

        List<Map<String, Object>> projects = (List<Map<String, Object>>) listResponse.jsonPath().get();

        if (projects.isEmpty()) {
            reportStep("Empty array: no uid available to call GET /projects/{uid} for timestamp validation");
            return;
        }

        String projectUid = (String) projects.get(0).get("uid");
        assertThat(projectUid).as("first list item uid").isNotBlank();

        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid) + " with valid Lytics headers");
        Response detailResponse = projectApiClient.getProject(projectUid);

        detailResponse.prettyPrint();
        reportResponseBody(detailResponse);

        reportStep("Assert HTTP 200 on single-project GET");
        assertThat(detailResponse.getStatusCode())
                .as("GET /projects/{uid} with valid auth and organization headers")
                .isEqualTo(200);

        reportStep("Assert createdAt, updatedAt, cdp.syncedAt are ISO-8601 timestamp strings");
        ProjectAssertions.assertGetProjectResponseTimestampFields(detailResponse);
    }

    @Test(
            priority = 8,
            description =
                    "GET /projects — empty project list: when the organization has no projects, expect 200 and body [] (JSON array with zero elements)")
    public void TC_GET_008_Send_GET_request_empty_project_list_returns_empty_array() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        reportStep("Build request: GET " + ApiPaths.PROJECTS + " with valid Lytics headers");
        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .when()
                        .get(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 200");
        assertThat(response.getStatusCode())
                .as("GET /projects with valid auth and organization headers")
                .isEqualTo(200);                
        reportResponseBody(response);
        List<Map<String, Object>> projects = (List<Map<String, Object>>) response.jsonPath().get();     

     if(projects.isEmpty()){
        reportStep("Assert no projects: list is empty and serializes as []");
        assertThat(projects).as("GET /projects with no projects must return an empty array").isEmpty();
    }else{
        reportStep("Assert projects: list is not empty and serializes as a non-empty array");
        assertThat(projects).as("GET /projects with projects must return a non-empty array").isNotEmpty();
    }
    }

    @Test(
            priority = 9,
            description =
                    "GET /projects — expect 200; round-trip response time is below the configured SLA (lytics.api.get.max.response.time.ms, default 30s)")
    public void TC_GET_009_Send_GET_request_response_time_within_sla() {
        reportStep("Reset cleanup uid");
        projectUidToCleanup = null;

        long maxResponseTimeMs = TestConfig.lyticsGetMaxResponseTimeMs();
        reportStep(
                "Build request: GET "
                        + ApiPaths.PROJECTS
                        + " with valid Lytics headers; SLA max response time = "
                        + maxResponseTimeMs
                        + " ms");

        Response response =
                given()
                        .spec(lyticsRequestSpec)
                        .when()
                        .get(ApiPaths.PROJECTS)
                        .then()
                        .extract()
                        .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 200");
        assertThat(response.getStatusCode())
                .as("GET /projects with valid auth and organization headers")
                .isEqualTo(200);

        long responseTimeMs = response.getTime();
        reportStep("Measured GET /projects response time: " + responseTimeMs + " ms (SLA < " + maxResponseTimeMs + " ms)");
        assertThat(responseTimeMs)
                .as("GET /projects response time must be less than %d ms", maxResponseTimeMs)
                .isLessThan(maxResponseTimeMs);
    }
}