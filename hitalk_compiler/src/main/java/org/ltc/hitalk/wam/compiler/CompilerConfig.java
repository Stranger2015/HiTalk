package org.ltc.hitalk.wam.compiler;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.ltc.hitalk.compiler.bktables.IConfig;
import org.ltc.hitalk.compiler.bktables.IProduct;

/**
 *
 */
public
class CompilerConfig implements IConfig {
    private final FileObject baseFile;
    private final FileSystemManager manager;
    private final String[] schemes;

    /**
     * @throws FileSystemException
     */
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

    /**
     * @return
     */
    public
    FileSystemManager getManager () {
        return manager;
    }

    /**
     * @return
     */
    public
    String[] getSchemes () {
        return schemes;
    }

    @Override
    public IProduct getProduct () {
        return null;
    }
}
