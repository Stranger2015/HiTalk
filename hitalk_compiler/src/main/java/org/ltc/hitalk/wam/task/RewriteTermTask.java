package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;

import java.util.EnumSet;
import java.util.List;

/**
 *
 */
public abstract class RewriteTermTask extends PreCompilerTask {

    /**
     * @param tokenSource
     * @param preCompiler
     * @param kind
     */
    public RewriteTermTask(PlLexer tokenSource,
                           IPreCompiler preCompiler,
                           EnumSet<DirectiveKind> kind) {
        super(tokenSource, preCompiler, kind);

        getQueue().add(new CondCompilationTask(tokenSource, preCompiler, kind));
        getQueue().add(new TermExpansionTask(preCompiler, tokenSource, kind));
        getQueue().add(new DcgExpansionTask(preCompiler, tokenSource, kind));
        getQueue().add(new GoalExpansionTask(preCompiler, tokenSource, kind));
    }

    /**
     * @param term
     * @return
     */
    @Override
    protected List<ITerm> invoke0(ITerm term) {
        output.add(term);
        return super.invoke0(term);
    }

}
