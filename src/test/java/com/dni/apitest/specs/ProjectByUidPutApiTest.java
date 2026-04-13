package com.dni.apitest.specs;

import com.dni.apitest.base.BaseApiTest;
import com.dni.apitest.constants.ApiPaths;
import com.dni.apitest.testdata.LyticsProjectPayloadBuilder;
import com.dni.apitest.testdata.LyticsProjectTestData;
import io.restassured.response.Response;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * PUT /projects/{uid} — update flow aligned with manual curls:
 *
 * <p>POST {@code /projects} then PUT with the same shape; each test varies one field. After GET
 * {@code /projects/{uid}}, assertions are limited to the field under test (name or domain only).
 */
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
}
