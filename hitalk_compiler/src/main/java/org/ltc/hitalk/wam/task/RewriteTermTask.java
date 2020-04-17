package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlLexer;

import java.util.EnumSet;

/**
 *
 */
@Deprecated
public abstract class RewriteTermTask extends PreCompilerTask {

    /**
     * @param tokenSource
     * @param preCompiler
     * @param kind
     */
    public RewriteTermTask(IPreCompiler preCompiler,
                           PlLexer tokenSource,
                           EnumSet<DirectiveKind> kind) {
        super(preCompiler, tokenSource, kind);

    }
}
