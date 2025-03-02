package io.jenkins.plugins.jobcacher.artifactory;

import hudson.AbortException;
import hudson.FilePath;
import hudson.model.Job;
import hudson.remoting.VirtualChannel;
import hudson.util.IOUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import jenkins.MasterToSlaveFileCallable;
import jenkins.plugins.itemstorage.ObjectPath;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class ArtifactoryItemPath extends ObjectPath {

    private final ArtifactoryClient client;
    private final String fullName;
    private final String path;

    public ArtifactoryItemPath(final ArtifactoryClient client, final String fullName, final String path) {
        this.client = client;
        this.fullName = fullName;
        this.path = path;
    }

    public ArtifactoryClient getClient() {
        return client;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPath() {
        return path;
    }

    @Override
    public ArtifactoryItemPath child(String childPath) throws IOException, InterruptedException {
        return new ArtifactoryItemPath(client, fullName, String.format("%s/%s", path, childPath));
    }

    @Override
    public void copyTo(FilePath target) throws IOException, InterruptedException {
        target.act(new DownloadFromArtifactoryStorage(client.getConfig(), String.format("%s/%s", fullName, path)));
    }

    @Override
    public void copyFrom(FilePath source) throws IOException, InterruptedException {
        source.act(new UploadToArtifactoryStorage(client.getConfig(), String.format("%s/%s", fullName, path)));
    }

    @Override
    public boolean exists() throws IOException, InterruptedException {
        return client.isFile(String.format("%s/%s", fullName, path));
    }

    @Override
    public void deleteRecursive() throws IOException, InterruptedException {
        client.deleteArtifact(String.format("%s/%s", fullName, path));
    }

    @Override
    public HttpResponse browse(StaplerRequest2 request, StaplerResponse2 response, Job<?, ?> job, String name)
            throws IOException {
        return null;
    }

    /**
     * Master to slave callable that upload a cache to Artifactory storage.
     */
    private static class UploadToArtifactoryStorage extends MasterToSlaveFileCallable<Void> {

        private final ArtifactoryClient.ArtifactoryConfig config;
        private final String path;

        public UploadToArtifactoryStorage(ArtifactoryClient.ArtifactoryConfig config, String path) {
            this.config = config;
            this.path = path;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            try (ArtifactoryClient client = new ArtifactoryClient(this.config)) {
                client.uploadArtifact(f.toPath(), path);
            } catch (Exception e) {
                throw new AbortException("Unable to upload cache to Artifactory. Details: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Master to slave callable that upload a cache to Artifactory storage.
     */
    private static class DownloadFromArtifactoryStorage extends MasterToSlaveFileCallable<Void> {

        private final ArtifactoryClient.ArtifactoryConfig config;
        private final String path;

        public DownloadFromArtifactoryStorage(ArtifactoryClient.ArtifactoryConfig config, String path) {
            this.config = config;
            this.path = path;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            try (ArtifactoryClient client = new ArtifactoryClient(this.config)) {
                try (InputStream is = client.downloadArtifact(path)) {
                    IOUtils.copy(is, f);
                }
            } catch (Exception e) {
                throw new AbortException("Unable to upload cache to Artifactory. Details: " + e.getMessage());
            }
            return null;
        }
    }
}
