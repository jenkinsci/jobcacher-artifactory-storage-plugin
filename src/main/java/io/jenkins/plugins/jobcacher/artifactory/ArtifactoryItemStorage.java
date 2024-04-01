package io.jenkins.plugins.jobcacher.artifactory;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import jenkins.plugins.itemstorage.GlobalItemStorage;
import jenkins.plugins.itemstorage.ItemStorage;
import jenkins.plugins.itemstorage.ItemStorageDescriptor;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class ArtifactoryItemStorage extends ItemStorage<ArtifactoryItemPath> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SERVER_URL_REGEXP =
            "^(http://|https://)[a-z0-9][a-z0-9-.]{0,}(?::[0-9]{1,5})?(/[0-9a-zA-Z_]*)*$";
    private static final Pattern endPointPattern = Pattern.compile(SERVER_URL_REGEXP, Pattern.CASE_INSENSITIVE);

    public static final Logger LOGGER = LoggerFactory.getLogger(ArtifactoryItemStorage.class);

    private String storageCredentialId;
    private String serverUrl;
    private String repository;
    private String prefix;

    @DataBoundConstructor
    public ArtifactoryItemStorage() {}

    @DataBoundSetter
    public void setStorageCredentialId(String storageCredentialId) {
        this.storageCredentialId = storageCredentialId;
    }

    @DataBoundSetter
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @DataBoundSetter
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @DataBoundSetter
    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getStorageCredentialId() {
        return storageCredentialId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getRepository() {
        return repository;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public ArtifactoryItemPath getObjectPath(Item item, String path) {
        return new ArtifactoryItemPath(
                createArtifactoryClient(), String.format("%s/%s", prefix, item.getFullName()), path);
    }

    @Override
    public ArtifactoryItemPath getObjectPathForBranch(Item item, String path, String branch) {
        String branchPath = new File(item.getFullName()).getParent() + "/" + branch;
        return new ArtifactoryItemPath(createArtifactoryClient(), String.format("%s/%s", prefix, branchPath), path);
    }

    public void deletePath(String path) {
        try (ArtifactoryClient client = createArtifactoryClient()) {
            client.deleteArtifact(String.format("%s/%s", prefix, path));
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to delete path at %s", path), e);
        }
    }

    public void movePath(String fromPath, String toPath) {
        try (ArtifactoryClient client = createArtifactoryClient()) {
            client.move(String.format("%s/%s", prefix, fromPath), String.format("%s/%s", prefix, toPath));
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to move path from %s to %s", fromPath, toPath), e);
        }
    }

    private ArtifactoryClient createArtifactoryClient() {
        LOGGER.info(serverUrl);
        LOGGER.info(repository);
        return new ArtifactoryClient(serverUrl, repository, Utils.getCredentials(storageCredentialId));
    }

    public static ArtifactoryItemStorage get() {
        return ExtensionList.lookupSingleton(ArtifactoryItemStorage.class);
    }

    @Extension
    public static final class DescriptorImpl extends ItemStorageDescriptor<ArtifactoryItemPath> {

        @Override
        public String getDisplayName() {
            return "Artifactory";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return super.configure(req, json);
        }

        @SuppressWarnings("lgtm[jenkins/csrf]")
        public ListBoxModel doFillStorageCredentialIdItems(@AncestorInPath Item item) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(get().getStorageCredentialId());
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(get().getStorageCredentialId());
                }
            }
            return result.includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM2,
                            item,
                            StandardUsernameCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.instanceOf(StandardUsernameCredentials.class))
                    .includeCurrentValue(get().getStorageCredentialId());
        }

        @SuppressWarnings("lgtm[jenkins/csrf]")
        public FormValidation doCheckRepository(@QueryParameter String repository) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(repository)) {
                ret = FormValidation.error("Repository cannot be blank");
            }
            return ret;
        }

        @SuppressWarnings("lgtm[jenkins/csrf]")
        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(serverUrl)) {
                ret = FormValidation.error("Server url cannot be blank");
            } else if (!endPointPattern.matcher(serverUrl).matches()) {
                ret = FormValidation.error("Server url doesn't seem valid. Should start with http:// or https://");
            }
            return ret;
        }

        @RequirePOST
        public FormValidation doValidateArtifactoryConfig(
                @QueryParameter("serverUrl") final String serverUrl,
                @QueryParameter("storageCredentialId") final String storageCredentialId,
                @QueryParameter("repository") final String repository,
                @QueryParameter("prefix") final String prefix) {

            Jenkins.get().checkPermission(Jenkins.ADMINISTER);

            if (StringUtils.isBlank(serverUrl)
                    || StringUtils.isBlank(storageCredentialId)
                    || StringUtils.isBlank(repository)) {
                return FormValidation.error("Fields required");
            }

            try {
                Path tmpFile = Files.createTempFile("tmp-", "jenkins-artifactory-plugin-test");

                // Upload and delete artifact to check connectivity
                try (ArtifactoryClient client =
                        new ArtifactoryClient(serverUrl, repository, Utils.getCredentials(storageCredentialId))) {
                    client.uploadArtifact(tmpFile, String.format("%s/%s", prefix, tmpFile.getFileName()));
                    client.deleteArtifact(String.format("%s/%s", prefix, tmpFile.getFileName()));
                }

                LOGGER.debug("Artifactory configuration validated");

            } catch (Exception e) {
                LOGGER.error("Unable to connect to Artifactory. Please check the server url and credentials", e);
                return FormValidation.error(
                        "Unable to connect to Artifactory. Please check the server url and credentials : "
                                + e.getMessage());
            }

            return FormValidation.ok("Success");
        }
    }

    @Extension
    public static final class ArtifactoryItemListener extends ItemListener {

        @Override
        public void onDeleted(Item item) {
            ArtifactoryItemStorage artifactoryItemStorage = lookupArtifactoryStorage();
            if (artifactoryItemStorage == null) {
                return;
            }
            artifactoryItemStorage.deletePath(item.getFullName());
        }

        @Override
        public void onLocationChanged(Item item, String oldFullName, String newFullName) {
            ArtifactoryItemStorage artifactoryItemStorage = lookupArtifactoryStorage();
            if (artifactoryItemStorage == null) {
                return;
            }
            artifactoryItemStorage.movePath(oldFullName, newFullName);
        }

        private ArtifactoryItemStorage lookupArtifactoryStorage() {
            ItemStorage<?> storage = GlobalItemStorage.get().getStorage();
            if (storage instanceof ArtifactoryItemStorage) {
                return (ArtifactoryItemStorage) storage;
            } else {
                return null;
            }
        }
    }
}
