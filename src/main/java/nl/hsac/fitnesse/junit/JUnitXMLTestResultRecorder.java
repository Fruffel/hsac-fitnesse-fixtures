package nl.hsac.fitnesse.junit;

import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class JUnitXMLTestResultRecorder {
    private final File reportsDir;

    public JUnitXMLTestResultRecorder(File reportsDir) {
        this.reportsDir = reportsDir;
        this.reportsDir.mkdirs();
    }

    void recordTestResult(String testName, String[] tags, int skipped, int failures, int errors, Throwable throwable, long executionTime) throws IOException {
        String resultXml = this.generateResultXml(testName, tags, skipped, failures, errors, throwable, (double) executionTime / 1000.0);
        this.writeResult(testName, resultXml);
    }

    private void writeResult(String testName, String resultXml) throws IOException {
        String finalPath = this.getXmlFileName(testName);
        Writer fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(finalPath), StandardCharsets.UTF_8));
        Throwable var5 = null;

        try {
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            fw.write(resultXml);
        } catch (Throwable var14) {
            var5 = var14;
            throw var14;
        } finally {
            if (fw != null) {
                if (var5 != null) {
                    try {
                        fw.close();
                    } catch (Throwable var13) {
                        var5.addSuppressed(var13);
                    }
                } else {
                    fw.close();
                }
            }

        }

    }

    private String getXmlFileName(String testName) {
        return (new File(this.reportsDir, "TEST-" + testName + ".xml")).getAbsolutePath();
    }

    private String generateResultXml(String testName, String[] tags, int skipped, int failures, int errors, Throwable throwable, double executionTime) {
        String failureXml = "";
        if (throwable != null) {
            failureXml = "<failure type=\"" + throwable.getClass().getName() + "\" message=\"" + this.getMessage(throwable) + "\"></failure>";
        }

        String xml = "<testsuite errors=\"" + errors + "\" skipped=\"" + skipped + "\" tests=\"1\" time=\"" + executionTime + "\" failures=\"" + failures + "\" name=\"" + testName + "\"><properties></properties><tags>" + String.join(",", tags) + "</tags><testcase classname=\"" + testName + "\" time=\"" + executionTime + "\" name=\"" + testName + "\">" + failureXml + "</testcase></testsuite>";
        return prettyPrint(xml);
    }

    private String getMessage(Throwable throwable) {
        String errorMessage = throwable.getMessage();
        return StringEscapeUtils.escapeXml10(errorMessage);
    }

    private String prettyPrint(String xmlString) {

        try {
            InputSource src = new InputSource(new StringReader(xmlString));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }
}