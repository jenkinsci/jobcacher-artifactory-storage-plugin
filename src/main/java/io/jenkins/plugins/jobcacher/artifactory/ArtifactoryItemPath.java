package io.jenkins.plugins.jobcacher.artifactory;

import hudson.FilePath;
import hudson.model.Job;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import jenkins.plugins.itemstorage.ObjectPath;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ArtifactoryItemPath extends ObjectPath {

    private final ArtifactoryClient client;
    private final String fullName;
    private final String path;

    public ArtifactoryItemPath(final ArtifactoryClient client, final String fullName, final String path) {
        this.client = client;
        this.fullName = fullName;
        this.path = path;
    }

    @Override
    public ArtifactoryItemPath child(String childPath) throws IOException, InterruptedException {
        return new ArtifactoryItemPath(client, fullName, String.format("%s/%s", path, childPath));
    }

    @Override
    public void copyTo(FilePath target) throws IOException, InterruptedException {
        try (InputStream stream = client.downloadArtifact(String.format("%s/%s", fullName, path))) {
            target.copyFrom(stream);
        }
    }

    @Override
    public void copyFrom(FilePath source) throws IOException, InterruptedException {
        client.uploadArtifact(Path.of(source.toURI()), String.format("%s/%s", fullName, path));
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
    public HttpResponse browse(StaplerRequest request, StaplerResponse response, Job<?, ?> job, String name)
            throws IOException {
        return null;
    }
}
