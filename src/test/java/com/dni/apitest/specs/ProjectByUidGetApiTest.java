package com.dni.apitest.specs;

import com.dni.apitest.assertions.ProjectAssertions;
import com.dni.apitest.base.BaseApiTest;
import com.dni.apitest.config.TestConfig;
import com.dni.apitest.constants.ApiPaths;
import com.dni.apitest.testdata.LyticsProjectPayloadBuilder;
import com.dni.apitest.testdata.LyticsProjectTestData;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * GET /projects/{uid} — single-project fetch.
 * The first case mirrors {@link ProjectPostApiTest#TC_001_Send_POST_request_with_valid_project_name}:
 * POST /projects, read {@code uid} from the response, then GET /projects/{uid}.
 */
public class ProjectByUidGetApiTest extends BaseApiTest {

    @Test(
        priority = 1,
        description = "POST /projects (same as TC_001) then GET /projects/{uid} with uid from POST "
            + "— expect 200; body matches list-item schema (400 duplicate: POST with unique name then GET)"
    )
    public void TC_GET_BY_UID_001_POST_then_GET_single_project_by_uid_from_response() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        // -------------------- POST: Create Project --------------------
        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with full valid JSON payload");

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayload())
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert status is 201 (created) or 400 (duplicate project name)");
        int statusCode = postResponse.getStatusCode();
        assertThat(statusCode)
            .as("Expected 201 (created) or 400 (duplicate project name)")
            .isIn(201, 400);

        String projectUid;
        String expectedName;
        String expectedDomain = LyticsProjectTestData.VALID_DOMAIN;
        String expectedDescription = LyticsProjectTestData.VALID_DESCRIPTION;

        if (statusCode == 201) {
            reportStep("201 path: capture project uid from POST response for GET /projects/{uid}");
            projectUid = postResponse.jsonPath().getString("uid");
            assertThat(projectUid).isNotNull();
            expectedName = LyticsProjectTestData.VALID_PROJECT_NAME;

            reportStep("Assert response fields match payload (name, domain, description, stack key, CDP status)");
            assertThat(postResponse.jsonPath().getString("name"))
                .isEqualTo(LyticsProjectTestData.VALID_PROJECT_NAME);
            assertThat(postResponse.jsonPath().getString("domain"))
                .isEqualTo(LyticsProjectTestData.VALID_DOMAIN);
            assertThat(postResponse.jsonPath().getString("description"))
                .isEqualTo(LyticsProjectTestData.VALID_DESCRIPTION);
            assertThat(postResponse.jsonPath().getString("connections.stackApiKeys[0]"))
                .isEqualTo(LyticsProjectTestData.STACK_API_KEY);
            assertThat(postResponse.jsonPath().getString("cdp.status"))
                .isEqualTo("active");
        } else {
            reportStep("400 path: assert duplicate project name error code");
            assertThat(postResponse.jsonPath().getString("errors.name[0].code"))
                .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");

            reportStep("POST again with unique name to obtain a uid for GET /projects/{uid}");
            String uniqueName = LyticsProjectTestData.VALID_PROJECT_NAME + " " + UUID.randomUUID();

            Response secondPost = given()
                .spec(lyticsRequestSpec)
                .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

            secondPost.prettyPrint();
            reportResponseBody(secondPost);

            assertThat(secondPost.getStatusCode())
                .as("Second POST /projects must return 201 to obtain a uid")
                .isEqualTo(201);

            projectUid = secondPost.jsonPath().getString("uid");
            assertThat(projectUid).isNotBlank();
            expectedName = uniqueName;
        }

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid) + " with valid Lytics headers");

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert HTTP 200");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with valid auth and organization headers")
            .isEqualTo(200);

        reportStep("Assert GET /projects/{uid} echoes the same data as POST (name, domain, description, connections, org)");
        ProjectAssertions.assertGetByUidResponseMatchesPostPayload(
            getResponse,
            projectUid,
            expectedName,
            expectedDomain,
            expectedDescription
        );

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 2,
        description = "POST /projects with unique name then GET /projects/{uid} — uid present, non-empty, "
            + "24 hex chars on POST and GET; GET uid equals POST uid"
    )
    public void TC_GET_BY_UID_002_POST_then_GET_verify_uid_field_present_nonempty_format() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID uid field " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with unique project name");

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and uid field is present, non-empty, well-formed");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        ProjectAssertions.assertLyticsProjectUidField(postResponse, "POST /projects");
        String projectUid = postResponse.jsonPath().getString("uid");

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200 and uid matches POST; field present, non-empty, well-formed");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        ProjectAssertions.assertLyticsProjectUidField(getResponse, "GET /projects/{uid}");

        assertThat(getResponse.jsonPath().getString("uid"))
            .as("GET body uid must equal uid returned by POST")
            .isEqualTo(projectUid);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 3,
        description = "GET /projects/{uid} with invalid/non-existing uid (non-hex path segment) "
            + "— expect 404 Project not found envelope"
    )
    public void TC_GET_BY_UID_003_GET_single_project_invalid_uid_expect_404_project_not_found() {
        reportStep("No project created; skip DELETE cleanup");
        projectUidToCleanup = null;

        String invalidUid = "invaliduid";

        // -------------------- GET: Invalid UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(invalidUid) + " (invalid uid, same as manual curl)");

        Response response = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(invalidUid))
            .then()
            .extract()
            .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 404 and Project not found body");
        ProjectAssertions.assertProjectNotFound(response);
    }

    @Test(
        priority = 4,
        description = "GET /projects/{uid} with well-formed 24-hex uid that does not exist "
            + "— expect 404 Project not found envelope"
    )
    public void TC_GET_BY_UID_004_GET_single_project_nonexistent_hex_uid_expect_404_project_not_found() {
        reportStep("No project created; skip DELETE cleanup");
        projectUidToCleanup = null;

        String nonexistentHexUid = "deadbeefdeadbeefdeadbeef";

        // -------------------- GET: Non-existent UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(nonexistentHexUid));

        Response response = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(nonexistentHexUid))
            .then()
            .extract()
            .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 404 and Project not found body");
        ProjectAssertions.assertProjectNotFound(response);
    }

    @Test(
        priority = 5,
        description = "POST /projects with a unique name then GET /projects/{uid} — GET name must equal "
            + "the name returned by POST and the name sent in the create payload"
    )
    public void TC_GET_BY_UID_005_POST_then_GET_verify_name_field_matches_create() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI Unique Name " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with unique project name");

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and name in response matches the payload");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        assertThat(postResponse.jsonPath().getString("name"))
            .as("POST response name must echo the name from the create request")
            .isEqualTo(uniqueName);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200 and name matches POST / create payload");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        assertThat(getResponse.jsonPath().getString("name"))
            .as("GET name must match create payload and POST response name")
            .isEqualTo(uniqueName)
            .isEqualTo(postResponse.jsonPath().getString("name"));

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 6,
        description = "POST /projects with a unique name and explicit domain then GET /projects/{uid} "
            + "— GET domain must equal the domain returned by POST and the domain sent in the create payload"
    )
    public void TC_GET_BY_UID_006_POST_then_GET_verify_domain_field_matches_create() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI Test " + UUID.randomUUID();
        String expectedDomain = LyticsProjectTestData.SUBDOMAIN_EXAMPLE_COM;

        // -------------------- POST: Create Project --------------------
        reportStep(
            "Build request: POST " + ApiPaths.PROJECTS
                + " with unique name and domain " + expectedDomain
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameAndDomain(
                uniqueName,
                expectedDomain
            ))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and domain in response matches the payload");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name and valid domain must return 201")
            .isEqualTo(201);

        assertThat(postResponse.jsonPath().getString("domain"))
            .as("POST response domain must echo the domain from the create request")
            .isEqualTo(expectedDomain);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200 and domain matches POST / create payload");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        assertThat(getResponse.jsonPath().getString("domain"))
            .as("GET domain must match create payload and POST response domain")
            .isEqualTo(expectedDomain)
            .isEqualTo(postResponse.jsonPath().getString("domain"));

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 7,
        description = "POST /projects with a unique name and explicit description then GET /projects/{uid} "
            + "— GET description must equal the description returned by POST and the value sent in create"
    )
    public void TC_GET_BY_UID_007_POST_then_GET_verify_description_field_matches_create() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI Test " + UUID.randomUUID();
        String expectedDescription = "DNI description body " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep(
            "Build request: POST " + ApiPaths.PROJECTS
                + " with unique name, default valid domain, and explicit description"
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                uniqueName,
                LyticsProjectTestData.VALID_DOMAIN,
                expectedDescription
            ))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and description in response matches the payload");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name and valid description must return 201")
            .isEqualTo(201);

        assertThat(postResponse.jsonPath().getString("description"))
            .as("POST response description must echo the description from the create request")
            .isEqualTo(expectedDescription);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200 and description matches POST / create payload");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        assertThat(getResponse.jsonPath().getString("description"))
            .as("GET description must match create payload and POST response description")
            .isEqualTo(expectedDescription)
            .isEqualTo(postResponse.jsonPath().getString("description"));

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 8,
        description = "POST /projects (organization_uid header) then GET /projects/{uid} "
            + "— response organizationUid must match the configured organization header on POST and GET"
    )
    public void TC_GET_BY_UID_008_POST_then_GET_verify_organization_uid_field_matches_header() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String expectedOrganizationUid = TestConfig.lyticsOrganizationUid();
        String uniqueName = "DNI GET by UID org " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep(
            "Build request: POST " + ApiPaths.PROJECTS
                + " with unique name; organization_uid header is " + expectedOrganizationUid
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and organizationUid in body matches organization_uid header");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        assertThat(postResponse.jsonPath().getString("organizationUid"))
            .as("POST response organizationUid must match organization_uid request header")
            .isEqualTo(expectedOrganizationUid);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid) + " with same Lytics headers");

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200 and organizationUid matches POST and configured header");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        assertThat(getResponse.jsonPath().getString("organizationUid"))
            .as("GET organizationUid must match organization_uid header and POST response")
            .isEqualTo(expectedOrganizationUid)
            .isEqualTo(postResponse.jsonPath().getString("organizationUid"));

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 9,
        description = "POST /projects with default connections then GET /projects/{uid} — GET connections object "
            + "(stackApiKeys, launchProjectUids, personalizeProjectUids) must match POST response"
    )
    public void TC_GET_BY_UID_009_POST_then_GET_verify_connections_object_matches_post() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID connections " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with unique name and default connections");

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and connections match create payload (default test org IDs)");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200 and connections object matches POST");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        ProjectAssertions.assertGetConnectionsMatchPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 10,
        description = "POST /projects with default connections then GET /projects/{uid} — verify only "
            + "connections.stackApiKeys: must match create payload on POST and match POST on GET"
    )
    public void TC_GET_BY_UID_010_POST_then_GET_verify_stack_api_keys_array_only() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID stackApiKeys " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with unique name and default connections");

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and connections.stackApiKeys matches create payload");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys"))
            .as("POST connections.stackApiKeys must echo default stack key from create request")
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200 and connections.stackApiKeys matches POST (only this array asserted)");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .as("GET connections.stackApiKeys must match default stack key from create payload")
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);

        ProjectAssertions.assertGetStackApiKeysMatchPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 11,
        description = "POST /projects with default connections then GET /projects/{uid} — verify only "
            + "connections.launchProjectUids: must match create payload on POST and match POST on GET"
    )
    public void TC_GET_BY_UID_011_POST_then_GET_verify_launch_project_uids_array_only() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID launchProjectUids " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with unique name and default connections");

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and connections.launchProjectUids matches create payload");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids"))
            .as("POST connections.launchProjectUids must echo default launch UID from create request")
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200 and connections.launchProjectUids matches POST (only this array asserted)");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .as("GET connections.launchProjectUids must match default launch UID from create payload")
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);

        ProjectAssertions.assertGetLaunchProjectUidsMatchPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 12,
        description = "POST /projects with default connections then GET /projects/{uid} — verify only "
            + "connections.personalizeProjectUids: must match create payload on POST and match POST on GET"
    )
    public void TC_GET_BY_UID_012_POST_then_GET_verify_personalize_project_uids_array_only() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID personalizeProjectUids " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with unique name and default connections");

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and connections.personalizeProjectUids matches create payload");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .as("POST connections.personalizeProjectUids must echo default personalize UID from create request")
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200 and connections.personalizeProjectUids matches POST (only this array asserted)");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .as("GET connections.personalizeProjectUids must match default personalize UID from create payload")
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        ProjectAssertions.assertGetPersonalizeProjectUidsMatchPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 13,
        description = "POST /projects with projectCreatePayloadWithDuplicateStackApiKeysOnly (same stack key twice) "
            + "then GET /projects/{uid} — stackApiKeys must be deduplicated to one entry; all connection arrays "
            + "must have no duplicate values (see ProjectPostApiTest#TC_059_Send_POST_request_with_duplicate_values_in_stackApiKeys_array_only)"
    )
    public void TC_GET_BY_UID_013_POST_duplicate_stack_keys_then_GET_verifies_no_duplicate_connection_entries() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET dup stack keys " + UUID.randomUUID();

        // -------------------- POST: Create Project with Duplicate Stack Keys --------------------
        reportStep(
            "Build request: POST " + ApiPaths.PROJECTS
                + " with duplicate stackApiKeys in payload (same key twice); launch and personalize single ID"
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDuplicateStackApiKeysOnly(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST is 201 or 400 duplicate name; on duplicate name, POST again with a fresh name");
        int postStatus = postResponse.getStatusCode();
        assertThat(postStatus)
            .as("Expected 201 (created) or 400 (duplicate project name), same as TC_059")
            .isIn(201, 400);

        if (postStatus == 400) {
            List<?> nameErrors = postResponse.jsonPath().getList("errors.name");
            assertThat(nameErrors)
                .as("400 without errors.name is unexpected for duplicate-stack payload with unique name")
                .isNotNull();
            assertThat(nameErrors).isNotEmpty();
            assertThat(postResponse.jsonPath().getString("errors.name[0].code"))
                .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");

            uniqueName = "DNI GET dup stack keys " + UUID.randomUUID();

            postResponse = given()
                .spec(lyticsRequestSpec)
                .body(LyticsProjectPayloadBuilder.projectCreatePayloadWithDuplicateStackApiKeysOnly(uniqueName))
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

            postResponse.prettyPrint();
            reportResponseBody(postResponse);
        }

        assertThat(postResponse.getStatusCode())
            .as("POST with duplicate stack keys in body must succeed once project name is unique")
            .isEqualTo(201);

        reportStep("Assert POST deduplicated stackApiKeys to a single entry; no duplicate values in any connection array");
        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        ProjectAssertions.assertConnectionsArraysHaveNoDuplicateEntries(postResponse);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200; connections match POST; no duplicate entries in any connection array");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        ProjectAssertions.assertConnectionsArraysHaveNoDuplicateEntries(getResponse);
        ProjectAssertions.assertGetConnectionsMatchPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 14,
        description = "POST /projects then GET /projects/{uid} — verify cdp object is present with aid, orgId, "
            + "accountId, status (active), and syncedAt (ISO-8601); same checks on POST 201 and GET 200"
    )
    public void TC_GET_BY_UID_014_POST_then_GET_verify_cdp_object_present() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID cdp " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with unique name");

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and cdp object matches schema (aid, orgId, accountId, status, syncedAt)");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        ProjectAssertions.assertSingleProjectCdpObjectMatchesSchema(postResponse);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200 and cdp object matches same schema as POST");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        ProjectAssertions.assertSingleProjectCdpObjectMatchesSchema(getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 15,
        description = "POST /projects then GET /projects/{uid} — verify cdp.aid is a four-digit integer (1000–9999) "
            + "and GET cdp.aid equals POST (stable per project)"
    )
    public void TC_GET_BY_UID_015_POST_then_GET_verify_cdp_aid_four_digits_and_matches_post() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID cdp aid " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with unique name");

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201 and cdp.aid is a four-digit number");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET returns 200; cdp.aid four-digit on both; GET aid equals POST aid");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        ProjectAssertions.assertGetCdpAidMatchesPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 16,
        description = "POST /projects (cdp.orgId is not in the request body — API assigns it) then GET /projects/{uid} "
            + "— verify cdp.orgId is 32 hex chars on POST 201 and GET (e.g. 930afa61e6720d4223efac8761bd9c39), "
            + "and GET equals POST"
    )
    public void TC_GET_BY_UID_016_POST_then_GET_verify_cdp_org_id_matches_post() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID cdp orgId " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep(
            "Build request: POST " + ApiPaths.PROJECTS
                + " with unique name (create payload has no cdp.orgId; API returns it on 201)"
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201; cdp.orgId will be asserted from response body (not from request)");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep(
            "Build request: GET " + ApiPaths.projectByUid(projectUid)
                + " — fetch same project; orgId must match create response"
        );

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET 200; cdp.orgId is 32 hex chars on POST 201 and GET; GET orgId equals server value from POST response");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        ProjectAssertions.assertGetCdpOrgIdMatchesPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 17,
        description = "POST /projects (cdp.accountId is not in the request body — API assigns it) then GET "
            + "/projects/{uid} — verify cdp.accountId is 32 hex chars on POST 201 and GET "
            + "(e.g. 99c7c1fce9bd1d947f7c7d6d88f4deea), and GET equals POST"
    )
    public void TC_GET_BY_UID_017_POST_then_GET_verify_cdp_account_id_matches_post() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID cdp accountId " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep(
            "Build request: POST " + ApiPaths.PROJECTS
                + " with unique name (create payload has no cdp.accountId; API returns it on 201)"
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201; cdp.accountId will be asserted from response body (not from request)");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep(
            "Build request: GET " + ApiPaths.projectByUid(projectUid)
                + " — fetch same project; accountId must match create response"
        );

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET 200; cdp.accountId is 32 hex chars on POST 201 and GET; GET accountId equals server value from POST response");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        ProjectAssertions.assertGetCdpAccountIdMatchesPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 18,
        description = "POST /projects (cdp.status is not in the request body — API returns it) then GET /projects/{uid} "
            + "— verify cdp.status is active on POST 201 and GET, and GET equals POST"
    )
    public void TC_GET_BY_UID_018_POST_then_GET_verify_cdp_status_matches_post() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID cdp status " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep(
            "Build request: POST " + ApiPaths.PROJECTS
                + " with unique name (create payload has no cdp.status; API returns it on 201)"
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201; cdp.status will be asserted from response body");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid) + " — cdp.status must match create response");

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET 200; cdp.status active on POST 201 and GET; GET status equals POST");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        ProjectAssertions.assertGetCdpStatusMatchesPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 19,
        description = "POST /projects then GET /projects/{uid} — verify cdp.syncedAt is ISO-8601 parseable "
            + "on POST 201 and GET (e.g. 2026-04-13T05:04:23.758Z)"
    )
    public void TC_GET_BY_UID_019_POST_then_GET_verify_cdp_synced_at_iso8601_format() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID cdp syncedAt " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep(
            "Build request: POST " + ApiPaths.PROJECTS
                + " with unique name (cdp.syncedAt is API-assigned; validate format on 201 and after GET)"
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET 200; cdp.syncedAt on POST 201 and GET is non-blank ISO-8601 (Instant.parse)");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        ProjectAssertions.assertGetCdpSyncedAtFormat(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 20,
        description = "POST /projects then GET /projects/{uid} — verify createdBy is non-blank on POST 201 and GET 200, "
            + "and GET equals POST (e.g. blt7a752c371e16a089)"
    )
    public void TC_GET_BY_UID_020_POST_then_GET_verify_created_by_matches_post() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID createdBy " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep(
            "Build request: POST " + ApiPaths.PROJECTS
                + " with unique name (createdBy is API-assigned; assert on 201 and after GET)"
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep("Assert GET 200; createdBy on POST 201 and GET is non-blank; GET createdBy equals POST");
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        ProjectAssertions.assertGetCreatedByMatchesPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }

    @Test(
        priority = 21,
        description = "POST /projects then GET /projects/{uid} — capture createdAt from POST 201 (ISO-8601 "
            + "e.g. 2026-04-13T05:04:13.635Z); GET must return the same createdAt"
    )
    public void TC_GET_BY_UID_021_POST_then_GET_verify_created_at_matches_post() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String uniqueName = "DNI GET by UID createdAt " + UUID.randomUUID();

        // -------------------- POST: Create Project --------------------
        reportStep("Build request: POST " + ApiPaths.PROJECTS + " with unique name");

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithName(uniqueName))
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        reportStep("Assert POST returns 201");
        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201")
            .isEqualTo(201);

        String createdAtFromPost = postResponse.jsonPath().getString("createdAt");
        reportStep(
            "Capture createdAt from POST 201 (ISO-8601 instant string, e.g. 2026-04-13T05:04:13.635Z): "
                + createdAtFromPost
        );

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        // -------------------- GET: Fetch Project by UID --------------------
        reportStep("Build request: GET " + ApiPaths.projectByUid(projectUid));

        Response getResponse = given()
            .spec(lyticsRequestSpec)
            .when()
            .get(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        reportStep(
            "Assert GET 200; createdAt on POST and GET is ISO-8601 (Instant.parse); GET createdAt equals POST "
                + "(same as captured value: " + createdAtFromPost + ")"
        );
        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} with uid from POST")
            .isEqualTo(200);

        ProjectAssertions.assertGetCreatedAtMatchesPost(postResponse, getResponse);

        reportStep("Register uid for DELETE cleanup after this test");
        projectUidToCleanup = projectUid;
    }
}