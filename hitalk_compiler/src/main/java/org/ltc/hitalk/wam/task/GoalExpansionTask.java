package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class GoalExpansionTask extends RewriteTermTask {
    public GoalExpansionTask(IPreCompiler preCompiler,
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
        return new ArrayList<>();
    }
}
