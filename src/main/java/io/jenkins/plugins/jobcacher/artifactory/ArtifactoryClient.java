package io.jenkins.plugins.jobcacher.artifactory;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.jfrog.artifactory.client.*;
import org.jfrog.artifactory.client.model.File;
import org.jfrog.filespecs.FileSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactoryClient implements AutoCloseable {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArtifactoryClient.class);

    private final ArtifactoryConfig config;
    private final Artifactory artifactory;

    public ArtifactoryClient(
            @NonNull String serverUrl, @NonNull String repository, @NonNull UsernamePasswordCredentials credentials) {
        this.config = new ArtifactoryConfig(serverUrl, repository, credentials);
        this.artifactory = buildArtifactory();
    }

    /**
     * Upload an artifact to the repository
     * @param file the file to upload
     * @param targetPath the path to upload the file to
     * @throws IOException if the file cannot be uploaded
     */
    public void uploadArtifact(Path file, String targetPath) throws IOException {
        UploadableArtifact artifact =
                artifactory.repository(this.config.repository).upload(urlEncodeParts(targetPath), file.toFile());
        artifact.withSize(Files.size(file));
        artifact.withListener(
                (bytesRead, totalBytes) -> LOGGER.trace(String.format("Uploaded %d/%d", bytesRead, totalBytes)));
        artifact.doUpload();
        LOGGER.trace(String.format("Uploaded %s to %s", file, targetPath));
    }

    /**
     * Delete an artifact or path from the repository
     * @param targetPath the path of the artifact to delete
     */
    public void deleteArtifact(String targetPath) {
        artifactory.repository(this.config.repository).delete(urlEncodeParts(targetPath));
    }

    /**
     * Move an artifact from one path to another. Require Artifactory PRO
     * @param sourcePath the source path
     * @param targetPath the target path
     */
    public void move(String sourcePath, String targetPath) {
        ItemHandle sourceItem = artifactory.repository(this.config.repository).folder(urlEncodeParts(sourcePath));
        sourceItem.move(this.config.repository, urlEncodeParts(targetPath));
    }

    /**
     * Copy an artifact from one path to another. Require Artifactory PRO
     * @param sourcePath the source path
     * @param targetPath the target path
     */
    public void copy(String sourcePath, String targetPath) {
        ItemHandle sourceItem = artifactory.repository(this.config.repository).folder(urlEncodeParts(sourcePath));
        sourceItem.copy(this.config.repository, targetPath);
    }

    /**
     * Download an artifact from the repository
     * @param targetPath the path of the artifact to download
     * @return a pair of the closable client and the input stream of the artifact
     * @throws IOException if the artifact cannot be downloaded
     */
    public InputStream downloadArtifact(String targetPath) throws IOException {
        DownloadableArtifact artifact =
                artifactory.repository(this.config.repository).download(urlEncodeParts(targetPath));
        return artifact.doDownload();
    }

    /**
     * Check if a path is a folder
     * @param targetPath the path to check
     * @return true if the path is a folder, false otherwise
     * @throws IOException if the path cannot be checked
     */
    public boolean isFolder(String targetPath) throws IOException {
        try {
            return artifactory.repository(this.config.repository).isFolder(urlEncodeParts(targetPath));
        } catch (Exception e) {
            LOGGER.debug(String.format("Failed to check if %s is a folder", targetPath));
            return false;
        }
    }

    /**
     * List the files in a folder
     * @param targetPath the path to list
     * @return the list of files in the folder
     * @throws IOException if the files cannot be listed
     */
    public List<String> list(String targetPath) throws IOException {
        if (!isFolder(targetPath)) {
            LOGGER.debug(String.format("Target path %s is not a folder. Cannot list files", targetPath));
            return List.of();
        }
        FileSpec fileSpec = FileSpec.fromString(
                String.format("{\"files\": [{\"pattern\": \"%s/%s*\"}]}", this.config.repository, targetPath));
        return artifactory.searches().artifactsByFileSpec(fileSpec).stream()
                .map((item -> String.format("%s/%s", item.getPath(), item.getName())))
                .collect(Collectors.toList());
    }

    /**
     * Check if a path is a file
     * @param targetPath the path to check
     * @return true if the path is a file, false otherwise
     * @throws IOException if the path cannot be checked
     */
    public boolean isFile(String targetPath) throws IOException {
        if (isFolder(targetPath)) {
            return false;
        }
        try {
            File file = artifactory
                    .repository(this.config.repository)
                    .file(urlEncodeParts(targetPath))
                    .info();
            return !file.isFolder();
        } catch (Exception e) {
            LOGGER.debug(String.format("Failed to check if %s is a file", targetPath));
            return false;
        }
    }

    /**
     * Get the last updated time of a path
     * @param targetPath the path to check
     * @return the last updated time of the path
     * @throws IOException if the last updated time cannot be checked
     */
    public long lastUpdated(String targetPath) throws IOException {
        LOGGER.trace(String.format("Getting last updated time for %s", targetPath));
        return artifactory
                .repository(this.config.repository)
                .file(targetPath)
                .info()
                .getLastModified()
                .getTime();
    }

    /**
     * Get the size of a path
     * @param targetPath the path to check
     * @return the size of the path
     * @throws IOException if the size cannot be checked
     */
    public long size(String targetPath) throws IOException {
        if (isFolder(targetPath)) {
            return 0;
        }
        LOGGER.trace(String.format("Getting size for %s", targetPath));
        File file = artifactory
                .repository(this.config.repository)
                .file(urlEncodeParts(targetPath))
                .info();
        return file.getSize();
    }

    /**
     * Return a new ArtifactoryConfig object for this client
     * @return the ArtifactoryConfig object
     */
    public ArtifactoryConfig buildArtifactoryConfig() {
        return new ArtifactoryConfig(this.config.serverUrl, this.config.repository, this.config.credentials);
    }

    /**
     * Build the Artifactory client
     * @return the Artifactory client
     */
    private Artifactory buildArtifactory() {
        return ArtifactoryClientBuilder.create()
                .setUrl(config.serverUrl)
                .setUsername(config.credentials.getUsername())
                .setPassword(config.credentials.getPassword().getPlainText())
                .addInterceptorLast((request, httpContext) -> {
                    LOGGER.debug(String.format("Sending Artifactory request to %s", request.getRequestLine()));
                })
                .build();
    }

    private String urlEncodeParts(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
                .replaceAll("%2F", "/")
                .replace("+", "%20");
    }

    @Override
    public void close() throws Exception {
        artifactory.close();
    }

    public static final class ArtifactoryConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String serverUrl;
        private final String repository;
        private final UsernamePasswordCredentials credentials;

        public ArtifactoryConfig(String serverUrl, String repository, UsernamePasswordCredentials credentials) {
            this.serverUrl = serverUrl;
            this.repository = repository;
            this.credentials = credentials;
        }

        public String getServerUrl() {
            return serverUrl;
        }

        public String getRepository() {
            return repository;
        }

        public UsernamePasswordCredentials getCredentials() {
            return credentials;
        }
    }
}
