package com.dni.apitest.base;

import com.dni.apitest.config.TestConfig;
import com.dni.apitest.http.LyticsProjectApiClient;
import com.dni.apitest.listeners.ExtentTestNgListener;
import com.dni.apitest.report.ExtentReportRequestFilter;
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
import org.testng.annotations.Listeners;

/**
 * Base class for Lytics API tests.
 *
 * <p>Configures REST Assured with the Lytics base URI and required auth headers, then exposes
 * a shared {@link LyticsProjectApiClient} for use in test methods.  Automatic project cleanup
 * via {@link #cleanupLyticsProject()} runs after every test method.
 *
 * <p>{@link ExtentTestNgListener} is registered here (not only in {@code testng.xml}) so Extent
 * Reports still initialise and flush when a spec class is executed alone from the IDE.
 *
 * <p>Request bodies for REST Assured calls that use {@link #lyticsRequestSpec} are written to the
 * report automatically via {@link ExtentReportRequestFilter}.
 */
@Listeners(ExtentTestNgListener.class)
public abstract class BaseApiTest {

    /** Pre-built request spec carrying base URI + Lytics auth headers. */
    protected static RequestSpecification lyticsRequestSpec;
    protected static ResponseSpecification responseSpec;

    /**
     * Reusable HTTP client for POST /projects, PUT /projects/{uid}, and DELETE /projects/{uid}.
     * Initialised in {@link #configureLyticsRestAssured()} and ready for all test methods.
     */
    protected static LyticsProjectApiClient projectApiClient;

    /**
     * Set to the created project UID after a test creates a project (status 201).
     * {@link #cleanupLyticsProject()} deletes it and clears this field after each test.
     */
    protected String projectUidToCleanup;

    /** Logs a numbered step to the Extent report for the currently running test. */
    protected static void reportStep(String message) {
        ReportSteps.step(message);
    }

    /** Attaches the response body to the Extent report as a highlighted JSON block. */
    protected static void reportResponseBody(Response response) {
        if (response == null) {
            return;
        }
        String body = response.getBody() != null ? response.getBody().asPrettyString() : "";
        ReportSteps.jsonBlock("HTTP " + response.getStatusCode() + " — response body", body);
    }

    /**
     * Attaches a request payload to the Extent report (for tests that do not use {@link #lyticsRequestSpec}).
     * Calls that go through {@link #lyticsRequestSpec} already log the body via {@link ExtentReportRequestFilter}.
     */
    protected static void reportRequestPayload(String title, Object payload) {
        ReportSteps.requestPayloadBlock(title, payload);
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
                .addFilter(ExtentReportRequestFilter.INSTANCE)
                .log(LogDetail.URI)
                .build();

        responseSpec = new ResponseSpecBuilder()
                .log(LogDetail.STATUS)
                .build();

        projectApiClient = new LyticsProjectApiClient(
                TestConfig.lyticsBaseUri(), lyticsRequestSpec);
    }

    /**
     * Deletes the project created during the test, if any.  Best-effort: exceptions are
     * swallowed so a failed DELETE does not mask a passing test assertion.
     */
    @AfterMethod(alwaysRun = true)
    public void cleanupLyticsProject() {
        if (projectUidToCleanup == null || projectUidToCleanup.isBlank()) {
            return;
        }
        String uid = projectUidToCleanup;
        projectUidToCleanup = null;

        try {
            projectApiClient.deleteProject(uid)
                    .then()
                    .statusCode(204);
        } catch (Throwable e) {
            System.err.println(
                    "[cleanup] DELETE /projects/" + uid + " failed (ignored): " + e.getMessage());
        }
    }
}
