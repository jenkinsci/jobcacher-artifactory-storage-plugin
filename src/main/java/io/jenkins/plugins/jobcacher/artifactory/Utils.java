package io.jenkins.plugins.jobcacher.artifactory;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.security.ACL;
import java.nio.file.Path;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;

public final class Utils {

    public static StandardUsernamePasswordCredentials getCredentials(String credentialsId) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItemGroup(
                        StandardUsernamePasswordCredentials.class, Jenkins.get(), ACL.SYSTEM2, Collections.emptyList()),
                CredentialsMatchers.allOf(
                        CredentialsMatchers.withId(credentialsId),
                        CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)));
    }

    /**
     * Strip the trailing slash
     * @param key the key
     * @return the key without the trailing slash
     */
    public static String stripTrailingSlash(String key) {
        String localKey = key;
        if (key.endsWith("/")) {
            localKey = localKey.substring(0, localKey.length() - 1);
        }
        return localKey;
    }

    /**
     * Get the path with the prefix
     * @param prefix the prefix. Can be null or empty. Must end with a slash if not empty.
     * @param filePath the file path
     * @return the path with the prefix
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public static @NonNull String getPath(String prefix, @NonNull Path filePath) {
        String defaultPrefix =
                StringUtils.isBlank(prefix) ? "" : prefix.endsWith("/") ? prefix : String.format("%s/", prefix);
        return String.format("%s%s", defaultPrefix, filePath.getFileName().toString());
    }
}
