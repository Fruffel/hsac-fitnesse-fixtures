package nl.hsac.fitnesse.fixture.util.selenium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipUtility {

    private static final Logger log = LoggerFactory.getLogger(UnzipUtility.class);

    public static void unzipFolder(Path sourceFolder, Path targetFolder) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(sourceFolder.toFile()))) {
            // list files in zip
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            String rootFolderName = null;

            while (zipEntry != null) {
                // Check for zip slip vulnerability attack
                Path newUnzipPathWithoutRoot;

                if (rootFolderName == null) {
                    // get the root folder name
                    rootFolderName = zipEntry.getName().split("/")[0];
                }

                String entryNameWithoutRoot = zipEntry.getName().replace(rootFolderName + "/", "");
                ZipEntry newZipEntry = new ZipEntry(entryNameWithoutRoot);
                newUnzipPathWithoutRoot = zipSlipVulnerabilityProtect(newZipEntry, targetFolder);

                boolean isDirectory = newZipEntry.getName().endsWith("/");
                //check for files or directory

                if (isDirectory && Files.notExists(newUnzipPathWithoutRoot)) {
                    boolean success = newUnzipPathWithoutRoot.toFile().mkdirs();
                    if (success) {
                        log.info("Successfully created directory: {}", newUnzipPathWithoutRoot);
                    } else {
                        log.warn("Failed to create directory: {}", newUnzipPathWithoutRoot);
                    }
                } else {
                    Path parentPath = newUnzipPathWithoutRoot.getParent();

                    if (parentPath != null && Files.notExists(parentPath)) {
                        boolean success = newUnzipPathWithoutRoot.getParent().toFile().mkdirs();
                        if (success) {
                            log.info("Successfully created parent directory: {}", parentPath);
                        } else {
                            log.warn("Failed to create parent directory: {}", parentPath);
                        }
                    }

                    // copy files using nio
                    Files.copy(zipInputStream, newUnzipPathWithoutRoot, StandardCopyOption.REPLACE_EXISTING);
                    File file = new File(newUnzipPathWithoutRoot.toString());

                    // Set permissions for others
                    if (!file.setExecutable(true)) {
                        log.warn("Failed to set execute permission on {}", file.getPath());
                    }

                    if (!file.setReadable(true)) {
                        log.warn("Failed to set read permission on {}", file.getPath());
                    }

                    if (!file.setWritable(true)) {
                        log.warn("Failed to set write permission on {}", file.getPath());
                    }
                }

                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
        }
    }

    // Check for zip slip attack
    private static Path zipSlipVulnerabilityProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
        Path dirResolved = targetDir.resolve(zipEntry.getName());

        //normalize the path on target directory or else throw exception
        Path normalizePath = dirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Invalid zip: " + zipEntry.getName());
        }

        return normalizePath;
    }
}
