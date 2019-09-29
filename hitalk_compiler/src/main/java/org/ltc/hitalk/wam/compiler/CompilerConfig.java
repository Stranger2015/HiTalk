package org.ltc.hitalk.wam.compiler;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;

public
class CompilerConfig implements org.ltc.hitalk.compiler.bktables.IConfig {
    private final FileObject baseFile;
    private final FileSystemManager manager;
    private final String[] schemes;

    public
    CompilerConfig () throws FileSystemException {
        manager = VFS.getManager();
        schemes = manager.getSchemes();
        baseFile = manager.getBaseFile();
    }

    public
    FileObject getBaseFile () {
        return baseFile;
    }

    public
    FileSystemManager getManager () {
        return manager;
    }

    public
    String[] getSchemes () {
        return schemes;
    }

}
