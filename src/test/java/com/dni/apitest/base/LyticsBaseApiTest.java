package com.dni.apitest.base;

import com.dni.apitest.config.TestConfig;
import com.dni.apitest.constants.ApiPaths;
import com.dni.apitest.report.ReportSteps;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import static io.restassured.RestAssured.given;

/**
 * REST Assured setup for Lytics API: base URI and headers on {@link #lyticsRequestSpec}.
 */
public abstract class LyticsBaseApiTest {

    protected static RequestSpecification lyticsRequestSpec;
    protected static ResponseSpecification responseSpec;

    /**
     * Set as soon as a test creates a project (e.g. after POST returns 201 with a uid). {@link #cleanupLyticsProject()}
     * sends DELETE and clears this field.
     */
    protected String projectUidToCleanup;

    /** Log a numbered-style step to the Extent report for the current test method. */
    protected static void reportStep(String message) {
        ReportSteps.step(message);
    }

    /** Attach the response JSON body to the Extent report (pretty-printed when possible). */
    protected static void reportResponseBody(Response response) {
        if (response == null) {
            return;
        }
        String body = response.getBody() != null ? response.getBody().asPrettyString() : "";
        ReportSteps.jsonBlock("HTTP " + response.getStatusCode() + " — response body", body);
    }

    @BeforeClass(alwaysRun = true)
    public static void configureLyticsRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        lyticsRequestSpec = new RequestSpecBuilder()
                .setBaseUri(TestConfig.lyticsBaseUri())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("x-cs-api-version", TestConfig.lyticsApiVersion())
                .addHeader("organization_uid", TestConfig.lyticsOrganizationUid())
                .addHeader("authtoken", TestConfig.lyticsAuthToken())
                .log(LogDetail.URI)
                .build();

        responseSpec = new ResponseSpecBuilder()
                .log(LogDetail.STATUS)
                .build();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupLyticsProject() {
        if (projectUidToCleanup == null || projectUidToCleanup.isBlank()) {
            return;
        }
        String uid = projectUidToCleanup;
        projectUidToCleanup = null;

        try {
            given()
                    .spec(lyticsRequestSpec)
            .when()
                    .delete(ApiPaths.projectByUid(uid))
            .then()
                    .statusCode(204);
        } catch (Throwable e) {
            // TestNG marks the @Test as FAILED if @AfterMethod throws — even when assertions passed.
            // Cleanup is best-effort; log and continue so the report reflects the test outcome, not DELETE noise.
            System.err.println(
                    "[cleanup] Optional DELETE /projects/" + uid + " failed (ignored): " + e.getMessage());
        }
    }
}
