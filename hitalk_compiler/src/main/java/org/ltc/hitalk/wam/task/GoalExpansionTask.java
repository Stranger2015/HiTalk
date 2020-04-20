package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.PrologBuiltIns;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
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
public class GoalExpansionTask<T extends HtClause> extends PreCompilerTask<T> {
    private IFunctor f;

    public GoalExpansionTask(IPreCompiler<T> preCompiler,
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
        return output;
    }

    protected List<ListTerm> expandGoal(IFunctor f) {
        final ListTerm lt = new ListTerm(asList(f.getArgument(0), f.getArgument(1)));
        return PrologBuiltIns.EXPAND_GOAL.getBuiltInDef().apply(lt);
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public void run() {

    }
}

