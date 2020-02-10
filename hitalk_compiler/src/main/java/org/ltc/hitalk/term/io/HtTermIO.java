package org.ltc.hitalk.term.io;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IHitalkObject;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

/**
 *
 */
public abstract class HtTermIO implements IHitalkObject {
    protected Path path;
    protected HiTalkStream stream;
    protected EnumSet<StandardOpenOption> openOption;
    protected IVafInterner interner;

    /**
     * @param path
     * @param stream
     */
    public HtTermIO(Path path, HiTalkStream stream) {
        this.path = path;
        this.stream = stream;
        openOption = stream.options;
    }
}
