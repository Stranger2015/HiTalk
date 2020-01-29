package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.builtins.Bypass;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;

import java.util.*;
import java.util.stream.IntStream;

/**
 *
 */
public class TermExpansionTask extends PreCompilerTask {

    protected final Deque <PreCompilerTask> tasks = new ArrayDeque <>();

    public TermExpansionTask(IPreCompiler preCompiler,
                             PlLexer tokenSource,
                             EnumSet<DirectiveKind> kind) {
        super(tokenSource, preCompiler, kind);
    }

    /**
     * @return
     */
    public Deque <PreCompilerTask> getQueue () {
        return tasks;
    }

    public void toString0 ( StringBuilder sb ) {
    }

    /**
     *
     */
    private static class CondCompilationTask extends TermExpansionTask {

        protected final PlPrologParser parser;
        protected final IVafInterner interner;

        /**
         * @param tokenSource
         */
        public CondCompilationTask(IPreCompiler preCompiler,
                                   PlLexer tokenSource,
                                   EnumSet<DirectiveKind> kind) {
            super(preCompiler, tokenSource, kind);

            parser = preCompiler.getParser();
            interner = preCompiler.getInterner();
        }

        /**
         * @param task
         */
        public void push ( PreCompilerTask task ) {
            tasks.push(task);
        }
    }
//
//    /**
//     * @param goal
//     * @return
//     */
//    protected boolean checkDirective (IFunctor goal, DirectiveKind kind ) {
//        final FunctorName functorName = interner.getDeinternedFunctorName(goal.getName());
//        return Objects.equals(functorName.getName(), kind.name()) && functorName.getArity() == 1;
//    }

    /**
     * Step 1
     *
     * @param t
     * @return
     */
    protected List <ITerm> condCompilation ( ITerm t ) {
        final List <ITerm> l = new ArrayList <>();
//        if (!checkDirective(t, kind)) {
//            l.add(t);
//        } else {
//
//        }
//        HiTalkWAMMachine wamMachine = new HtResolutionEngine <HiTalkWAMCompiledQuery,>();
//        HiTalkWAMCompiledQuery query = new HiTalkWAMCompiledQuery();

        return l;
    }

    private boolean checkDirective ( ITerm t, DirectiveKind kind ) {
        return false;
    }


    /**
     * Step 2a
     *
     * @param t
     * @return
     */
    protected List <ITerm> defaultExpansion ( ITerm t ) {
        List <ITerm> result = new ArrayList <>();
        if (t instanceof Bypass) {
            result = Collections.singletonList(t);
        }

        return result;
    }

    /**
     * Step 2b
     *
     * @param t
     * @return
     */
    protected List <ITerm> userExpansion ( ITerm t ) {
        final List <ITerm> l = new ArrayList <>();
        HiTalkWAMCompiledQuery query = new HiTalkWAMCompiledQuery();

        return l;
    }

    /**
     * @param t
     * @return
     */
    protected List <ITerm> dcgExpansion ( ITerm t ) {
        final List <ITerm> l = new ArrayList <>();
        HiTalkWAMCompiledQuery query = new HiTalkWAMCompiledQuery();

        return l;
    }

    /**
     * Step 4
     *
     * @param t
     * @return
     */
    protected List <ITerm> goalExpansion ( ITerm t ) {
        final List <ITerm> l = new ArrayList <>();
        HiTalkWAMCompiledQuery query = new HiTalkWAMCompiledQuery();

        return l;
    }

    protected List <ITerm> apply ( ITerm term ) {
        final List <ITerm> l = Collections.singletonList(term);
        List <ITerm> output = new ArrayList <>();
        IntStream.range(0, tasks.size())
                .mapToObj(i -> tasks.poll()).filter(Objects::nonNull).forEachOrdered(task -> l.stream()
                .map(task::invoke).forEachOrdered(output::addAll));

        return output;
    }

//    public Deque <PreCompilerTask> getComponents () {
//        return tasks;
//    }
}
