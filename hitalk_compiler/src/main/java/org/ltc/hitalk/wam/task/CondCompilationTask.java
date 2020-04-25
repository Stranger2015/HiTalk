package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.EnumSet;
import java.util.List;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.*;

/**
 *
 */
public class CondCompilationTask<T extends HtClause> extends PreCompilerTask<T> {

    public EnumSet<DirectiveKind> getDkRDelim() {
        return dkRDelim;
    }

    protected EnumSet<DirectiveKind> dkRDelim;

    /**
     * @param tokenSource
     * @param preCompiler
     * @param kind
     */
    public CondCompilationTask(IPreCompiler<T, PreCompilerTask<T>, ?, ?, ?, ?> preCompiler,
                               PlLexer tokenSource,
                               EnumSet<DirectiveKind> kind) {
        super(preCompiler, tokenSource, kind);

        this.dkRDelim = kind;
    }

    /**
     * @param term
     */
    public boolean ccIf(ITerm term) throws Exception {
        final GoalExpansionTask<T> getask;
        getask = new GoalExpansionTask<>(getPreCompiler(), getTokenSource(), getKind());
        final List<ITerm> l = getask.invoke(term);
        for (ITerm t : l) {
            final ExecutionTask<T> etask;
            try {
                etask = new ExecutionTask<>(getPreCompiler(), getTokenSource(), getKind());
                etask.input = t;
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExecutionError(PERMISSION_ERROR, toString(), e);
            }
            output = invoke(etask.input);

            dkRDelim = of(DK_ELIF, DK_ELSE, DK_ENDIF);
        }

        return true;
    }

    /**
     * @param term
     */
    public boolean ccElIf(ITerm term) throws Exception {
        final boolean b = ccIf(term);
        dkRDelim = of(DK_ELIF, DK_ELSE, DK_ENDIF);
        return b;
    }

    /**
     * @param term
     */
    public boolean ccElse(ITerm term) throws Exception {
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
    protected List<ITerm> invoke0(ITerm term) throws Exception {
        final List<ITerm> l = super.invoke0(term);
        for (final ITerm t : l) {
            for (DirectiveKind directiveKind : dkRDelim) {
                switch (directiveKind) {
                    case DK_IF:
                        ccIf(t);
                        break;
                    case DK_ELSE:
                        ccElse(t);
                        break;
                    case DK_ELIF:
                        ccElIf(t);
                        break;
                    case DK_ENDIF:
                        ccEndIf(t);
                        break;
                }
            }
        }
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

    private boolean checkDirective(HtClause clause) {
        if (clause instanceof Directive) {
            IFunctor body = ((Directive) clause).getDef();
            final DirectiveKind kind = ((Directive) clause).getKind();
        }

        return false;
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