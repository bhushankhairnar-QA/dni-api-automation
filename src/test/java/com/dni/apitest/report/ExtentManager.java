package com.dni.apitest.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.aventstack.extentreports.reporter.configuration.ViewName;

/**
 * Holds the shared {@link ExtentReports} instance and a {@link ThreadLocal} {@link ExtentTest}
 * for the currently running TestNG method (safe if parallel execution is enabled later).
 */
public final class ExtentManager {

    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();

    private ExtentManager() {}

    public static synchronized void initSuite(String suiteName) {
        if (extentReports != null) {
            return;
        }
        String reportPath = "target/extent-report/index.html";
        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        spark.config().setTheme(Theme.DARK);
        spark.config().setEncoding("utf-8");
        spark.config().setDocumentTitle("DNI API Automation Report");
        spark.config().setReportName(suiteName != null && !suiteName.isBlank() ? suiteName : "API test run");
        spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
        spark.config().thumbnailForBase64(false);
        spark.viewConfigurer().viewOrder().as(new ViewName[] {
                ViewName.DASHBOARD,
                ViewName.TEST,
                ViewName.EXCEPTION,
                ViewName.LOG,
        }).apply();

        extentReports = new ExtentReports();
        extentReports.attachReporter(spark);
        extentReports.setSystemInfo("OS", System.getProperty("os.name"));
        extentReports.setSystemInfo("OS arch", System.getProperty("os.arch"));
        extentReports.setSystemInfo("Java", System.getProperty("java.version"));
        extentReports.setSystemInfo("User", System.getProperty("user.name"));
    }

    public static ExtentReports reporter() {
        return extentReports;
    }

    public static ExtentTest createTest(String name, String description) {
        if (extentReports == null) {
            initSuite("API test run");
        }
        if (description != null && !description.isBlank()) {
            return extentReports.createTest(name, description);
        }
        return extentReports.createTest(name);
    }

    public static ExtentTest currentTest() {
        return CURRENT_TEST.get();
    }

    public static void setCurrentTest(ExtentTest test) {
        CURRENT_TEST.set(test);
    }

    public static void clearCurrentTest() {
        CURRENT_TEST.remove();
    }

    public static synchronized void flush() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
}
