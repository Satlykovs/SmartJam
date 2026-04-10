package com.smartjam.smartjamanalyzer.domain.port;

import java.nio.file.Path;

/** Port for interacting with remote audio storage. */
public interface AudioStorage {
    /**
     * Downloads an audio file from the specified bucket to a local temporary location.
     *
     * @param bucketName The name of the storage bucket.
     * @param fileKey The object key in the bucket.
     * @param workspace The workspace that registers the downloaded file for cleanup.
     * @return A {@link Path} to the downloaded file.
     * @throws RuntimeException if the download fails.
     */
    Path downloadAudioFile(String bucketName, String fileKey, Workspace workspace);
}
