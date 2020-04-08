package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;

import java.util.EnumSet;
import java.util.List;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.*;

/**
 *
 */
public class CondCompilationTask extends RewriteTermTask {

    public EnumSet<DirectiveKind> getDkRDelim() {
        return dkRDelim;
    }

    EnumSet<DirectiveKind> dkRDelim;

    public CondCompilationTask(PlLexer tokenSource,
                               IPreCompiler preCompiler,
                               EnumSet<DirectiveKind> kind) {
        super(tokenSource, preCompiler, kind);
        this.dkRDelim = kind;
    }

    public void ccIf(ITerm term) {
        final GoalExpansionTask getask = new GoalExpansionTask(getPreCompiler(), getTokenSource(), getKind());
        getQueue().add(getask);
        getask.input = term;
        final ExecutionTask etask = new ExecutionTask(getTokenSource(), getPreCompiler(), getKind());
        getQueue().add(etask);
        dkRDelim = of(DK_ELIF, DK_ELSE, DK_ENDIF);

    }

    public void ccElIf(ITerm term) {
        final GoalExpansionTask getask;
        getask = new GoalExpansionTask(getPreCompiler(), getTokenSource(), getKind());
        getQueue().add(getask);
        final ExecutionTask etask = new ExecutionTask(getTokenSource(), getPreCompiler(), getKind());
        getQueue().add(etask);
        dkRDelim = of(DK_ELIF, DK_ELSE, DK_ENDIF);
    }

    public void ccElse(ITerm term) {
        dkRDelim = of(DK_ENDIF);
    }

    public void ccEndIf(ITerm term) {
        dkRDelim = of(DK_IF);
    }

    /**
     * @param term
     * @return
     */
    @Override
    protected List<ITerm> invoke0(ITerm term) {
        super.invoke0(term);
        return output;
    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }

//    protected List<ITerm> condCompilation(ITerm t) {

//        if (!checkDirective(t, kind)) {
//            l.add(t);
//        } else {
//
//        }
//        HiTalkWAMMachine wamMachine = new HtResolutionEngine <HiTalkWAMCompiledQuery,>();
//        HiTalkWAMCompiledQuery query = new HiTalkWAMCompiledQuery();

//    }
//
//    private boolean checkDirective(ITerm t, DirectiveKind kind) {
//        return false;
//    }

//    /**
//     * @param sb
//     */
//    public void toString0(StringBuilder sb) {
//
//    }
}}