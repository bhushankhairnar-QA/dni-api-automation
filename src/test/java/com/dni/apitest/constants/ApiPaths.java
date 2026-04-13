package com.dni.apitest.constants;

/**
 * Central path fragments for APIs under {@link com.dni.apitest.config.TestConfig#baseUri()}.
 */
public final class ApiPaths {

    private ApiPaths() {}

    /** Example: JSONPlaceholder posts collection */
    public static final String POSTS = "/posts";

    /** Lytics projects collection (under {@link com.dni.apitest.config.TestConfig#lyticsBaseUri()}). */
    public static final String PROJECTS = "/projects";

    public static String projectByUid(String uid) {
        return PROJECTS + "/" + uid;
    }

    public static String postById(int id) {
        return POSTS + "/" + id;
    }
}
