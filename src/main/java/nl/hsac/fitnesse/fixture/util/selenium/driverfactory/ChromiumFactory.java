package nl.hsac.fitnesse.fixture.util.selenium.driverfactory;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChromiumFactory {
    private final File version = new File(getLoc("ChromiumVersion.txt"));
    private final File chrome7z = new File(getLoc("chromiumTemp/chrome.7z"));
    private final File chromiumTemp = new File(getLoc("chromiumTemp"));
    private final String chrome7zTemp = getLoc("chromiumTemp/chrome.7z");
    private final File chromiumTempBin = new File(getLoc("chromiumTemp/Chrome-bin"));
    private final File chromium = new File(getLoc("chromium"));

    public TagAndUrl downloadChromium() {
        TagAndUrl tagAndUrl = getTagAndUrl();

        try {
            if (Files.exists(chromium.toPath()) && Files.exists(version.toPath())) {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(version));
                String v = bufferedReader.readLine();

                Matcher matcher = getMatcherFromTag(v);
                Matcher matcher2 = getMatcherFromTag(tagAndUrl.getTag());

                if (matcher.find() && matcher2.find()) {
                    int fileG1 = Integer.parseInt(matcher.group(1));
                    int fileG2 = Integer.parseInt(matcher.group(2));
                    int fileG3 = Integer.parseInt(matcher.group(3));
                    int fileG4 = Integer.parseInt(matcher.group(4));
                    int tagG1 = Integer.parseInt(matcher2.group(1));
                    int tagG2 = Integer.parseInt(matcher2.group(2));
                    int tagG3 = Integer.parseInt(matcher2.group(3));
                    int tagG4 = Integer.parseInt(matcher2.group(4));

                    if (fileG1 < tagG1 || fileG2 < tagG2 || fileG3 < tagG3 || fileG4 < tagG4) {
                        createChromium(tagAndUrl.getUrl(), tagAndUrl.getTag());
                    }
                }
            } else {
                createChromium(tagAndUrl.getUrl(), tagAndUrl.getTag());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return tagAndUrl;
    }

    public Matcher getMatcherFromTag(String tag) {
        String regex = "(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(tag);
    }

    private TagAndUrl getTagAndUrl() {
        TagAndUrl tagAndUrl = new TagAndUrl();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://api.github.com/repos/Hibbiki/chromium-win64/releases/latest");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    JSONObject json = new JSONObject(result);
                    tagAndUrl.setTag(json.getString("tag_name"));
                    JSONArray assets = json.getJSONArray("assets");
                    JSONObject noSync = assets.getJSONObject(0);
                    tagAndUrl.setUrl(new URL(noSync.getString("browser_download_url")));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return tagAndUrl;
    }

    private void decompress(String in, File destination) throws IOException {
        SevenZFile sevenZFile = new SevenZFile(new File(in));
        SevenZArchiveEntry entry;
        while ((entry = sevenZFile.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            File curfile = new File(destination, entry.getName());
            File parent = curfile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            FileOutputStream out = new FileOutputStream(curfile);
            byte[] content = new byte[(int) entry.getSize()];
            sevenZFile.read(content, 0, content.length);
            out.write(content);
            out.close();
        }
    }

    private void createVersionFile(String tag) {
        try (FileWriter fileWriter = new FileWriter(version.toString())) {
            fileWriter.write(tag);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private void createChromium(URL downloadUrl, String tag) throws IOException {
        createVersionFile(tag);
        FileUtils.copyURLToFile(downloadUrl, chrome7z);
        decompress(chrome7zTemp, chromiumTemp);

        if (Files.exists(chromium.toPath())) {
            FileUtils.forceDelete(chromium);
        }

        if (chromiumTempBin.renameTo(chromium)) {
            FileUtils.deleteQuietly(chromiumTemp);
        }
    }

    public String getProperties(String value) throws IOException {
        Path str = Paths.get(getLoc("chromium.properties"));

        try (FileInputStream fileInputStream = new FileInputStream(str.toFile())) {
            Properties prop = new Properties();
            prop.load(fileInputStream);

            return prop.getProperty(value);
        }
    }

    public String getLoc(String file) {
        Path str = Paths.get(System.getProperty("user.dir"));
        if (!System.getProperty("user.dir").contains("wiki")) {
            str = Paths.get(System.getProperty("user.dir") + "/wiki");
        }

        return str + "/" + file;
    }

    static class TagAndUrl {
        private URL url;
        private String tag;

        public URL getUrl() {
            return url;
        }

        public void setUrl(URL url) {
            this.url = url;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }
    }

}
