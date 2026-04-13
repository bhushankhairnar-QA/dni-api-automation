package com.dni.apitest.report;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.MarkupHelper;

/**
 * Small helpers so tests can log numbered steps and JSON to the active Extent test.
 */
public final class ReportSteps {

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

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
