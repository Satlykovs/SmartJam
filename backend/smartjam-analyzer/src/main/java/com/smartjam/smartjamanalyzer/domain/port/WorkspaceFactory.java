package com.smartjam.smartjamanalyzer.domain.port;

/**
 * Factory port for creating isolated {@link Workspace} instances. This abstraction allows business logic to acquire
 * temporary resources without depending on specific implementations.
 */
public interface WorkspaceFactory {

    /**
     * Creates a new, isolated workspace.
     *
     * @return A new {@link Workspace} instance.
     */
    Workspace create();
}
