package io.jenkins.plugins.jobcacher.artifactory;

import static org.mockito.Mockito.*;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WireMockTest(httpPort = 7000)
public class ArtifactoryClientTest {

    private UsernamePasswordCredentials credentials;
    private ArtifactoryClient client;

    public void configure(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        // Add credentials to the store
        UsernamePasswordCredentialsImpl credentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "the-credentials-id", "sample", "sample", "sample");
        CredentialsProvider.lookupStores(jenkinsRule.getInstance())
                .iterator()
                .next()
                .addCredentials(Domain.global(), credentials);

        // Configure stubs
        wmRuntimeInfo
                .getWireMock()
                .register(WireMock.delete(WireMock.urlEqualTo("/my-generic-repo/job"))
                        .willReturn(WireMock.ok()));
        wmRuntimeInfo
                .getWireMock()
                .register(WireMock.put(WireMock.urlEqualTo("/my-generic-repo/target-path"))
                        .willReturn(WireMock.okJson("{}")));

        // Tested client
        client = new ArtifactoryClient("http://localhost:7000", "my-generic-repo", credentials);
    }

    @Test
    public void testUpload(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        configure(jenkinsRule, wmRuntimeInfo);

        // Create temp file
        File file = File.createTempFile("test", ".txt");

        client.uploadArtifact(file.toPath(), "target-path");
    }
}
