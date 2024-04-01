package io.jenkins.plugins.jobcacher.artifactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import jenkins.plugins.itemstorage.GlobalItemStorage;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
public class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void shouldSupportConfigurationAsCode(JenkinsConfiguredWithCodeRule jenkinsRule) throws Exception {
        ArtifactoryItemStorage itemStorage =
                (ArtifactoryItemStorage) GlobalItemStorage.get().getStorage();
        assertThat(itemStorage.getStorageCredentialId(), is("the-credentials-id"));
        assertThat(itemStorage.getServerUrl(), is("http://localhost:7000"));
        assertThat(itemStorage.getRepository(), is("my-generic-repo"));
        assertThat(itemStorage.getPrefix(), is("jenkins/"));
    }
}
