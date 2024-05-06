package nl.hsac.fitnesse.fixture.util.selenium;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.hsac.fitnesse.fixture.util.selenium.UnzipUtility.unzipFolder;

public class DownloadChromeBrowser {

    private final String URL;
    private final String URL2;
    private final String ROOT = ".chrome";
    public final String BROWSER = ROOT + "/browser";
    private final String VERSIONS_FILE = ROOT + "/versions.json";
    private final String PLATFORM;

    public DownloadChromeBrowser() {
        URL = "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json";
        URL2 = "https://googlechromelabs.github.io/chrome-for-testing/known-good-versions-with-downloads.json";
        if (SystemUtils.IS_OS_LINUX) {
            PLATFORM = "linux64";
        } else {
            PLATFORM = "win64";
        }
    }

    public static void main(String[] args) throws IOException {
        DownloadChromeBrowser downloadChromeBrowser = new DownloadChromeBrowser();
        downloadChromeBrowser.download();
    }

    public void download() throws IOException {
        System.out.println("Fetching: chrome");

        String localVersion = getLocalChromeVersion();
        System.out.printf("Local version is: %s\n", localVersion);

        Path path = Paths.get(BROWSER);
        boolean chromeExists = Files.exists(path);
        boolean alwaysLatest = localVersion.startsWith("^");

        System.out.printf("Chrome exists: %s\n", chromeExists);

        if (alwaysLatest) {
            JSONObject jsonObject = getChromeForTestingJson(URL);
            downloadLatestChromeVersion(path, localVersion.replaceFirst("\\^", ""), jsonObject, chromeExists);
        } else {
            JSONObject jsonObject = getChromeForTestingJson(URL2);
            downloadSpecificChromeVersion(path, localVersion, jsonObject);
        }
    }

    private void downloadLatestChromeVersion(Path path, String localVersion, JSONObject jsonObject, boolean chromeExists) throws IOException {
        String latestVersion = getVersion(jsonObject);
        System.out.printf("Latest version is: %s\n", latestVersion);

        // check if a newer version is available
        if (isNewerChromeVersionAvailable(localVersion, latestVersion) && isChromeVersionAvailable(jsonObject, localVersion) && chromeExists) {
            // no newer version is available
            System.out.println("No newer version " + "chrome" + " is available on the API. Skipping download.\n");
            return;
        }

        // download the latest version of Chrome instead
        System.out.printf("Always downloading latest version. Version is: %s\n", latestVersion);
        deleteFilesInPath(path);

        // generate the download URL
        ChromeUrl downloadUrl = getDownloadUrl(jsonObject, latestVersion);
        chrome(downloadUrl, path);
        chromedriver(downloadUrl, path);

        if (!localVersion.equals(latestVersion)) {
            latestVersion = "^" + latestVersion;
            updateVersionsFile(latestVersion);
        }
    }

    private void downloadSpecificChromeVersion(Path path, String localVersion, JSONObject jsonObject) throws IOException {
        String version = getSpecificVersion(jsonObject, localVersion);

        if (version.isEmpty()) {
            System.out.printf("No version found for: %s\n", localVersion);
            return;
        }

        deleteFilesInPath(path);
        ChromeUrl downloadUrl = getDownloadUrlSpecific(jsonObject, version);
        chrome(downloadUrl, path);
        chromedriver(downloadUrl, path);
    }

    private void chrome(ChromeUrl chromeUrl, Path path) throws IOException {
        URL url = new URL(chromeUrl.getChrome());
        File zip = new File(path + ".zip");
        FileUtils.copyURLToFile(url, zip);
        unzipFolder(zip.toPath(), path);
        Files.delete(zip.toPath());
    }

    private void chromedriver(ChromeUrl chromeUrl, Path path) throws IOException {
        URL url = new URL(chromeUrl.getChromedriver());
        File zip = new File(Paths.get(path.toString().replace("browser", "driver")) + ".zip");
        FileUtils.copyURLToFile(url, zip);
        unzipFolder(zip.toPath(), path);
        Files.delete(zip.toPath());
    }

    private ChromeUrl getUrl(JSONObject downloadsObject) {
        String chromeUrl = getChromeUrl(downloadsObject);
        String chromedriverUrl = "";

        int index = chromeUrl.lastIndexOf("chrome");
        if (index != -1) {
            chromedriverUrl = chromeUrl.substring(0, index) + "chromedriver" + chromeUrl.substring(index + "chrome".length());
        }

        return new ChromeUrl(chromeUrl, chromedriverUrl);
    }

    private ChromeUrl getDownloadUrl(JSONObject jsonObject, String chromeVersion) {
        JSONObject channels = jsonObject.getJSONObject("channels");
        JSONObject stable = channels.getJSONObject("Stable");
        String version = getVersion(jsonObject);

        if (version.equals(chromeVersion)) {
            JSONObject downloads = stable.getJSONObject("downloads");
            return getUrl(downloads);
        }

        return new ChromeUrl("", "");
    }

    private ChromeUrl getDownloadUrlSpecific(JSONObject jsonObject, String chromeVersion) {
        JSONArray versions = jsonObject.getJSONArray("versions");
        for (int i = 0; i < versions.length(); i++) {
            JSONObject versionObject = versions.getJSONObject(i);
            String versionsString = versionObject.getString("version");

            if (versionsString.equals(chromeVersion)) {
                JSONObject downloads = versionObject.getJSONObject("downloads");
                return getUrl(downloads);
            }
        }

        return new ChromeUrl("", "");
    }


    private String getChromeUrl(JSONObject downloads) {
        JSONArray chrome = downloads.getJSONArray("chrome");
        for (int i = 0; i < chrome.length(); i++) {
            JSONObject object = chrome.getJSONObject(i);

            if (object.getString("platform").equals(PLATFORM)) {
                return object.getString("url");
            }
        }
        return "";
    }

    // this method updates the versions.json file with the latest version of the gem that was downloaded
    private void updateVersionsFile(String latestVersion) throws IOException {
        // check if the versions.json file exists
        Path path = Paths.get(VERSIONS_FILE);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }

        // read the contents of the versions.json file
        String jsonString = new String(Files.readAllBytes(path));

        // parse the JSON string, or initialize it with an empty JSON object if the string is empty
        JSONObject json = jsonString.isEmpty() ? new JSONObject() : new JSONObject(jsonString);

        // update the version number for Chrome
        json.put("Chrome", latestVersion);

        // write the updated JSON to the versions.json file
        try (Writer writer = new FileWriter(VERSIONS_FILE)) {
            writer.write(json.toString(4)); // use pretty printing with 4 spaces for indentation
        }
    }

    private boolean isNewerChromeVersionAvailable(String localVersion, String latestVersion) {
        return localVersion == null || localVersion.isEmpty() || compareVersions(localVersion, latestVersion) >= 0;
    }

    private boolean isChromeVersionAvailable(JSONObject jsonObject, String chromeVersion) {
        String version = getVersion(jsonObject);
        return version.equals(chromeVersion);
    }

    private JSONObject getChromeForTestingJson(String urlString) {
        try {
            // get the URL for the specified version of Chrome
            URL url = new URL(urlString);

            // open a connection to the URL and send a GET request
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // read the response from the API
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                return new JSONObject(reader.readLine());
            }
        } catch (IOException e) {
            System.out.printf("Error checking if Chrome version is available: %s\n", e.getMessage());
            return new JSONObject();
        }
    }

    private String getVersion(JSONObject jsonObject) {
        JSONObject channels = jsonObject.getJSONObject("channels");
        JSONObject stable = channels.getJSONObject("Stable");
        return stable.getString("version");
    }

    private String getSpecificVersion(JSONObject jsonObject, String version) {
        JSONArray versions = jsonObject.getJSONArray("versions");
        for (int i = 0; i < versions.length(); i++) {
            JSONObject versionObject = versions.getJSONObject(i);
            String versionsString = versionObject.getString("version");
            if (versionsString.equals(version)) {
                return versionsString;
            }
        }
        return "";
    }

    // this method gets the local version of a gem by reading the versions.json file
    private String getLocalChromeVersion() throws IOException {
        // parse the versions.json file
        File versionsJson = new File(VERSIONS_FILE);

        if (versionsJson.exists() && versionsJson.length() > 0) {
            JSONObject versionsObject = new JSONObject(new JSONTokener(new FileReader(VERSIONS_FILE)));
            // get the local version of chrome from the versions.json object
            return versionsObject.optString("Chrome", "");
        } else {
            Files.createDirectories(Paths.get(VERSIONS_FILE).getParent());
            JSONObject jsonObject2 = new JSONObject();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Chrome", "^0");

            try (FileWriter fileWriter = new FileWriter(VERSIONS_FILE)) {
                jsonObject.write(fileWriter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            JSONObject versionsObject = new JSONObject(new JSONTokener(new FileReader(VERSIONS_FILE)));
            // get the local version of chrome from the versions.json object
            return versionsObject.optString("Chrome", "^0");
        }
    }

    private int compareVersions(String version1, String version2) {
        return version1.compareTo(version2);
    }

    private void deleteFilesInPath(Path path) {
        try {
            FileUtils.deleteDirectory(path.toFile());
        } catch (IOException e) {
            System.out.printf("Failed to delete %s %s\n", path, e);
        }
    }

    private static class ChromeUrl {
        private final String chrome;
        private final String chromedriver;

        public ChromeUrl(String chrome, String chromedriver) {
            this.chrome = chrome;
            this.chromedriver = chromedriver;
        }

        public String getChrome() {
            return chrome;
        }

        public String getChromedriver() {
            return chromedriver;
        }
    }
}
