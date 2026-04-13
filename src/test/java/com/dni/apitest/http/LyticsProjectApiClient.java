package com.dni.apitest.http;

import com.dni.apitest.constants.ApiPaths;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Reusable REST Assured HTTP client for the Lytics Projects API.
 *
 * <p>Standard test methods use {@link #listProjects()}, {@link #getProject(String)},
 * {@link #createProject(Map)}, and {@link #deleteProject(String)}, which operate on the pre-built
 * {@code lyticsRequestSpec}.
 * Header-validation tests that need
 * to supply or omit specific headers should call
 * {@link #createProjectWithHeaders(Map, Map)}.
 */
public class LyticsProjectApiClient {

    private final String baseUri;
    private final RequestSpecification requestSpec;

    public LyticsProjectApiClient(String baseUri, RequestSpecification requestSpec) {
        this.baseUri = baseUri;
        this.requestSpec = requestSpec;
    }

    /**
     * GET /projects — returns all projects for the organization implied by {@code organization_uid}
     * on the shared request spec.
     */
    public Response listProjects() {
        return given()
                .spec(requestSpec)
                .when()
                .get(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();
    }

    /**
     * GET /projects/{uid} — returns one project for the organization implied by {@code organization_uid}
     * on the shared request spec.
     */
    public Response getProject(String uid) {
        return given()
                .spec(requestSpec)
                .when()
                .get(ApiPaths.projectByUid(uid))
                .then()
                .extract()
                .response();
    }

    /**
     * POST /projects with the supplied payload using the shared {@code lyticsRequestSpec}
     * (base URI + JSON content-type + Lytics auth headers already included).
     */
    public Response createProject(Map<String, Object> payload) {
        return given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(ApiPaths.PROJECTS)
                .then()
                .extract()
                .response();
    }

    /**
     * DELETE /projects/{uid} — used by cleanup after tests that create a project.
     */
    public Response deleteProject(String uid) {
        return given()
                .spec(requestSpec)
                .when()
                .delete(ApiPaths.projectByUid(uid))
                .then()
                .extract()
                .response();
    }

    /**
     * POST /projects with an explicitly constructed set of headers.  Use this method in
     * header-validation tests where specific headers must be omitted, overridden, or set to
     * invalid values.
     *
     * @param payload request body
     * @param headers map of header name → value; only these headers are sent (no implicit spec headers)
     */
    public Response createProjectWithHeaders(Map<String, Object> payload, Map<String, String> headers) {
        RequestSpecification req = given()
                .baseUri(baseUri)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(payload);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            req = req.header(entry.getKey(), entry.getValue());
        }

        return req.when().post(ApiPaths.PROJECTS).then().extract().response();
    }
}
