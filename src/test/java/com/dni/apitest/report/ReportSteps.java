package com.dni.apitest.report;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Small helpers so tests can log numbered steps and JSON to the active Extent test.
 */
public final class ReportSteps {

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    private ReportSteps() {}

    public static void step(String message) {
        ExtentTest test = ExtentManager.currentTest();
        if (test != null) {
            test.info("<b>Step:</b> " + escapeHtml(message));
        }
    }

    public static void detail(String message) {
        ExtentTest test = ExtentManager.currentTest();
        if (test != null) {
            test.info(escapeHtml(message));
        }
    }

    public static void pass(String message) {
        ExtentTest test = ExtentManager.currentTest();
        if (test != null) {
            test.pass(escapeHtml(message));
        }
    }

    public static void jsonBlock(String title, String json) {
        ExtentTest test = ExtentManager.currentTest();
        if (test == null) {
            return;
        }
        if (title != null && !title.isBlank()) {
            test.info("<b>" + escapeHtml(title) + "</b>");
        }
        if (json == null || json.isBlank()) {
            test.info("(empty body)");
            return;
        }
        test.info(MarkupHelper.createCodeBlock(json, CodeLanguage.JSON));
    }

    /**
     * Logs a request payload object (map, list, string JSON, etc.) the same way as {@link ExtentReportRequestFilter}.
     */
    public static void requestPayloadBlock(String title, Object payload) {
        ExtentTest test = ExtentManager.currentTest();
        if (test == null) {
            return;
        }
        jsonBlock(title, formatPayloadForReport(payload));
    }

    /**
     * Formats a body for Extent JSON code blocks — shared by the request filter and manual test logging.
     */
    static String formatPayloadForReport(Object body) {
        if (body == null) {
            return "";
        }
        if (body instanceof String) {
            return prettyPrintJsonIfPossible((String) body);
        }
        if (body instanceof byte[]) {
            return prettyPrintJsonIfPossible(new String((byte[]) body, StandardCharsets.UTF_8));
        }
        if (body instanceof File) {
            return "(file body: " + ((File) body).getAbsolutePath() + ")";
        }
        if (body instanceof Map || body instanceof List) {
            return PRETTY_GSON.toJson(body);
        }
        try {
            return PRETTY_GSON.toJson(body);
        } catch (Exception e) {
            return String.valueOf(body);
        }
    }

    private static String prettyPrintJsonIfPossible(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.isBlank()) {
            return raw;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return raw;
        }
        try {
            JsonElement el = JsonParser.parseString(raw);
            return PRETTY_GSON.toJson(el);
        } catch (Exception e) {
            return raw;
        }
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
