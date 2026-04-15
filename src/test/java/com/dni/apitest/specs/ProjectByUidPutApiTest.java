package com.dni.apitest.specs;

import com.dni.apitest.assertions.ProjectAssertions;
import com.dni.apitest.base.BaseApiTest;
import com.dni.apitest.config.TestConfig;
import com.dni.apitest.constants.ApiPaths;
import com.dni.apitest.testdata.LyticsProjectPayloadBuilder;
import com.dni.apitest.testdata.LyticsProjectTestData;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;


public class ProjectByUidPutApiTest extends BaseApiTest {

   @Test(
    priority = 1,
    description = "POST /projects (curl-shaped body) then PUT with updated name only "
        + "(same domain/description/connections) — given/when/then; GET — assert only name"
)
public void TC_PUT_BY_UID_001_POST_then_PUT_valid_request_update_name_only() {
    reportStep("Reset cleanup uid so this run does not delete an unrelated project");
    projectUidToCleanup = null;

    String suffix = UUID.randomUUID().toString();
    String initialName = "DNI Test " + suffix;
    String updatedName = "DNI Test updated " + suffix;

    Map<String, Object> postBody =
        LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
            initialName,
            LyticsProjectTestData.VALID_DOMAIN,
            LyticsProjectTestData.VALID_DESCRIPTION
        );

    // -------------------- POST: Create Project --------------------
    reportStep(
        "Given: Lytics headers + JSON body matching POST curl; "
            + "When: POST " + ApiPaths.PROJECTS + "; "
            + "Then: extract response"
    );

    Response postResponse = given()
        .spec(lyticsRequestSpec)
        .body(postBody)
        .when()
        .post(ApiPaths.PROJECTS)
        .then()
        .extract()
        .response();

    postResponse.prettyPrint();
    reportResponseBody(postResponse);

    int postStatus = postResponse.getStatusCode();

    if (postStatus == 400
        && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
            postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
        )) {
        throw new SkipException(
            "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                + "project in this org. Delete the other project (or free the connection) "
                + "so this curl-shaped POST can return 201."
        );
    }

    assertThat(postStatus)
        .as("POST /projects with unique name must return 201 Created")
        .isEqualTo(201);

    String projectUid = postResponse.jsonPath().getString("uid");
    assertThat(projectUid).isNotBlank();

    reportStep("Register uid for DELETE cleanup before PUT so a failed PUT does not strand the project");
    projectUidToCleanup = projectUid;

    // -------------------- PUT: Update Project Name --------------------
    Map<String, Object> putBody =
        LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
            updatedName,
            LyticsProjectTestData.VALID_DOMAIN,
            LyticsProjectTestData.VALID_DESCRIPTION
        );

    reportStep(
        "Given: same headers + JSON body as POST curl with updated name; "
            + "When: PUT " + ApiPaths.projectByUid(projectUid) + "; "
            + "Then: extract response"
    );

    Response putResponse = given()
        .spec(lyticsRequestSpec)
        .body(putBody)
        .when()
        .put(ApiPaths.projectByUid(projectUid))
        .then()
        .extract()
        .response();

    putResponse.prettyPrint();
    reportResponseBody(putResponse);

    assertThat(putResponse.getStatusCode())
        .as("PUT /projects/{uid} valid full-body update (name changed)")
        .isIn(200, 204);

    // -------------------- GET: Verify Update --------------------
    reportStep(
        "Given: Lytics headers; "
            + "When: GET " + ApiPaths.projectByUid(projectUid) + "; "
            + "Then: extract response"
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

    assertThat(getResponse.getStatusCode())
        .as("GET /projects/{uid} after PUT")
        .isEqualTo(200);

    // -------------------- Assert Updated Name --------------------
    reportStep("Assert only name on GET response matches the value sent on PUT");

    assertThat(getResponse.jsonPath().getString("name"))
        .as("Updated project name")
        .isEqualTo(updatedName);
}
    @Test(
    priority = 2,
    description = "POST /projects (curl-shaped body) then PUT with updated domain only "
        + "(same name/description/connections) — given/when/then; GET — assert only domain"
)
public void TC_PUT_BY_UID_002_POST_then_PUT_valid_request_update_domain_only() {
    reportStep("Reset cleanup uid so this run does not delete an unrelated project");
    projectUidToCleanup = null;

    String projectName = "DNI Test domain " + UUID.randomUUID();
    String initialDomain = LyticsProjectTestData.VALID_DOMAIN;
    String updatedDomain = LyticsProjectTestData.EXAMPLE_COM_DOMAIN;

    Map<String, Object> postBody =
        LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
            projectName,
            initialDomain,
            LyticsProjectTestData.VALID_DESCRIPTION
        );

    // -------------------- POST: Create Project --------------------
    reportStep(
        "Given: Lytics headers + JSON body (www.google.com); "
            + "When: POST " + ApiPaths.PROJECTS + "; "
            + "Then: extract response"
    );

    Response postResponse = given()
        .spec(lyticsRequestSpec)
        .body(postBody)
        .when()
        .post(ApiPaths.PROJECTS)
        .then()
        .extract()
        .response();

    postResponse.prettyPrint();
    reportResponseBody(postResponse);

    String projectUid = postResponse.jsonPath().getString("uid");
    assertThat(projectUid).isNotBlank();

    reportStep("Register uid for DELETE cleanup before PUT so a failed PUT does not strand the project");
    projectUidToCleanup = projectUid;

    // -------------------- PUT: Update Project Domain --------------------
    Map<String, Object> putBody =
        LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
            projectName,
            updatedDomain,
            LyticsProjectTestData.VALID_DESCRIPTION
        );

    reportStep(
        "Given: same headers + body as POST with updated domain only; "
            + "When: PUT " + ApiPaths.projectByUid(projectUid) + "; "
            + "Then: extract response"
    );

    Response putResponse = given()
        .spec(lyticsRequestSpec)
        .body(putBody)
        .when()
        .put(ApiPaths.projectByUid(projectUid))
        .then()
        .extract()
        .response();

    putResponse.prettyPrint();
    reportResponseBody(putResponse);

    assertThat(putResponse.getStatusCode())
        .as("PUT /projects/{uid} valid full-body update (domain changed)")
        .isIn(200, 204);

    // -------------------- GET: Verify Update --------------------
    reportStep(
        "Given: Lytics headers; "
            + "When: GET " + ApiPaths.projectByUid(projectUid) + "; "
            + "Then: extract response"
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

    assertThat(getResponse.getStatusCode())
        .as("GET /projects/{uid} after PUT")
        .isEqualTo(200);

    // -------------------- Assert Updated Domain --------------------
    reportStep("Assert only domain on GET response matches the value sent on PUT");

    assertThat(getResponse.jsonPath().getString("domain"))
        .as("Updated project domain")
        .isEqualTo(updatedDomain);
}

    @Test(
        priority = 3,
        description = "POST /projects (curl-shaped body) then PUT with updated description only "
            + "(same name/domain/connections) — given/when/then; GET — assert only description"
    )
    public void TC_PUT_BY_UID_003_POST_then_PUT_valid_request_update_description_only() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String projectName = "DNI Test desc " + UUID.randomUUID();
        String initialDescription = LyticsProjectTestData.VALID_DESCRIPTION;
        String updatedDescription = LyticsProjectTestData.SAMPLE_PROJECT_DESCRIPTION;

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                initialDescription
            );

        // -------------------- POST: Create Project --------------------
        reportStep(
            "Given: Lytics headers + JSON body with initial description; "
                + "When: POST " + ApiPaths.PROJECTS + "; "
                + "Then: extract response"
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(postBody)
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        assertThat(postResponse.getStatusCode())
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        reportStep("Register uid for DELETE cleanup before PUT so a failed PUT does not strand the project");
        projectUidToCleanup = projectUid;

        // -------------------- PUT: Update Project Description --------------------
        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                updatedDescription
            );

        reportStep(
            "Given: same headers + JSON body as POST curl with updated description only; "
                + "When: PUT " + ApiPaths.projectByUid(projectUid) + "; "
                + "Then: extract response"
        );

        Response putResponse = given()
            .spec(lyticsRequestSpec)
            .body(putBody)
            .when()
            .put(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} valid full-body update (description changed)")
            .isIn(200, 204);

        // -------------------- GET: Verify Update --------------------
        reportStep(
            "Given: Lytics headers; "
                + "When: GET " + ApiPaths.projectByUid(projectUid) + "; "
                + "Then: extract response"
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

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT")
            .isEqualTo(200);

        // -------------------- Assert Updated Description --------------------
        reportStep("Assert only description on GET response matches the value sent on PUT");

        assertThat(getResponse.jsonPath().getString("description"))
            .as("Updated project description")
            .isEqualTo(updatedDescription);
    }

    @Test(
        priority = 4,
        description = "POST /projects with default connections (curl-shaped) then PUT with updated "
            + "stackApiKeys, launchProjectUids, personalizeProjectUids — same name/domain/description; "
            + "GET — assert connections match PUT payload"
    )
    public void TC_PUT_BY_UID_004_POST_then_PUT_valid_request_updating_connections() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String projectName = "DNI Test " + UUID.randomUUID();
        String domain = LyticsProjectTestData.VALID_DOMAIN;
        String description = LyticsProjectTestData.VALID_DESCRIPTION;

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                domain,
                description,
                LyticsProjectPayloadBuilder.defaultConnections()
            );

        reportStep(
            "Given: Lytics headers + JSON body matching POST curl (name, domain, desc, default connections); "
                + "When: POST " + ApiPaths.PROJECTS + "; "
                + "Then: extract response"
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(postBody)
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    postResponse.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    postResponse.jsonPath().getString(
                        "errors['connections.personalizeProjectUids'][0].code")))) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: a connection ID is already linked to another "
                    + "project in this org. Free the connection or use a different environment."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        reportStep("Register uid for DELETE cleanup before PUT so a failed PUT does not strand the project");
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                domain,
                description,
                LyticsProjectPayloadBuilder.connectionsAfterPutUpdate()
            );

        reportStep(
            "Given: same name/domain/description as POST; connections match PUT curl (alternate stack, "
                + "launch, personalize); "
                + "When: PUT " + ApiPaths.projectByUid(projectUid) + "; "
                + "Then: extract response"
        );

        Response putResponse = given()
            .spec(lyticsRequestSpec)
            .body(putBody)
            .when()
            .put(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 400
            && ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString(
                        "errors['connections.personalizeProjectUids'][0].code")))) {
            throw new SkipException(
                "PUT returned DUPLICATE_CONNECTION: an alternate connection ID is already linked "
                    + "to another project. Free those connections or refresh "
                    + "LyticsProjectTestData alternate PUT constants."
            );
        }

        assertThat(putStatus)
            .as("PUT /projects/{uid} valid full-body update (connections changed)")
            .isIn(200, 204);

        reportStep(
            "Given: Lytics headers; "
                + "When: GET " + ApiPaths.projectByUid(projectUid) + "; "
                + "Then: extract response"
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

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT")
            .isEqualTo(200);

        reportStep("Assert name, domain, description unchanged; connections match PUT payload");

        assertThat(getResponse.jsonPath().getString("name"))
            .as("Project name unchanged after connections-only PUT")
            .isEqualTo(projectName);
        assertThat(getResponse.jsonPath().getString("domain"))
            .as("Project domain unchanged after connections-only PUT")
            .isEqualTo(domain);
        assertThat(getResponse.jsonPath().getString("description"))
            .as("Project description unchanged after connections-only PUT")
            .isEqualTo(description);

        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .as("GET connections.stackApiKeys must match PUT body")
            .containsExactly(LyticsProjectTestData.STACK_API_KEY_AFTER_PUT_UPDATE);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .as("GET connections.launchProjectUids must match PUT body")
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID_AFTER_PUT_UPDATE);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .as("GET connections.personalizeProjectUids must match PUT body")
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID_AFTER_PUT_UPDATE);
    }

    @Test(
        priority = 5,
        description = "POST /projects (full valid body + default connections) then PUT updating "
            + "name, domain, description, and connections — valid UID from POST; GET asserts all "
            + "fields match PUT payload"
    )
    public void TC_PUT_BY_UID_005_POST_then_PUT_valid_request_update_all_fields() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String suffix = UUID.randomUUID().toString();
        String initialName = "DNI Test all-fields " + suffix;
        String updatedName = "DNI Test all-fields updated " + suffix;
        String initialDomain = LyticsProjectTestData.VALID_DOMAIN;
        String updatedDomain = LyticsProjectTestData.EXAMPLE_COM_DOMAIN;
        String initialDescription = LyticsProjectTestData.VALID_DESCRIPTION;
        String updatedDescription = LyticsProjectTestData.SAMPLE_PROJECT_DESCRIPTION;

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                initialName,
                initialDomain,
                initialDescription,
                LyticsProjectPayloadBuilder.defaultConnections()
            );

        reportStep(
            "Given: Lytics headers + full POST body (name, domain, desc, default connections); "
                + "When: POST " + ApiPaths.PROJECTS + "; "
                + "Then: extract response"
        );

        Response postResponse = given()
            .spec(lyticsRequestSpec)
            .body(postBody)
            .when()
            .post(ApiPaths.PROJECTS)
            .then()
            .extract()
            .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    postResponse.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    postResponse.jsonPath().getString(
                        "errors['connections.personalizeProjectUids'][0].code")))) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: a default connection ID is already linked to "
                    + "another project. Free the connection or use a different environment."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        reportStep("Register uid for DELETE cleanup before PUT so a failed PUT does not strand the project");
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                updatedName,
                updatedDomain,
                updatedDescription,
                LyticsProjectPayloadBuilder.connectionsAfterPutUpdate()
            );

        reportStep(
            "Given: full valid PUT body — new name, domain, description, alternate connections; "
                + "When: PUT " + ApiPaths.projectByUid(projectUid) + "; "
                + "Then: extract response"
        );

        Response putResponse = given()
            .spec(lyticsRequestSpec)
            .body(putBody)
            .when()
            .put(ApiPaths.projectByUid(projectUid))
            .then()
            .extract()
            .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 400
            && ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString(
                        "errors['connections.personalizeProjectUids'][0].code")))) {
            throw new SkipException(
                "PUT returned DUPLICATE_CONNECTION: alternate connection IDs are already linked "
                    + "to another project. Free those connections or refresh "
                    + "LyticsProjectTestData alternate PUT constants."
            );
        }

        assertThat(putStatus)
            .as("PUT /projects/{uid} valid full-body update (all scalar + connection fields changed)")
            .isIn(200, 204);

        reportStep(
            "Given: Lytics headers; "
                + "When: GET " + ApiPaths.projectByUid(projectUid) + "; "
                + "Then: extract response"
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

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT")
            .isEqualTo(200);

        reportStep("Assert name, domain, description, and connections match PUT payload");

        assertThat(getResponse.jsonPath().getString("name"))
            .as("Updated project name")
            .isEqualTo(updatedName);
        assertThat(getResponse.jsonPath().getString("domain"))
            .as("Updated project domain")
            .isEqualTo(updatedDomain);
        assertThat(getResponse.jsonPath().getString("description"))
            .as("Updated project description")
            .isEqualTo(updatedDescription);
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .as("GET connections.stackApiKeys must match PUT body")
            .containsExactly(LyticsProjectTestData.STACK_API_KEY_AFTER_PUT_UPDATE);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .as("GET connections.launchProjectUids must match PUT body")
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID_AFTER_PUT_UPDATE);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .as("GET connections.personalizeProjectUids must match PUT body")
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID_AFTER_PUT_UPDATE);
    }

    @Test(
        priority = 6,
        description =
            "PUT /projects (collection path, no {uid} segment) with curl-shaped JSON body — "
                + "expect 400 Bad Request; message names missing x-project-uid header"
    )
    public void TC_PUT_BY_UID_006_Send_PUT_request_to_projects_without_project_uid() {
        reportStep("Reset cleanup uid; this negative call must not create a project");
        projectUidToCleanup = null;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                "DNI Test",
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: x-cs-api-version, organization_uid, authtoken, JSON body (name/domain/description); "
                + "When: PUT "
                + ApiPaths.PROJECTS
                + " (no uid in path); "
                + "Then: extract response"
        );

        Response response =
            given()
                .baseUri(TestConfig.lyticsBaseUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                .header("organization_uid", TestConfig.lyticsOrganizationUid())
                .header("authtoken", TestConfig.lyticsAuthToken())
                .body(putBody)
                .when()
                .put(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400 and envelope for missing project identification (x-project-uid)");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message"))
            .isEqualTo("Missing required header: x-project-uid");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(response.jsonPath().getString("error")).isEqualTo("Bad Request");
    }

    @Test(
        priority = 7,
        description =
            "PUT /projects/{uid} with invalid/non-resolvable uid in path (curl-shaped body) — "
                + "expect 404 Not Found; message Project not found"
    )
    public void TC_PUT_BY_UID_007_Send_PUT_request_with_invalid_uid_expect_404_project_not_found() {
        reportStep("Reset cleanup uid; PUT must not target a real project");
        projectUidToCleanup = null;

        String invalidPathUid = "69dc794d3456t27a58e1956f";

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                "DNI Test",
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: Lytics headers + JSON body (name/domain/description) matching manual curl; "
                + "When: PUT "
                + ApiPaths.projectByUid(invalidPathUid)
                + "; "
                + "Then: extract response"
        );

        Response response =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(invalidPathUid))
                .then()
                .extract()
                .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 404 and Project not found envelope");
        ProjectAssertions.assertProjectNotFound(response);
    }

    @Test(
        priority = 8,
        description =
            "PUT /projects/{uid} without authtoken header (missing authorization) — curl-shaped body; "
                + "expect 401 with error_message, error_code 105, errors.authtoken"
    )
    public void TC_PUT_BY_UID_008_Send_PUT_request_without_authtoken_header() {
        reportStep("Reset cleanup uid; unauthenticated PUT must not update a project");
        projectUidToCleanup = null;

        String pathUid = "69dc794d3456t27a58e1956f";

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                "DNI Test",
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: x-cs-api-version, organization_uid, JSON body only (no authtoken), same path as manual curl; "
                + "When: PUT "
                + ApiPaths.projectByUid(pathUid)
                + "; "
                + "Then: extract response"
        );

        Response response =
            given()
                .baseUri(TestConfig.lyticsBaseUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                .header("organization_uid", TestConfig.lyticsOrganizationUid())
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(pathUid))
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
        priority = 9,
        description =
            "PUT /projects/{uid} with invalid authtoken header — curl-shaped body; "
                + "expect 401 with error_message, error_code 105, errors.authtoken"
    )
    public void TC_PUT_BY_UID_009_Send_PUT_request_with_invalid_authtoken_header() {
        reportStep("Reset cleanup uid; invalid token must not update a project");
        projectUidToCleanup = null;

        String pathUid = "69dc794d3456t27a58e1956f";

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                "DNI Test",
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: x-cs-api-version, organization_uid, authtoken set to a non-valid value, JSON body; "
                + "When: PUT "
                + ApiPaths.projectByUid(pathUid)
                + "; "
                + "Then: extract response"
        );

        Response response =
            given()
                .baseUri(TestConfig.lyticsBaseUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                .header("organization_uid", TestConfig.lyticsOrganizationUid())
                .header("authtoken", "invalid-authtoken-" + UUID.randomUUID())
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(pathUid))
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
        priority = 10,
        description =
            "PUT /projects/{uid} with expired authtoken header — curl-shaped body; "
                + "expect 401 with error_message, error_code 105, errors.authtoken"
    )
    public void TC_PUT_BY_UID_010_Send_PUT_request_with_expired_authtoken_header() {
        Optional<String> expiredToken = TestConfig.lyticsExpiredAuthToken();
        if (expiredToken.isEmpty()) {
            throw new SkipException(
                "Set lytics.auth.token.expired in config.properties or -Dlytics.auth.token.expired=... to run this"
                    + " test"
            );
        }

        reportStep("Reset cleanup uid; expired token must not update a project");
        projectUidToCleanup = null;

        String pathUid = "69dc794d3456t27a58e1956f";

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                "DNI Test",
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: x-cs-api-version, organization_uid, authtoken from lytics.auth.token.expired, JSON body; "
                + "When: PUT "
                + ApiPaths.projectByUid(pathUid)
                + "; "
                + "Then: extract response"
        );

        Response response =
            given()
                .baseUri(TestConfig.lyticsBaseUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                .header("organization_uid", TestConfig.lyticsOrganizationUid())
                .header("authtoken", expiredToken.get())
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(pathUid))
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
        priority = 11,
        description =
            "PUT /projects/{uid} without organization_uid header — curl-shaped JSON body; "
                + "expect 400 with message, status, error for missing organization_uid"
    )
    public void TC_PUT_BY_UID_011_Send_PUT_request_without_organization_uid_header() {
        reportStep("Reset cleanup uid; missing organization must not update a project");
        projectUidToCleanup = null;

        String pathUid = "69dc794d3456t27a58e1956f";

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                "DNI Test",
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: x-cs-api-version, authtoken, JSON body (no organization_uid), same path as manual curl; "
                + "When: PUT "
                + ApiPaths.projectByUid(pathUid)
                + "; "
                + "Then: extract response"
        );

        Response response =
            given()
                .baseUri(TestConfig.lyticsBaseUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                .header("authtoken", TestConfig.lyticsAuthToken())
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(pathUid))
                .then()
                .extract()
                .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400 Bad Request and envelope for missing organization_uid header");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message"))
            .isEqualTo("Missing required header: organization_uid");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(response.jsonPath().getString("error")).isEqualTo("Bad Request");
    }

    @Test(
        priority = 12,
        description =
            "PUT /projects/{uid} with invalid project UID (wrong format, e.g. alphanumeric segment) — "
                + "curl-shaped body; expect 404 Not Found; message Project not found"
    )
    public void TC_PUT_BY_UID_012_Send_PUT_request_with_invalid_project_uid_wrong_format() {
        reportStep("Reset cleanup uid; PUT must not target a real project");
        projectUidToCleanup = null;

        String invalidPathUid = "test3383782";

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                "DNI Test",
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: Lytics headers + JSON body (name/domain/description) matching manual curl; "
                + "When: PUT "
                + ApiPaths.projectByUid(invalidPathUid)
                + " (non-UID path segment); "
                + "Then: extract response"
        );

        Response response =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(invalidPathUid))
                .then()
                .extract()
                .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 404 and Project not found envelope");
        ProjectAssertions.assertProjectNotFound(response);
    }

    @Test(
        priority = 13,
        description =
            "PUT /projects/{uid} with well-formed project UID that does not exist — curl-shaped body; "
                + "expect 404 Not Found; message Project not found"
    )
    public void TC_PUT_BY_UID_013_Send_PUT_request_with_nonexisting_project_uid() {
        reportStep("Reset cleanup uid; PUT must not update a missing project");
        projectUidToCleanup = null;

        String nonexistentHexUid = "deadbeefdeadbeefdeadbeef";

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                "DNI Test",
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: Lytics headers + JSON body (name/domain/description); "
                + "When: PUT "
                + ApiPaths.projectByUid(nonexistentHexUid)
                + " (valid-format uid unlikely to exist); "
                + "Then: extract response"
        );

        Response response =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(nonexistentHexUid))
                .then()
                .extract()
                .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert HTTP 404 and Project not found envelope");
        ProjectAssertions.assertProjectNotFound(response);
    }

    @Test(
        priority = 14,
        description =
            "PUT /projects/{uid} with empty project UID in path (/projects/) — curl-shaped JSON body; "
                + "expect 400 Bad Request; message names missing x-project-uid header"
    )
    public void TC_PUT_BY_UID_014_Send_PUT_request_with_empty_project_uid_in_path() {
        reportStep("Reset cleanup uid; empty path uid must not update a project");
        projectUidToCleanup = null;

        String emptyPathUid = "";
        String path = ApiPaths.projectByUid(emptyPathUid);

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                "DNI Test",
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: Lytics headers + JSON body (name/domain/description); "
                + "When: PUT "
                + path
                + " (empty uid segment); "
                + "Then: extract response"
        );

        Response response =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(path)
                .then()
                .extract()
                .response();

        response.prettyPrint();
        reportResponseBody(response);

        reportStep("Assert 400 and envelope for missing project identification (x-project-uid)");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message"))
            .isEqualTo("Missing required header: x-project-uid");
        assertThat(response.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(response.jsonPath().getString("error")).isEqualTo("Bad Request");
    }

    @Test(
        priority = 15,
        description =
            "POST /projects (name/domain/description, no connections) in default org, then PUT /projects/{uid} with a "
                + "different organization_uid (cross.put or nonexisting) — expect 404 Project not found; cleanup DELETE"
    )
    public void TC_PUT_BY_UID_015_POST_then_PUT_with_different_organization_uid_expect_404() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String suffix = UUID.randomUUID().toString();
        String projectName = "DNI Test " + suffix;
        String crossOrgUid = "bltabd665cea8d08fce";

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: default Lytics headers + JSON body (name/domain/description, connections key absent); "
                + "When: POST "
                + ApiPaths.PROJECTS
                + "; "
                + "Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();

        assertThat(postStatus)
            .as("POST /projects with curl-shaped body must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();

        reportStep("Register uid for DELETE cleanup before cross-org PUT");
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                "DNI Test updated " + suffix,
                "www.google.com",
                "desc"
            );

        reportStep(
            "Given: same base URI, version, authtoken, PUT body (name/domain/description, no connections); organization_uid set to a different org ("
                + crossOrgUid
                + "); "
                + "When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; "
                + "Then: extract response"
        );

        Response putResponse =
            given()
                .baseUri(TestConfig.lyticsBaseUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("x-cs-api-version", TestConfig.lyticsApiVersion())
                .header("organization_uid", crossOrgUid)
                .header("authtoken", TestConfig.lyticsAuthToken())
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        reportStep("Assert HTTP 404 and Project not found envelope (project not visible under the other organization)");
        ProjectAssertions.assertProjectNotFound(putResponse);
        //This is issue discuss with santosh and vedant it should be 404 Project not found
    }

    @Test(
        priority = 16,
        description =
            "POST /projects twice with different names, then PUT /projects/{uid} for the second project using the "
                + "first project's name — expect 400 Bad request and lytics.PROJECTS.DUPLICATE_PROJECT_NAME on errors.name"
    )
    public void TC_PUT_BY_UID_016_POST_POST_then_PUT_with_duplicate_name_expect_400() {
        reportStep("Reset cleanup uid; both created projects are deleted in a finally block");
        projectUidToCleanup = null;

        String suffix = UUID.randomUUID().toString();
        String firstProjectName = "DNI Test A " + suffix;
        String secondProjectName = "DNI Test B " + suffix;
        String domain = "www.google.com";
        String description = "desc";

        String firstUid = null;
        String secondUid = null;
        try {
            Map<String, Object> firstPostBody =
                LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                    firstProjectName,
                    domain,
                    description
                );

            reportStep(
                "Given: Lytics headers + JSON (name A, domain, description, no connections); "
                    + "When: POST "
                    + ApiPaths.PROJECTS
                    + "; Then: extract response"
            );

            Response firstPostResponse =
                given()
                    .spec(lyticsRequestSpec)
                    .body(firstPostBody)
                    .when()
                    .post(ApiPaths.PROJECTS)
                    .then()
                    .extract()
                    .response();

            firstPostResponse.prettyPrint();
            reportResponseBody(firstPostResponse);

            assertThat(firstPostResponse.getStatusCode())
                .as("First POST /projects must return 201 Created")
                .isEqualTo(201);
            firstUid = firstPostResponse.jsonPath().getString("uid");
            assertThat(firstUid).isNotBlank();

            Map<String, Object> secondPostBody =
                LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                    secondProjectName,
                    domain,
                    description
                );

            reportStep(
                "Given: same headers + JSON (name B, same domain/description, no connections); "
                    + "When: POST "
                    + ApiPaths.PROJECTS
                    + "; Then: extract response"
            );

            Response secondPostResponse =
                given()
                    .spec(lyticsRequestSpec)
                    .body(secondPostBody)
                    .when()
                    .post(ApiPaths.PROJECTS)
                    .then()
                    .extract()
                    .response();

            secondPostResponse.prettyPrint();
            reportResponseBody(secondPostResponse);

            assertThat(secondPostResponse.getStatusCode())
                .as("Second POST /projects with a different name must return 201 Created")
                .isEqualTo(201);
            secondUid = secondPostResponse.jsonPath().getString("uid");
            assertThat(secondUid).isNotBlank();

            Map<String, Object> putBody =
                LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                    firstProjectName,
                    domain,
                    description
                );

            reportStep(
                "Given: PUT body uses the first project's name on the second project's uid (duplicate name in org); "
                    + "When: PUT "
                    + ApiPaths.projectByUid(secondUid)
                    + "; Then: extract response"
            );

            Response putResponse =
                given()
                    .spec(lyticsRequestSpec)
                    .body(putBody)
                    .when()
                    .put(ApiPaths.projectByUid(secondUid))
                    .then()
                    .extract()
                    .response();

            putResponse.prettyPrint();
            reportResponseBody(putResponse);

            reportStep(
                "Assert HTTP 400, message Bad request, status 400, and DUPLICATE_PROJECT_NAME on errors.name[0]"
            );
            assertThat(putResponse.getStatusCode()).isEqualTo(400);
            assertThat(putResponse.jsonPath().getString("message")).isEqualTo("Bad request");
            assertThat(putResponse.jsonPath().getInt("status")).isEqualTo(400);
            assertThat(putResponse.jsonPath().getList("errors.name")).hasSize(1);
            assertThat(putResponse.jsonPath().getString("errors.name[0].code"))
                .isEqualTo("lytics.PROJECTS.DUPLICATE_PROJECT_NAME");
        } finally {
            if (secondUid != null && !secondUid.isBlank()) {
                try {
                    projectApiClient.deleteProject(secondUid).then().statusCode(204);
                } catch (Throwable e) {
                    System.err.println(
                        "[cleanup] DELETE /projects/" + secondUid + " failed (ignored): " + e.getMessage());
                }
            }
            if (firstUid != null && !firstUid.isBlank()) {
                try {
                    projectApiClient.deleteProject(firstUid).then().statusCode(204);
                } catch (Throwable e) {
                    System.err.println(
                        "[cleanup] DELETE /projects/" + firstUid + " failed (ignored): " + e.getMessage());
                }
            }
        }
    }

    @Test(
        priority = 17,
        description =
            "POST /projects with a valid project, then PUT /projects/{uid} with JSON missing the `name` field — "
                + "expect 400; name validation treats missing name as empty (same NOT_EMPTY signal as empty-string name)"
    )
    public void TC_PUT_BY_UID_017_POST_then_PUT_without_name_field_expect_400() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String suffix = UUID.randomUUID().toString();
        String projectName = "DNI Test " + suffix;

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + valid POST body (name, domain, description, default connections); "
                + "When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                    + "project in this org. Free the connection or delete the conflicting project."
            );
        }

        assertThat(postStatus)
            .as("POST /projects must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody = LyticsProjectPayloadBuilder.projectCreatePayloadWithoutName();

        reportStep(
            "Given: PUT body matches POST TC_002 shape (domain, description, connections; `name` key absent); "
                + "When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        reportStep(
            "Assert 400 Bad request; errors.name includes NOT_EMPTY (missing name treated as name field empty \"\")"
        );
        assertThat(putResponse.getStatusCode()).isEqualTo(400);
        assertThat(putResponse.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(putResponse.jsonPath().getInt("status")).isEqualTo(400);

    List<Map<String, Object>> nameErrors = putResponse.jsonPath().getList("errors.name");
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
        priority = 18,
        description =
            "POST /projects (name/domain/description, no connections), then PUT /projects/{uid} with "
                + "whitespace-only name — expect 400 Bad request and lytics.PROJECTS.NOT_EMPTY on errors.name[0]"
    )
    public void TC_PUT_BY_UID_018_POST_without_connections_then_PUT_space_only_name_expect_400() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String suffix = UUID.randomUUID().toString();
        String projectName = "DNI Test " + suffix;

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + JSON (name, domain, description; connections key absent); "
                + "When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        assertThat(postResponse.getStatusCode())
            .as("POST /projects without connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                LyticsProjectTestData.SPACE_ONLY_PROJECT_NAME,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body same shape as POST (no connections) with name containing only whitespace; "
                + "When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        reportStep(
            "Assert 400 Bad request envelope and single NOT_EMPTY on errors.name (whitespace-only name rejected)"
        );
        assertThat(putResponse.getStatusCode()).isEqualTo(400);
        assertThat(putResponse.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(putResponse.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(putResponse.jsonPath().getList("errors.name")).hasSize(1);
        assertThat(putResponse.jsonPath().getString("errors.name[0].code"))
            .isEqualTo("lytics.PROJECTS.NOT_EMPTY");
    }

    @Test(
        priority = 19,
        description =
            "POST /projects then PUT /projects/{uid} with name exactly 200 characters — expect 200/204; "
                + "GET /projects/{uid} — assert name matches the 200-character value sent on PUT"
    )
    public void TC_PUT_BY_UID_019_POST_then_PUT_name_exactly_200_chars_expect_success_verify_name() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String suffix = UUID.randomUUID().toString();
        String initialName = "DNI Test " + suffix;

        String maxNamePrefix = "DNI PUT MAX " + suffix + " ";
        assertThat(maxNamePrefix.length())
            .as("Prefix for max-length name must fit within API max so padding can reach exactly 200")
            .isLessThanOrEqualTo(LyticsProjectTestData.PROJECT_NAME_MAX_LENGTH);
        String updatedName =
            maxNamePrefix
                + "Y".repeat(LyticsProjectTestData.PROJECT_NAME_MAX_LENGTH - maxNamePrefix.length());

        reportStep("Precondition: updated name is exactly PROJECT_NAME_MAX_LENGTH characters");
        assertThat(updatedName.length()).isEqualTo(LyticsProjectTestData.PROJECT_NAME_MAX_LENGTH);

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                initialName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + valid POST body; When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                    + "project in this org. Free the connection or delete the conflicting project."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                updatedName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body with name exactly 200 characters; When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} with max-length name must succeed (200 OK or 204 No Content)")
            .isIn(200, 204);

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT")
            .isEqualTo(200);

        reportStep("Assert GET name matches the 200-character string sent on PUT");
        assertThat(getResponse.jsonPath().getString("name"))
            .as("Project name after PUT at max length")
            .isEqualTo(updatedName);
    }

    @Test(
        priority = 20,
        description =
            "POST /projects (no connections), then PUT /projects/{uid} with invalid domain format (abc) — "
                + "expect 400 Bad request and lytics.PROJECTS.INVALID_DOMAIN on errors.domain[0]"
    )
    public void TC_PUT_BY_UID_020_POST_without_connections_then_PUT_invalid_domain_format_expect_400() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String suffix = UUID.randomUUID().toString();
        String projectName = "DNI Test " + suffix;

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + JSON (name, domain, description; connections absent); "
                + "When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        assertThat(postResponse.getStatusCode())
            .as("POST /projects without connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.INVALID_DOMAIN_FORMAT_ABC,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body same shape as POST with domain = invalid format \"abc\"; "
                + "When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        reportStep(
            "Assert 400 Bad request envelope and single INVALID_DOMAIN on errors.domain (same shape as POST TC_020)"
        );
        assertThat(putResponse.getStatusCode()).isEqualTo(400);
        assertThat(putResponse.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(putResponse.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(putResponse.jsonPath().getList("errors.domain")).hasSize(1);
        assertThat(putResponse.jsonPath().getString("errors.domain[0].code"))
            .isEqualTo("lytics.PROJECTS.INVALID_DOMAIN");
    }

    @Test(
        priority = 21,
        description =
            "POST /projects (no connections), then PUT /projects/{uid} with empty string domain — "
                + "expect 400 Bad request; errors.domain includes INVALID_DOMAIN and NOT_EMPTY (same shape as POST TC_016)"
    )
    public void TC_PUT_BY_UID_021_POST_without_connections_then_PUT_empty_domain_expect_400() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String suffix = UUID.randomUUID().toString();
        String projectName = "DNI Test " + suffix;

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + JSON (name, domain, description; connections absent); "
                + "When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        assertThat(postResponse.getStatusCode())
            .as("POST /projects without connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.EMPTY_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body same shape as POST with domain = empty string; "
                + "When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        reportStep(
            "Assert 400 Bad request; errors.domain has INVALID_DOMAIN and NOT_EMPTY (order-independent)"
        );
        assertThat(putResponse.getStatusCode()).isEqualTo(400);
        assertThat(putResponse.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(putResponse.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> domainErrors = putResponse.jsonPath().getList("errors.domain");
        assertThat(domainErrors).hasSize(2);
        assertThat(domainErrors)
            .extracting(m -> m.get("code"))
            .containsExactlyInAnyOrder(
                "lytics.PROJECTS.INVALID_DOMAIN",
                "lytics.PROJECTS.NOT_EMPTY");
    }

    @Test(
        priority = 22,
        description =
            "POST /projects with non-empty description, then PUT /projects/{uid} with description = empty string — "
                + "expect HTTP 200; GET — assert description is \"\" (same acceptance as POST TC_036)"
    )
    public void TC_PUT_BY_UID_022_POST_then_PUT_empty_description_expect_200_verify_description() {
        reportStep("Reset cleanup uid so this run does not delete an unrelated project");
        projectUidToCleanup = null;

        String projectName = "DNI Test empty desc " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + valid POST body (non-empty description); "
                + "When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                    + "project in this org. Free the connection or delete the conflicting project."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.EMPTY_DESCRIPTION
            );

        reportStep(
            "Given: same name/domain/connections with description cleared to empty string; "
                + "When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} clearing description must return 200 OK")
            .isEqualTo(200);

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT")
            .isEqualTo(200);

        reportStep("Assert GET description matches empty string sent on PUT");
        assertThat(getResponse.jsonPath().getString("description"))
            .as("Updated project description")
            .isEqualTo(LyticsProjectTestData.EMPTY_DESCRIPTION);
    }

    @Test(
        priority = 23,
        description =
            "POST /projects then PUT /projects/{uid} with description longer than 255 characters — "
                + "expect 400 Bad request; errors.description[0] MAX_CHAR_LIMIT with maxCharacters 255 (POST TC_039 shape)"
    )
    public void TC_PUT_BY_UID_023_POST_then_PUT_description_over_255_chars_expect_400() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        reportStep("Precondition: over-max description string is exactly max+1 characters");
        assertThat(LyticsProjectTestData.DESCRIPTION_ONE_OVER_MAX_LENGTH.length())
            .isEqualTo(LyticsProjectTestData.PROJECT_DESCRIPTION_MAX_LENGTH + 1);

        String projectName = "DNI Test desc over " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + valid POST body; When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                    + "project in this org. Free the connection or delete the conflicting project."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.DESCRIPTION_ONE_OVER_MAX_LENGTH
            );

        reportStep(
            "Given: PUT body with description length max+1; When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        reportStep(
            "Assert 400 Bad request; errors.description has MAX_CHAR_LIMIT and maxCharacters = 255"
        );
        assertThat(putResponse.getStatusCode()).isEqualTo(400);
        assertThat(putResponse.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(putResponse.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> descriptionErrors = putResponse.jsonPath().getList("errors.description");
        assertThat(descriptionErrors).hasSize(1);

        Map<String, Object> descriptionError = descriptionErrors.get(0);
        assertThat(descriptionError.get("code")).isEqualTo("lytics.PROJECTS.MAX_CHAR_LIMIT");
        assertThat(((Number) Objects.requireNonNull(descriptionError.get("maxCharacters"))).intValue())
            .isEqualTo(LyticsProjectTestData.PROJECT_DESCRIPTION_MAX_LENGTH);
    }

    @Test(
        priority = 24,
        description =
            "POST /projects with valid default connections then PUT /projects/{uid} with an unknown "
                + "stackApiKeys value (valid launch + personalize) — expect 400 Bad request; "
                + "errors.connections.stackApiKeys[0].code lytics.PROJECTS.CONNECTION_NOT_FOUND"
    )
    public void TC_PUT_BY_UID_024_POST_valid_connections_then_PUT_invalid_stack_expect_400() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT invalid stack " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + valid POST body (default connections); When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                    + "project in this org. Free the connection or delete the conflicting project."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithInvalidStackApiKeyAndValidLaunchPersonalize(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body same name/domain/description but stackApiKeys replaced with unknown key; "
                + "When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        reportStep(
            "Assert 400 Bad request; errors.connections.stackApiKeys CONNECTION_NOT_FOUND"
        );
        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} with unknown stack API key must return 400 Bad request")
            .isEqualTo(400);
        assertThat(putResponse.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(putResponse.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> stackErrors =
            putResponse.jsonPath().getList("errors['connections.stackApiKeys']");
        assertThat(stackErrors).as("errors.connections.stackApiKeys").hasSize(1);
        assertThat(stackErrors.get(0).get("code"))
            .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
    }

    @Test(
        priority = 25,
        description =
            "POST /projects with valid default connections then PUT /projects/{uid} with an unknown "
                + "launchProjectUids value (valid stack + personalize) — expect 400 Bad request; "
                + "errors.connections.launchProjectUids[0].code lytics.PROJECTS.CONNECTION_NOT_FOUND"
    )
    public void TC_PUT_BY_UID_025_POST_valid_connections_then_PUT_invalid_launch_expect_400() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT invalid launch " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + valid POST body (default connections); When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                    + "project in this org. Free the connection or delete the conflicting project."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithValidStackPersonalizeAndInvalidLaunchProjectUid(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body same name/domain/description but launchProjectUids replaced with unknown uid; "
                + "When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        reportStep(
            "Assert 400 Bad request; errors.connections.launchProjectUids CONNECTION_NOT_FOUND"
        );
        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} with unknown launch project uid must return 400 Bad request")
            .isEqualTo(400);
        assertThat(putResponse.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(putResponse.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> launchErrors =
            putResponse.jsonPath().getList("errors['connections.launchProjectUids']");
        assertThat(launchErrors).as("errors.connections.launchProjectUids").hasSize(1);
        assertThat(launchErrors.get(0).get("code"))
            .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
    }

    @Test(
        priority = 26,
        description =
            "POST /projects with valid default connections then PUT /projects/{uid} with an unknown "
                + "personalizeProjectUids value (valid stack + launch) — expect 400 Bad request; "
                + "errors.connections.personalizeProjectUids[0].code lytics.PROJECTS.CONNECTION_NOT_FOUND"
    )
    public void TC_PUT_BY_UID_026_POST_valid_connections_then_PUT_invalid_personalize_expect_400() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT invalid personalize " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + valid POST body (default connections); When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                    + "project in this org. Free the connection or delete the conflicting project."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithValidStackLaunchAndInvalidPersonalizeProjectUid(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body same name/domain/description but personalizeProjectUids replaced with unknown uid; "
                + "When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        reportStep(
            "Assert 400 Bad request; errors.connections.personalizeProjectUids CONNECTION_NOT_FOUND"
        );
        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} with unknown personalize project uid must return 400 Bad request")
            .isEqualTo(400);
        assertThat(putResponse.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(putResponse.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> personalizeErrors =
            putResponse.jsonPath().getList("errors['connections.personalizeProjectUids']");
        assertThat(personalizeErrors).as("errors.connections.personalizeProjectUids").hasSize(1);
        assertThat(personalizeErrors.get(0).get("code"))
            .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
    }

    @Test(
        priority = 27,
        description =
            "POST /projects with valid default connections then PUT /projects/{uid} with mismatched UID types "
                + "(JSON numbers in stackApiKeys, launchProjectUids, personalizeProjectUids) — expect 400 Bad request; "
                + "CONNECTION_NOT_FOUND on all three connections.* error arrays"
    )
    public void TC_PUT_BY_UID_027_POST_valid_connections_then_PUT_non_string_connection_values_expect_400() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT non-string conn " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + valid POST body (default connections); When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                    + "project in this org. Free the connection or delete the conflicting project."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithNonStringConnectionValues(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body with JSON numbers (non-string) in each connections array; When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        reportStep(
            "Assert 400 Bad request; CONNECTION_NOT_FOUND on stack, launch, and personalize connection errors"
        );
        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} with non-string connection values must return 400 Bad request")
            .isEqualTo(400);
        assertThat(putResponse.jsonPath().getString("message")).isEqualTo("Bad request");
        assertThat(putResponse.jsonPath().getInt("status")).isEqualTo(400);

        List<Map<String, Object>> stackErrors =
            putResponse.jsonPath().getList("errors['connections.stackApiKeys']");
        assertThat(stackErrors).as("errors.connections.stackApiKeys").hasSize(1);
        assertThat(stackErrors.get(0).get("code"))
            .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");

        List<Map<String, Object>> launchErrors =
            putResponse.jsonPath().getList("errors['connections.launchProjectUids']");
        assertThat(launchErrors).as("errors.connections.launchProjectUids").hasSize(1);
        assertThat(launchErrors.get(0).get("code"))
            .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");

        List<Map<String, Object>> personalizeErrors =
            putResponse.jsonPath().getList("errors['connections.personalizeProjectUids']");
        assertThat(personalizeErrors).as("errors.connections.personalizeProjectUids").hasSize(1);
        assertThat(personalizeErrors.get(0).get("code"))
            .isEqualTo("lytics.PROJECTS.CONNECTION_NOT_FOUND");
    }

    @Test(
        priority = 28,
        description =
            "Curl-shaped POST /projects (single connection ids) then PUT /projects/{uid} with each array listing the "
                + "same id twice (www.contentstack.com, fixed project name) — expect 200/204; GET project matches "
                + "curl example: deduplicated connections, organizationUid from config, cdp.status active, audit fields"
    )
    public void TC_PUT_BY_UID_028_POST_valid_connections_then_PUT_duplicate_uids_per_array_deduped_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        // POST /projects — same JSON shape as manual curl (single value per connection array).
        final String curlProjectName = "DNI Test duplicate connections";
        final String curlDomain = "www.contentstack.com";
        final String curlDescription = "This is a sample project description";

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                curlProjectName,
                curlDomain,
                curlDescription,
                LyticsProjectPayloadBuilder.defaultConnections()
            );

        reportStep(
            "Given: Lytics headers + curl-shaped POST body (single stack/launch/personalize); When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400) {
            if ("lytics.PROJECTS.DUPLICATE_PROJECT_NAME".equals(
                    postResponse.jsonPath().getString("errors.name[0].code"))) {
                throw new SkipException(
                    "POST DUPLICATE_PROJECT_NAME: delete the existing project named '"
                        + curlProjectName
                        + "' or pick another environment."
                );
            }
            if ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))) {
                throw new SkipException(
                    "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                        + "project in this org. Free the connection or delete the conflicting project."
                );
            }
        }

        assertThat(postStatus)
            .as("POST /projects with curl-shaped body must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        reportStep("Assert POST 201 response echoes curl-shaped name, domain, description and single connection ids");
        assertThat(postResponse.jsonPath().getString("name")).isEqualTo(curlProjectName);
        assertThat(postResponse.jsonPath().getString("domain")).isEqualTo(curlDomain);
        assertThat(postResponse.jsonPath().getString("description")).isEqualTo(curlDescription);
        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        // PUT /projects/{uid} — same scalars; connections list each id twice (curl duplicate-array shape).
        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                curlProjectName,
                curlDomain,
                curlDescription,
                LyticsProjectPayloadBuilder.connectionsWithDuplicateIdsPerField()
            );

        reportStep(
            "Given: curl-shaped PUT body (duplicate ids per connections array); When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 400
            && ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString(
                        "errors['connections.personalizeProjectUids'][0].code")))) {
            throw new SkipException(
                "PUT returned DUPLICATE_CONNECTION while sending duplicate entries of the same org UIDs — "
                    + "environment or API behaviour mismatch; investigate before re-running."
            );
        }

        assertThat(putStatus)
            .as("PUT /projects/{uid} with duplicate connection values in arrays must succeed (200 or 204)")
            .isIn(200, 204);

        if (putStatus == 200) {
            reportStep("200 path: assert PUT response mirrors GET shape (deduplicated connections, same scalars)");
            assertThat(putResponse.jsonPath().getString("uid")).isEqualTo(projectUid);
            assertThat(putResponse.jsonPath().getString("name")).isEqualTo(curlProjectName);
            assertThat(putResponse.jsonPath().getString("domain")).isEqualTo(curlDomain);
            assertThat(putResponse.jsonPath().getString("description")).isEqualTo(curlDescription);
            assertThat(putResponse.jsonPath().getString("organizationUid"))
                .isEqualTo(TestConfig.lyticsOrganizationUid());
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys"))
                .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids"))
                .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
            ProjectAssertions.assertConnectionsArraysHaveNoDuplicateEntries(putResponse);
            assertThat(putResponse.jsonPath().getString("cdp.status")).isEqualTo("active");
            Object putCdpAid = putResponse.jsonPath().get("cdp.aid");
            assertThat(putCdpAid).as("PUT cdp.aid").isNotNull();
            assertThat(putResponse.jsonPath().getString("cdp.orgId")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("cdp.accountId")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("cdp.syncedAt")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("createdBy")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("createdAt")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("updatedBy")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("updatedAt")).isNotBlank();
        }

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT with duplicate connection payloads")
            .isEqualTo(200);

        reportStep(
            "Assert GET project matches curl example: uid, scalars, organizationUid, deduplicated connections, cdp, audit"
        );
        assertThat(getResponse.jsonPath().getString("uid")).isEqualTo(projectUid);
        assertThat(getResponse.jsonPath().getString("name")).isEqualTo(curlProjectName);
        assertThat(getResponse.jsonPath().getString("domain")).isEqualTo(curlDomain);
        assertThat(getResponse.jsonPath().getString("description")).isEqualTo(curlDescription);
        assertThat(getResponse.jsonPath().getString("organizationUid"))
            .isEqualTo(TestConfig.lyticsOrganizationUid());
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
        ProjectAssertions.assertConnectionsArraysHaveNoDuplicateEntries(getResponse);

        assertThat(getResponse.jsonPath().getString("cdp.status")).isEqualTo("active");
        Object getCdpAid = getResponse.jsonPath().get("cdp.aid");
        assertThat(getCdpAid).as("GET cdp.aid").isNotNull();
        assertThat(getResponse.jsonPath().getString("cdp.orgId")).isNotBlank();
        assertThat(getResponse.jsonPath().getString("cdp.accountId")).isNotBlank();
        assertThat(getResponse.jsonPath().getString("cdp.syncedAt")).isNotBlank();

        assertThat(getResponse.jsonPath().getString("createdBy")).isNotBlank();
        assertThat(getResponse.jsonPath().getString("createdAt")).isNotBlank();
        assertThat(getResponse.jsonPath().getString("updatedBy")).isNotBlank();
        assertThat(getResponse.jsonPath().getString("updatedAt")).isNotBlank();
    }

    @Test(
        priority = 29,
        description =
            "POST /projects with default connections then PUT removes all connections (all three arrays []); "
                + "www.contentstack.com scalars — expect 200/204; GET empty connections; organizationUid, cdp, audit "
                + "(VALID_DOMAIN variant: TC_PUT_BY_UID_037)"
    )
    public void TC_PUT_BY_UID_029_POST_with_connections_then_PUT_empty_connection_arrays_expect_empty_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        final String projectName = "DNI Test empty connections " + UUID.randomUUID();
        final String curlDomain = "www.contentstack.com";
        final String curlDescription = "This is a sample project description";

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                curlDomain,
                curlDescription,
                LyticsProjectPayloadBuilder.defaultConnections()
            );

        reportStep(
            "Given: POST body with populated connections; When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400) {
            if ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))) {
                throw new SkipException(
                    "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another "
                        + "project in this org. Free the connection or delete the conflicting project."
                );
            }
        }

        assertThat(postStatus)
            .as("POST /projects with unique name must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                curlDomain,
                curlDescription,
                LyticsProjectPayloadBuilder.connectionsWithAllEmptyArrays()
            );

        reportStep(
            "Given: PUT body with empty stackApiKeys, launchProjectUids, personalizeProjectUids; When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} clearing all connection arrays must succeed (200 or 204)")
            .isIn(200, 204);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 200) {
            reportStep("200 path: assert PUT response shows empty connection arrays and same scalars");
            assertThat(putResponse.jsonPath().getString("uid")).isEqualTo(projectUid);
            assertThat(putResponse.jsonPath().getString("name")).isEqualTo(projectName);
            assertThat(putResponse.jsonPath().getString("domain")).isEqualTo(curlDomain);
            assertThat(putResponse.jsonPath().getString("description")).isEqualTo(curlDescription);
            assertThat(putResponse.jsonPath().getString("organizationUid"))
                .isEqualTo(TestConfig.lyticsOrganizationUid());
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
            assertThat(putResponse.jsonPath().getString("cdp.status")).isEqualTo("active");
            Object putCdpAid = putResponse.jsonPath().get("cdp.aid");
            assertThat(putCdpAid).as("PUT cdp.aid").isNotNull();
            assertThat(putResponse.jsonPath().getString("cdp.orgId")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("cdp.accountId")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("cdp.syncedAt")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("createdBy")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("createdAt")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("updatedBy")).isNotBlank();
            assertThat(putResponse.jsonPath().getString("updatedAt")).isNotBlank();
        }

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT with empty connection arrays")
            .isEqualTo(200);

        reportStep(
            "Assert GET project: empty connections; scalars; organizationUid; cdp; audit fields"
        );
        assertThat(getResponse.jsonPath().getString("uid")).isEqualTo(projectUid);
        assertThat(getResponse.jsonPath().getString("name")).isEqualTo(projectName);
        assertThat(getResponse.jsonPath().getString("domain")).isEqualTo(curlDomain);
        assertThat(getResponse.jsonPath().getString("description")).isEqualTo(curlDescription);
        assertThat(getResponse.jsonPath().getString("organizationUid"))
            .isEqualTo(TestConfig.lyticsOrganizationUid());
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();

        assertThat(getResponse.jsonPath().getString("cdp.status")).isEqualTo("active");
        Object getCdpAid = getResponse.jsonPath().get("cdp.aid");
        assertThat(getCdpAid).as("GET cdp.aid").isNotNull();
        assertThat(getResponse.jsonPath().getString("cdp.orgId")).isNotBlank();
        assertThat(getResponse.jsonPath().getString("cdp.accountId")).isNotBlank();
        assertThat(getResponse.jsonPath().getString("cdp.syncedAt")).isNotBlank();

        assertThat(getResponse.jsonPath().getString("createdBy")).isNotBlank();
        assertThat(getResponse.jsonPath().getString("createdAt")).isNotBlank();
        assertThat(getResponse.jsonPath().getString("updatedBy")).isNotBlank();
        assertThat(getResponse.jsonPath().getString("updatedAt")).isNotBlank();
    }

    @Test(
        priority = 30,
        description =
            "POST /projects with connections field omitted (see ProjectPostApiTest#TC_045) then PUT /projects/{uid} "
                + "with only stackApiKeys set (launch and personalize empty arrays) — expect 200/204; GET shows stack "
                + "only"
    )
    public void TC_PUT_BY_UID_030_POST_without_connections_then_PUT_stack_only_expect_stack_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI POST no conn stack PUT " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + body with name, domain, description; connections key absent; When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        assertThat(postStatus)
            .as("POST /projects without connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        reportStep("Assert POST 201 echoes empty connection arrays (same as TC_045 201 path)");
        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsOnlyStackApiKeys()
            );

        reportStep(
            "Given: PUT body with connections: stackApiKeys only, launch and personalize []; When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                putResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "PUT returned DUPLICATE_CONNECTION: stack API key is already linked to another project in this org."
            );
        }

        assertThat(putStatus)
            .as("PUT /projects/{uid} with stack-only connections must succeed (200 or 204)")
            .isIn(200, 204);

        if (putStatus == 200) {
            reportStep("200 path: assert PUT response has stack only");
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys"))
                .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
        }

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT with stack-only connections")
            .isEqualTo(200);

        reportStep("Assert GET connections: stackApiKeys populated; launch and personalize empty");
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
    }

    @Test(
        priority = 31,
        description =
            "POST /projects with connections field omitted then PUT /projects/{uid} with only launchProjectUids "
                + "(stack and personalize keys absent in payload — see projectCreatePayloadWithConnectionsOnlyLaunchProjectUids) "
                + "— expect 200/204; GET shows launch only"
    )
    public void TC_PUT_BY_UID_031_POST_without_connections_then_PUT_launch_only_expect_launch_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI POST no conn launch PUT " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + body with name, domain, description; connections key absent; When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        assertThat(postStatus)
            .as("POST /projects without connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        reportStep("Assert POST 201 echoes empty connection arrays");
        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithConnectionsOnlyLaunchProjectUids(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body with connections: launchProjectUids only (stack and personalize omitted); When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                putResponse.jsonPath().getString("errors['connections.launchProjectUids'][0].code")
            )) {
            throw new SkipException(
                "PUT returned DUPLICATE_CONNECTION: launch project uid is already linked to another project in this org."
            );
        }

        assertThat(putStatus)
            .as("PUT /projects/{uid} with launch-only connections must succeed (200 or 204)")
            .isIn(200, 204);

        if (putStatus == 200) {
            reportStep("200 path: assert PUT response has launch only");
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids"))
                .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
        }

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT with launch-only connections")
            .isEqualTo(200);

        reportStep("Assert GET connections: launchProjectUids populated; stack and personalize empty");
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
    }

    @Test(
        priority = 32,
        description =
            "POST /projects with connections field omitted then PUT /projects/{uid} with only personalizeProjectUids "
                + "(stack and launch keys absent — projectCreatePayloadWithConnectionsOnlyPersonalizeProjectUids) "
                + "— expect 200/204; GET shows personalize only"
    )
    public void TC_PUT_BY_UID_032_POST_without_connections_then_PUT_personalize_only_expect_personalize_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI POST no conn personalize PUT " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + body with name, domain, description; connections key absent; When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        assertThat(postStatus)
            .as("POST /projects without connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        reportStep("Assert POST 201 echoes empty connection arrays");
        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithConnectionsOnlyPersonalizeProjectUids(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body with connections: personalizeProjectUids only (stack and launch omitted); When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                putResponse.jsonPath().getString("errors['connections.personalizeProjectUids'][0].code")
            )) {
            throw new SkipException(
                "PUT returned DUPLICATE_CONNECTION: personalize project uid is already linked to another project "
                    + "in this org."
            );
        }

        assertThat(putStatus)
            .as("PUT /projects/{uid} with personalize-only connections must succeed (200 or 204)")
            .isIn(200, 204);

        if (putStatus == 200) {
            reportStep("200 path: assert PUT response has personalize only");
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
        }

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT with personalize-only connections")
            .isEqualTo(200);

        reportStep("Assert GET connections: personalizeProjectUids populated; stack and launch empty");
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
    }

    @Test(
        priority = 33,
        description =
            "POST /projects with default connections (stack + launch + personalize) then PUT /projects/{uid} with "
                + "stackApiKeys [] and same launch/personalize — expect 200/204; GET shows no stack key; launch and "
                + "personalize unchanged"
    )
    public void TC_PUT_BY_UID_033_POST_with_connections_then_PUT_remove_stack_expect_launch_personalize_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT remove stack " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: POST body with default connections; When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another project in this org."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with default connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        reportStep("Assert POST response has all three connection ids");
        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsWithLaunchPersonalizeAndEmptyStackApiKeys()
            );

        reportStep(
            "Given: PUT body with stackApiKeys [] and launch/personalize unchanged; When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} removing stack connection must succeed (200 or 204)")
            .isIn(200, 204);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 200) {
            reportStep("200 path: assert PUT response has empty stack; launch and personalize unchanged");
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids"))
                .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
        }

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT removing stack connection")
            .isEqualTo(200);

        reportStep("Assert GET: stackApiKeys empty; launch and personalize match prior connections");
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
    }

    @Test(
        priority = 34,
        description =
            "POST /projects with default connections then PUT /projects/{uid} with launchProjectUids [] and same "
                + "stack/personalize — expect 200/204; GET shows no launch uid; stack and personalize unchanged"
    )
    public void TC_PUT_BY_UID_034_POST_with_connections_then_PUT_remove_launch_expect_stack_personalize_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT remove launch " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: POST body with default connections; When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another project in this org."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with default connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        reportStep("Assert POST response has all three connection ids");
        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsWithStackPersonalizeAndEmptyLaunchProjectUids()
            );

        reportStep(
            "Given: PUT body with launchProjectUids [] and stack/personalize unchanged; When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} removing launch connection must succeed (200 or 204)")
            .isIn(200, 204);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 200) {
            reportStep("200 path: assert PUT response has empty launch; stack and personalize unchanged");
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys"))
                .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
        }

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT removing launch connection")
            .isEqualTo(200);

        reportStep("Assert GET: launchProjectUids empty; stack and personalize match prior connections");
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
    }

    @Test(
        priority = 35,
        description =
            "POST /projects with default connections then PUT /projects/{uid} with personalizeProjectUids [] and "
                + "same stack/launch — expect 200/204; GET shows no personalize uid; stack and launch unchanged"
    )
    public void TC_PUT_BY_UID_035_POST_with_connections_then_PUT_remove_personalize_expect_stack_launch_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT remove personalize " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: POST body with default connections; When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another project in this org."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with default connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        reportStep("Assert POST response has all three connection ids");
        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsWithStackLaunchAndEmptyPersonalizeProjectUids()
            );

        reportStep(
            "Given: PUT body with personalizeProjectUids [] and stack/launch unchanged; When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} removing personalize connection must succeed (200 or 204)")
            .isIn(200, 204);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 200) {
            reportStep("200 path: assert PUT response has empty personalize; stack and launch unchanged");
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys"))
                .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids"))
                .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
        }

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT removing personalize connection")
            .isEqualTo(200);

        reportStep("Assert GET: personalizeProjectUids empty; stack and launch match prior connections");
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
    }

    @Test(
        priority = 36,
        description =
            "POST /projects with connections field omitted then PUT /projects/{uid} with full default connections "
                + "(stack + launch + personalize) — expect 200/204; GET shows all three connection ids"
    )
    public void TC_PUT_BY_UID_036_POST_without_connections_then_PUT_add_all_connections_expect_full_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI POST no conn PUT all " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: Lytics headers + body with name, domain, description; connections key absent; When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        assertThat(postStatus)
            .as("POST /projects without connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        reportStep("Assert POST 201 echoes empty connection arrays");
        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: PUT body with default connections (all three ids); When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 400
            && ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString(
                        "errors['connections.personalizeProjectUids'][0].code")))) {
            throw new SkipException(
                "PUT returned DUPLICATE_CONNECTION: one or more connection ids are already linked to another "
                    + "project in this org. Free those connections or use alternate test data."
            );
        }

        assertThat(putStatus)
            .as("PUT /projects/{uid} adding full default connections must succeed (200 or 204)")
            .isIn(200, 204);

        if (putStatus == 200) {
            reportStep("200 path: assert PUT response has all three default connection ids");
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys"))
                .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids"))
                .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
        }

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT adding all connections")
            .isEqualTo(200);

        reportStep("Assert GET connections: stack, launch, and personalize populated with default test ids");
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
    }

    @Test(
        priority = 37,
        description =
            "POST /projects with default connections (inverse of TC_PUT_BY_UID_036) then PUT /projects/{uid} with "
                + "connectionsWithAllEmptyArrays — remove all stack, launch, and personalize — expect 200/204; GET shows "
                + "empty connection arrays (see TC_PUT_BY_UID_029 for www.contentstack.com + full cdp assertions)"
    )
    public void TC_PUT_BY_UID_037_POST_with_connections_then_PUT_remove_all_connections_expect_empty_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT remove all conn " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep(
            "Given: POST body with default connections (all three ids); When: POST "
                + ApiPaths.PROJECTS
                + "; Then: extract response"
        );

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another project in this org."
            );
        }

        assertThat(postStatus)
            .as("POST /projects with default connections must return 201 Created")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        reportStep("Assert POST 201 has all three connection ids before clearing on PUT");
        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsWithAllEmptyArrays()
            );

        reportStep(
            "Given: PUT body removes all connections (empty arrays); When: PUT "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT /projects/{uid} clearing all connection arrays must succeed (200 or 204)")
            .isIn(200, 204);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 200) {
            reportStep("200 path: assert PUT response has empty connection arrays");
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
        }

        reportStep(
            "Given: Lytics headers; When: GET "
                + ApiPaths.projectByUid(projectUid)
                + "; Then: extract response"
        );

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode())
            .as("GET /projects/{uid} after PUT removing all connections")
            .isEqualTo(200);

        reportStep("Assert GET: all connection arrays empty");
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
    }

    @Test(
        priority = 38,
        description =
            "POST /projects with full default connections then PUT removes stack and launch (personalize only) — "
                + "connectionsWithPersonalizeOnlyAndEmptyStackAndLaunch; expect 200/204; GET personalize only"
    )
    public void TC_PUT_BY_UID_038_POST_full_connections_then_PUT_remove_stack_and_launch_expect_personalize_only() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT drop stack+launch " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep("Given: POST body with full default connections; When: POST " + ApiPaths.PROJECTS);

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another project in this org."
            );
        }

        assertThat(postStatus).as("POST with full connections").isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsWithPersonalizeOnlyAndEmptyStackAndLaunch()
            );

        reportStep(
            "Given: PUT clears stack and launch (personalize only); When: PUT "
                + ApiPaths.projectByUid(projectUid)
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT removing stack and launch")
            .isIn(200, 204);

        if (putResponse.getStatusCode() == 200) {
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
        }

        reportStep("When: GET " + ApiPaths.projectByUid(projectUid) + "; Then: assert personalize only");

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode()).isEqualTo(200);
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
    }

    @Test(
        priority = 39,
        description =
            "POST /projects with full default connections then PUT removes launch and personalize (stack only) — "
                + "connectionsOnlyStackApiKeys; expect 200/204; GET stack only"
    )
    public void TC_PUT_BY_UID_039_POST_full_connections_then_PUT_remove_launch_and_personalize_expect_stack_only() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT drop launch+pers " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep("Given: POST body with full default connections; When: POST " + ApiPaths.PROJECTS);

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another project in this org."
            );
        }

        assertThat(postStatus).as("POST with full connections").isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsOnlyStackApiKeys()
            );

        reportStep(
            "Given: PUT clears launch and personalize (stack only); When: PUT "
                + ApiPaths.projectByUid(projectUid)
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT removing launch and personalize")
            .isIn(200, 204);

        if (putResponse.getStatusCode() == 200) {
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys"))
                .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
        }

        reportStep("When: GET " + ApiPaths.projectByUid(projectUid) + "; Then: assert stack only");

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode()).isEqualTo(200);
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
    }

    @Test(
        priority = 40,
        description =
            "POST /projects with full default connections then PUT removes stack and personalize (launch only) — "
                + "connectionsWithLaunchOnlyAndEmptyStackAndPersonalize; expect 200/204; GET launch only"
    )
    public void TC_PUT_BY_UID_040_POST_full_connections_then_PUT_remove_stack_and_personalize_expect_launch_only() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI PUT drop stack+pers " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainAndDescription(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep("Given: POST body with full default connections; When: POST " + ApiPaths.PROJECTS);

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        int postStatus = postResponse.getStatusCode();
        if (postStatus == 400
            && "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                postResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code")
            )) {
            throw new SkipException(
                "POST returned DUPLICATE_CONNECTION: stackApiKey is already linked to another project in this org."
            );
        }

        assertThat(postStatus).as("POST with full connections").isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsWithLaunchOnlyAndEmptyStackAndPersonalize()
            );

        reportStep(
            "Given: PUT clears stack and personalize (launch only); When: PUT "
                + ApiPaths.projectByUid(projectUid)
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        assertThat(putResponse.getStatusCode())
            .as("PUT removing stack and personalize")
            .isIn(200, 204);

        if (putResponse.getStatusCode() == 200) {
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids"))
                .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
        }

        reportStep("When: GET " + ApiPaths.projectByUid(projectUid) + "; Then: assert launch only");

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode()).isEqualTo(200);
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
    }

    @Test(
        priority = 41,
        description =
            "POST /projects without connections then PUT adds stack + launch only (personalize []) — "
                + "connectionsWithStackLaunchAndEmptyPersonalizeProjectUids; expect 200/204; GET matches"
    )
    public void TC_PUT_BY_UID_041_POST_without_connections_then_PUT_add_stack_and_launch_expect_pair_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI POST no conn PUT stack+launch " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep("Given: POST without connections; When: POST " + ApiPaths.PROJECTS);

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        assertThat(postResponse.getStatusCode())
            .as("POST without connections")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsWithStackLaunchAndEmptyPersonalizeProjectUids()
            );

        reportStep(
            "Given: PUT adds stack + launch; When: PUT " + ApiPaths.projectByUid(projectUid)
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 400
            && ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString(
                        "errors['connections.personalizeProjectUids'][0].code")))) {
            throw new SkipException(
                "PUT returned DUPLICATE_CONNECTION: free stack/launch/personalize test ids or use alternate data."
            );
        }

        assertThat(putStatus).as("PUT adding stack and launch").isIn(200, 204);

        if (putStatus == 200) {
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys"))
                .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids"))
                .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
        }

        reportStep("When: GET " + ApiPaths.projectByUid(projectUid) + "; Then: assert stack + launch only");

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode()).isEqualTo(200);
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();
    }

    @Test(
        priority = 42,
        description =
            "POST /projects without connections then PUT adds launch + personalize only (stack []) — "
                + "connectionsWithLaunchPersonalizeAndEmptyStackApiKeys; expect 200/204; GET matches"
    )
    public void TC_PUT_BY_UID_042_POST_without_connections_then_PUT_add_launch_and_personalize_expect_pair_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI POST no conn PUT launch+pers " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep("Given: POST without connections; When: POST " + ApiPaths.PROJECTS);

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        assertThat(postResponse.getStatusCode())
            .as("POST without connections")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsWithLaunchPersonalizeAndEmptyStackApiKeys()
            );

        reportStep(
            "Given: PUT adds launch + personalize; When: PUT " + ApiPaths.projectByUid(projectUid)
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 400
            && ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString(
                        "errors['connections.personalizeProjectUids'][0].code")))) {
            throw new SkipException(
                "PUT returned DUPLICATE_CONNECTION: free stack/launch/personalize test ids or use alternate data."
            );
        }

        assertThat(putStatus).as("PUT adding launch and personalize").isIn(200, 204);

        if (putStatus == 200) {
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids"))
                .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
        }

        reportStep("When: GET " + ApiPaths.projectByUid(projectUid) + "; Then: assert launch + personalize only");

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode()).isEqualTo(200);
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids"))
            .containsExactly(LyticsProjectTestData.LAUNCH_PROJECT_UID);
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
    }

    @Test(
        priority = 43,
        description =
            "POST /projects without connections then PUT adds stack + personalize only (launch []) — "
                + "connectionsWithStackPersonalizeAndEmptyLaunchProjectUids; expect 200/204; GET matches"
    )
    public void TC_PUT_BY_UID_043_POST_without_connections_then_PUT_add_stack_and_personalize_expect_pair_on_read() {
        reportStep("Reset cleanup uid so DELETE runs after a successful POST");
        projectUidToCleanup = null;

        String projectName = "DNI POST no conn PUT stack+pers " + UUID.randomUUID();

        Map<String, Object> postBody =
            LyticsProjectPayloadBuilder.projectCreatePayloadWithoutConnectionsField(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION
            );

        reportStep("Given: POST without connections; When: POST " + ApiPaths.PROJECTS);

        Response postResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(postBody)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();

        postResponse.prettyPrint();
        reportResponseBody(postResponse);

        assertThat(postResponse.getStatusCode())
            .as("POST without connections")
            .isEqualTo(201);

        String projectUid = postResponse.jsonPath().getString("uid");
        assertThat(projectUid).isNotBlank();
        projectUidToCleanup = projectUid;

        assertThat(postResponse.jsonPath().getList("connections.stackApiKeys")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(postResponse.jsonPath().getList("connections.personalizeProjectUids")).isEmpty();

        Map<String, Object> putBody =
            LyticsProjectPayloadBuilder.validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                projectName,
                LyticsProjectTestData.VALID_DOMAIN,
                LyticsProjectTestData.VALID_DESCRIPTION,
                LyticsProjectPayloadBuilder.connectionsWithStackPersonalizeAndEmptyLaunchProjectUids()
            );

        reportStep(
            "Given: PUT adds stack + personalize; When: PUT " + ApiPaths.projectByUid(projectUid)
        );

        Response putResponse =
            given()
                .spec(lyticsRequestSpec)
                .body(putBody)
                .when()
                .put(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        putResponse.prettyPrint();
        reportResponseBody(putResponse);

        int putStatus = putResponse.getStatusCode();
        if (putStatus == 400
            && ("lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.stackApiKeys'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString("errors['connections.launchProjectUids'][0].code"))
                || "lytics.PROJECTS.DUPLICATE_CONNECTION".equals(
                    putResponse.jsonPath().getString(
                        "errors['connections.personalizeProjectUids'][0].code")))) {
            throw new SkipException(
                "PUT returned DUPLICATE_CONNECTION: free stack/launch/personalize test ids or use alternate data."
            );
        }

        assertThat(putStatus).as("PUT adding stack and personalize").isIn(200, 204);

        if (putStatus == 200) {
            assertThat(putResponse.jsonPath().getList("connections.stackApiKeys"))
                .containsExactly(LyticsProjectTestData.STACK_API_KEY);
            assertThat(putResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
            assertThat(putResponse.jsonPath().getList("connections.personalizeProjectUids"))
                .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
        }

        reportStep("When: GET " + ApiPaths.projectByUid(projectUid) + "; Then: assert stack + personalize only");

        Response getResponse =
            given()
                .spec(lyticsRequestSpec)
                .when()
                .get(ApiPaths.projectByUid(projectUid))
                .then()
                .extract()
                .response();

        getResponse.prettyPrint();
        reportResponseBody(getResponse);

        assertThat(getResponse.getStatusCode()).isEqualTo(200);
        assertThat(getResponse.jsonPath().getList("connections.stackApiKeys"))
            .containsExactly(LyticsProjectTestData.STACK_API_KEY);
        assertThat(getResponse.jsonPath().getList("connections.launchProjectUids")).isEmpty();
        assertThat(getResponse.jsonPath().getList("connections.personalizeProjectUids"))
            .containsExactly(LyticsProjectTestData.PERSONALIZE_PROJECT_UID);
    }

}
