package io.jenkins.plugins.jobcacher.artifactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class ArtifactoryItemPathTest {

    private ArtifactoryClient client;
    private ArtifactoryItemPath itemPath;

    @BeforeEach
    public void setUp() {
        client = mock(ArtifactoryClient.class);
        itemPath = new ArtifactoryItemPath(client, "fullName", "path");
    }

    @Test
    public void testChild() throws IOException, InterruptedException {
        ArtifactoryItemPath child = itemPath.child("childPath");
        assertNotNull(child);
        assertEquals("fullName", child.getFullName());
        assertEquals("path/childPath", child.getPath());
    }

    @Test
    public void testExists() throws IOException, InterruptedException {
        when(client.isFile("fullName/path")).thenReturn(true);
        assertTrue(itemPath.exists());
    }

}
