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

import java.util.Map;
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

}
