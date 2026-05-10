package com.smartjam.analyzer.infrastructure.utils;

import com.smartjam.analyzer.domain.port.Workspace;
import com.smartjam.analyzer.domain.port.WorkspaceFactory;
import org.springframework.stereotype.Component;

/** File-system based implementation of {@link WorkspaceFactory}. */
@Component
public class FsWorkspaceFactory implements WorkspaceFactory {

    /**
     * Creates a new {@link FsWorkspace}.
     *
     * @return a new {@link Workspace} instance backed by the local file system.
     */
    public Workspace create() {
        return new FsWorkspace();
    }
}
