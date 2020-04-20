package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;

import java.util.EnumSet;

/**
 *
 */
public class StandardPreprocessingTask<T extends HtClause> extends PreCompilerTask<T> {

    /**
     * @param preCompiler
     * @param tokenSource
     * @param kind
     */
    public StandardPreprocessingTask(IPreCompiler<T> preCompiler,
                                     PlLexer tokenSource,
                                     EnumSet<Directive.DirectiveKind> kind) {
        super(preCompiler, tokenSource, kind);
    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }

//    /**
//     * @return
//     */
//    public Deque<PreCompilerTask<T>> getTaskQueue() {
//        return null;
//    }
}