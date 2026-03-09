package com.smartjam.smartjamanalyzer.domain.port;

import java.io.IOException;
import java.nio.file.Path;

/** Port for a workspace that provides isolated temporary storage. */
public interface Workspace extends AutoCloseable {

    /**
     * Allocates a new temporary resource.
     *
     * @return Path to the allocated resource.
     */
    Path allocate(String prefix, String suffix) throws IOException;

    @Override
    void close();
}
