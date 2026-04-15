package com.dni.apitest.report;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.List;

/**
 * Logs JSON (and similar) request bodies to the active {@link com.aventstack.extentreports.ExtentTest}
 * as the request is sent. Use a single shared instance on the {@link io.restassured.specification.RequestSpecification}
 * so POST/PUT payloads appear in the Spark report next to response logs.
 */
public final class ExtentReportRequestFilter implements Filter {

    public static final ExtentReportRequestFilter INSTANCE = new ExtentReportRequestFilter();

    private ExtentReportRequestFilter() {}

    @Override
    public Response filter(
            FilterableRequestSpecification requestSpec,
            FilterableResponseSpecification responseSpec,
            FilterContext ctx) {
        logRequestIfNeeded(requestSpec);
        return ctx.next(requestSpec, responseSpec);
    }

    private static void logRequestIfNeeded(FilterableRequestSpecification requestSpec) {
        @SuppressWarnings("rawtypes")
        List multiparts = requestSpec.getMultiPartParams();
        if (multiparts != null && !multiparts.isEmpty()) {
            String title = titlePrefix(requestSpec) + " — multipart request";
            ReportSteps.detail(
                    title + " (" + multiparts.size() + " part(s); file/binary parts not expanded here)");
            return;
        }

        Object body = requestSpec.getBody();
        if (body == null) {
            return;
        }

        String blockTitle = titlePrefix(requestSpec) + " — request body";
        ReportSteps.jsonBlock(blockTitle, ReportSteps.formatPayloadForReport(body));
    }

    private static String titlePrefix(FilterableRequestSpecification requestSpec) {
        return requestSpec.getMethod() + " " + requestSpec.getDerivedPath();
    }
}
