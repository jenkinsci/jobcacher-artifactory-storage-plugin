package io.jenkins.plugins.jobcacher.artifactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hudson.FilePath;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        doReturn(false).when(client).isFile(anyString());
        doReturn(true).when(client).isFile("fullName/path");
        assertTrue(itemPath.exists());
        verify(client, times(1)).isFile(anyString());
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testCopyTo() throws IOException, InterruptedException {
        FilePath filePath = mock(FilePath.class);
        InputStream stream = mock(InputStream.class);
        doReturn(stream).when(client).downloadArtifact("fullName/path");

        // Test
        itemPath.copyTo(filePath);

        // Assert
        verify(client, times(1)).downloadArtifact("fullName/path");
        verifyNoMoreInteractions(client);
        verify(filePath, times(1)).copyFrom(stream);
        verifyNoMoreInteractions(filePath);
    }

    @Test
    public void testCopyFrom() throws IOException, InterruptedException, URISyntaxException {
        FilePath filePath = mock(FilePath.class);
        URI uri = new URI("file:///path");
        doReturn(uri).when(filePath).toURI();

        // Test
        itemPath.copyFrom(filePath);

        // Assert
        verify(client, times(1)).uploadArtifact(Path.of("/path"), "fullName/path");
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testDelete() throws IOException, InterruptedException {
        itemPath.deleteRecursive();
        verify(client, times(1)).deleteArtifact("fullName/path");
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testGetFullName() {
        assertEquals("fullName", itemPath.getFullName());
    }
}
