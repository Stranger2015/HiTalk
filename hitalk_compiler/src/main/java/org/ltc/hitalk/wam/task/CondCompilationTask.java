package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.*;

/**
 *
 */
public class CondCompilationTask extends PreCompilerTask {

    public EnumSet<DirectiveKind> getDkRDelim() {
        return dkRDelim;
    }

    protected EnumSet<DirectiveKind> dkRDelim;

    /**
     * @param tokenSource
     * @param preCompiler
     * @param kind
     */
    public CondCompilationTask(IPreCompiler preCompiler,
                               PlLexer tokenSource,
                               EnumSet<DirectiveKind> kind) {
        super(preCompiler, tokenSource, kind);

        this.dkRDelim = kind;
    }

    /**
     * @param term
     */
    public boolean ccIf(ITerm term) throws IOException {
        final GoalExpansionTask getask;
        getask = new GoalExpansionTask(getPreCompiler(), getTokenSource(), getKind());
//        addTask(getask);
//        getask.input = term;
        getask.invoke(term);
        for (ITerm t : getask.output) {
            final ExecutionTask etask;
            try {
                etask = new ExecutionTask(getPreCompiler(), getTokenSource(), getKind());
                etask.input = t;
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExecutionError(PERMISSION_ERROR, toString(), e);
            }
            final List<ITerm> l = invoke(etask.input);

            dkRDelim = of(DK_ELIF, DK_ELSE, DK_ENDIF);
        }

        return true;
    }

    /**
     * @param term
     */
    public boolean ccElIf(ITerm term) throws IOException {
        final boolean b = ccIf(term);
//        addTask(etask);
//        etask.input = term;
        dkRDelim = of(DK_ELIF, DK_ELSE, DK_ENDIF);
        return b;
    }

    /**
     * @param term
     */
    public boolean ccElse(ITerm term) throws IOException {
        final boolean b = !ccIf(term);
        dkRDelim = of(DK_ENDIF);
        return b;
    }

    /**
     * @param term
     */
    public boolean ccEndIf(ITerm term) {
        dkRDelim = of(DK_IF);
        return true;
    }

    /**
     * @param term
     * @return
     */
    @Override
    protected List<ITerm> invoke0(ITerm term) throws IOException {
        final List<ITerm> l = super.invoke0(term);
        for (ITerm t : l) {
            // output.addAll(ccIf(t));
        }
        return l;
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

    private boolean checkDirective(HtClause clause) {
        if (clause instanceof Directive) {
            IFunctor body = ((Directive) clause).getDef();
            final DirectiveKind kind = ((Directive) clause).getKind();
        }

        return false;
    }
}