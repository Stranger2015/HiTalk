package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;

import java.util.EnumSet;
import java.util.List;

public class DcgExpansionTask extends RewriteTermTask {

    /**
     * @param preCompiler
     * @param tokenSource
     * @param kind
     */
    public DcgExpansionTask(
            IPreCompiler preCompiler,
            PlLexer tokenSource,
            EnumSet<DirectiveKind> kind) {
        super(tokenSource, preCompiler, kind);
    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {
    }

    /**
     * @param term
     * @return
     */
    @Override
    protected List<ITerm> invoke0(ITerm term) {

    }
}