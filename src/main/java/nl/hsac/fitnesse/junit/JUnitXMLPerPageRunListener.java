package nl.hsac.fitnesse.junit;

import fitnesse.junit.FitNessePageAnnotation;
import fitnesse.util.TimeMeasurement;
import fitnesse.wiki.WikiPage;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.File;

public class JUnitXMLPerPageRunListener extends RunListener {
    private static final String OUTPUT_PATH = "target/failsafe-reports/";
    private final JUnitXMLTestResultRecorder testResultRecorder;
    private TimeMeasurement timeMeasurement;

    public JUnitXMLPerPageRunListener() {
        this.testResultRecorder = new JUnitXMLTestResultRecorder(new File("target/failsafe-reports/"));
    }

    public JUnitXMLPerPageRunListener(JUnitXMLTestResultRecorder jUnitXMLTestResultRecorder) {
        this.testResultRecorder = jUnitXMLTestResultRecorder;
    }

    public void testStarted(Description description) throws Exception {
        this.timeMeasurement = (new TimeMeasurement()).start();
        super.testStarted(description);
    }

    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
        if (!this.timeMeasurement.isStopped()) {
            this.testResultRecorder.recordTestResult(this.getTestName(description), this.getTags(description), 0, 0, 0, (Throwable) null, this.getExecutionTime());
        }

    }

    public void testFailure(Failure failure) throws Exception {
        super.testFailure(failure);
        String testName = this.getTestName(failure.getDescription());
        String[] tags = this.getTags(failure.getDescription());
        Throwable throwable = failure.getException();
        long executionTime = this.getExecutionTime();
        if (throwable instanceof AssertionError) {
            this.testResultRecorder.recordTestResult(testName, tags, 0, 1, 0, throwable, executionTime);
        } else {
            this.testResultRecorder.recordTestResult(testName, tags, 0, 0, 1, throwable, executionTime);
        }

    }

    public void testIgnored(Description description) throws Exception {
        super.testIgnored(description);
        if (!this.timeMeasurement.isStopped()) {
            this.testResultRecorder.recordTestResult(this.getTestName(description), getTags(description), 1, 0, 0, (Throwable) null, this.getExecutionTime());
        }

    }

    protected long getExecutionTime() {
        long executionTime = 0L;
        if (this.timeMeasurement != null) {
            executionTime = this.timeMeasurement.elapsed();
            if (!this.timeMeasurement.isStopped()) {
                this.timeMeasurement.stop();
            }
        }

        return executionTime;
    }

    protected String getOutputPath() {
        return "target/failsafe-reports/";
    }

    protected String getTestName(Description description) {
        return description.getMethodName();
    }

    protected String[] getTags(Description description) {
        FitNessePageAnnotation pageAnn = description.getAnnotation(FitNessePageAnnotation.class);
        String[] tags = new String[0];

        if (pageAnn != null) {
            WikiPage page = pageAnn.getWikiPage();
            if (page != null) {
                String tagInfo = page.getData().getProperties().get("Suites");
                if (null != tagInfo) {
                    tags = tagInfo.split(",");
                }
            }
        }
        return tags;
    }
}
