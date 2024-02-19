package nl.hsac.fitnesse.fixture.util.selenium.driverfactory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.util.HashMap;
import java.util.Map;

/**
 * Gets a selenium driver installed by project (in wiki/webdrivers).
 */
public class ProjectDriverFactoryFactory {
    public LocalDriverFactory create(String browser, Map<String, Object> profile) {
        String driverClass;
        String browserName = browser.toLowerCase();
        switch (browserName) {
            case "firefox": {
                Class<? extends WebDriver> driver = FirefoxDriver.class;
                driverClass = driver.getName();
                break;
            }
            case "safari": {
                Class<? extends WebDriver> driver = SafariDriver.class;
                driverClass = driver.getName();
                break;
            }
            case "chrome mobile emulation":
                profile = new HashMap<>();
            case "chrome": {
                Class<? extends WebDriver> driver = ChromeDriver.class;
                driverClass = driver.getName();
                break;
            }
            case "microsoftedge":
            case "edge": {
                Class<? extends WebDriver> driver = EdgeDriver.class;
                driverClass = driver.getName();
                break;
            }
            case "internet explorer": {
                Class<? extends WebDriver> driver = InternetExplorerDriver.class;
                driverClass = driver.getName();
                break;
            }
            case "phantomjs": {
                Class<? extends WebDriver> driver = PhantomJSDriver.class;
                driverClass = driver.getName();
                break;
            }
            default:
                throw new IllegalArgumentException("No defaults known for: " + browser);
        }
        return new LocalDriverFactory(driverClass, profile);
    }
}
