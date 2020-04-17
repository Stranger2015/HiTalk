package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.PrologBuiltIns;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import static java.util.Arrays.asList;

/**
 *
 */
public class GoalExpansionTask extends PreCompilerTask {
    private IFunctor f;

    public GoalExpansionTask(IPreCompiler preCompiler,
                             PlLexer tokenSource,
                             EnumSet<DirectiveKind> kind) {
        super(preCompiler, tokenSource, kind);
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
    protected List<ITerm> invoke0(ITerm term) throws IOException {
        final List<ITerm> l = super.invoke0(term);
        for (ITerm t : l) {
            output.addAll(expandGoal((IFunctor) t));
        }
        return l;
    }

    protected List<ListTerm> expandGoal(IFunctor f) {
        final ListTerm lt = new ListTerm(asList(f.getArgument(0), f.getArgument(1)));
        return PrologBuiltIns.EXPAND_GOAL.getBuiltInDef().apply(lt);
    }
}

