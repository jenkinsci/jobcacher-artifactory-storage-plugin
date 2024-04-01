package io.jenkins.plugins.jobcacher.artifactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import jenkins.plugins.itemstorage.GlobalItemStorage;
import jenkins.plugins.itemstorage.ItemStorage;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class DataMigrationTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    @LocalData
    public void shouldMigrateArtifactoryData() {
        ItemStorage<?> storage = GlobalItemStorage.get().getStorage();
        assertThat(storage, is(notNullValue()));
        ArtifactoryItemStorage artifactoryItemStorage = (ArtifactoryItemStorage) storage;
        assertThat(artifactoryItemStorage.getPrefix(), is("the-prefix/"));
        assertThat(artifactoryItemStorage.getRepository(), is("the-repo"));
        assertThat(artifactoryItemStorage.getServerUrl(), is("http://localhost:9000"));
        assertThat(artifactoryItemStorage.getStorageCredentialId(), is("artifactory"));
    }
}
