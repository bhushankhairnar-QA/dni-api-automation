package com.dni.apitest.report;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Creates one Spark HTML report per suite, one {@link ExtentTest} per TestNG test method,
 * and maps pass / fail / skip to Extent statuses.
 */
public final class ExtentTestNgListener implements ITestListener, ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        ExtentManager.initSuite(suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        ExtentManager.flush();
    }

    @Override
    public void onStart(ITestContext context) {
        // no-op: suite-level init is enough
    }

    @Override
    public void onFinish(ITestContext context) {
        // no-op
    }

    @Override
    public void onTestStart(ITestResult result) {
        String name = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        ExtentTest test = ExtentManager.createTest(name, description);
        for (String group : result.getMethod().getGroups()) {
            test.assignCategory(group);
        }
        ExtentManager.setCurrentTest(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentManager.currentTest();
        if (test != null) {
            test.log(Status.PASS, "Assertions passed");
        }
        ExtentManager.clearCurrentTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentManager.currentTest();
        if (test != null) {
            Throwable t = result.getThrowable();
            if (t != null) {
                if (isLikelyConnectivityFailure(t)) {
                    test.assignCategory("Connectivity");
                    test.log(
                            Status.WARNING,
                            "Failure looks like a network/DNS/connect issue (e.g. VPN, firewall, or bad "
                                    + "lytics.base.uri). Fix connectivity before treating this as an API assertion "
                                    + "failure.");
                }
                test.fail(t);
            } else {
                test.log(Status.FAIL, "Test failed (no throwable attached)");
            }
        }
        ExtentManager.clearCurrentTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentManager.currentTest();
        if (test != null) {
            Throwable t = result.getThrowable();
            if (t != null) {
                test.skip(t);
            } else {
                test.log(Status.SKIP, "Test skipped");
            }
        }
        ExtentManager.clearCurrentTest();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        onTestFailure(result);
    }

    private static boolean isLikelyConnectivityFailure(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof java.net.UnknownHostException
                    || c instanceof java.net.ConnectException
                    || c instanceof java.net.SocketTimeoutException
                    || c instanceof java.net.NoRouteToHostException) {
                return true;
            }
        }
        return false;
    }
}
