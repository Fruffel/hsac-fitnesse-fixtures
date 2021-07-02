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
import java.util.Comparator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ChromiumFactory {
    private final File version = new File("./ChromiumVersion.txt");
    private final File chrome7z = new File("./chromiumTemp/chrome.7z");
    private final File chromiumTemp = new File("./chromiumTemp");
    private final String chrome7zTemp = "./chromiumTemp/chrome.7z";
    private final File chromiumTempBin = new File("./chromiumTemp/Chrome-bin");
    private final File chromium = new File("./chromium");

    public void downloadChromium() {
        TagAndUrl tagAndUrl = getTagAndUrl();

        try {
            if (Files.exists(chromium.toPath()) && Files.exists(version.toPath())) {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(version));
                String v = bufferedReader.readLine();

                String regex = "([0-9]+)\\.([0-9]+)\\.([0-9]+)\\.([0-9]+)";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(v);
                Matcher matcher2 = pattern.matcher(tagAndUrl.getTag());

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

    private boolean walkDeleteFiles(File fileToBeDeleted) throws IOException {
        Path pathToBeDeleted = fileToBeDeleted.toPath();

        try (Stream<Path> walk = Files.walk(pathToBeDeleted)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        return Files.exists(pathToBeDeleted);
    }

    private void createVersionFile(String location, String tag) {
        try (FileWriter fileWriter = new FileWriter(version.toString())) {
            fileWriter.write(tag);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private void createChromium(URL downloadUrl, String tag) throws IOException {
        createVersionFile(version.toString(), tag);
        FileUtils.copyURLToFile(downloadUrl, chrome7z);
        decompress(chrome7zTemp, chromiumTemp);
        if (Files.exists(chromium.toPath())) {
            walkDeleteFiles(chromium);
        }
        FileUtils.moveDirectory(chromiumTempBin, chromium);
        walkDeleteFiles(chromiumTemp);
    }

    public String getProperties(String value) throws IOException {
        Path str = Paths.get(System.getProperty("user.dir") + "/chromium.properties");

        if (!System.getProperty("user.dir").contains("wiki")) {
            str = Paths.get(str + "/wiki");
        }

        try (FileInputStream fileInputStream = new FileInputStream(str.toFile())) {
            Properties prop = new Properties();
            prop.load(fileInputStream);

            return prop.getProperty(value);
        }
    }

    private static class TagAndUrl {
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
