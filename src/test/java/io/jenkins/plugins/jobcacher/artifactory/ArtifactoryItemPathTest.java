package io.jenkins.plugins.jobcacher.artifactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArtifactoryItemPathTest {

    private ArtifactoryClient client;
    private ArtifactoryItemPath itemPath;

    @BeforeEach
    void setUp() {
        client = mock(ArtifactoryClient.class);
        itemPath = new ArtifactoryItemPath(client, "fullName", "path");
    }

    @Test
    void testChild() throws IOException, InterruptedException {
        ArtifactoryItemPath child = itemPath.child("childPath");
        assertNotNull(child);
        assertEquals("fullName", child.getFullName());
        assertEquals("path/childPath", child.getPath());
    }

    @Test
    void testExists() throws IOException, InterruptedException {
        doReturn(false).when(client).isFile(anyString());
        doReturn(true).when(client).isFile("fullName/path");
        assertTrue(itemPath.exists());
        verify(client, times(1)).isFile(anyString());
        verifyNoMoreInteractions(client);
    }

    @Test
    void testDelete() throws IOException, InterruptedException {
        itemPath.deleteRecursive();
        verify(client, times(1)).deleteArtifact("fullName/path");
        verifyNoMoreInteractions(client);
    }

    @Test
    void testGetFullName() {
        assertEquals("fullName", itemPath.getFullName());
    }
}
