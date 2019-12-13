package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IComposite;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.builtins.Bypass;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 *
 */
public class TermExpansionTask extends CompilerTask implements IComposite <CompilerTask> {

    protected final Deque <CompilerTask> tasks = new ArrayDeque <>();

    public TermExpansionTask ( IPreCompiler preCompiler,
                               Function <ITerm, List <ITerm>> action,
                               DirectiveKind kind ) {
        super(preCompiler, action, kind);
    }

    /**
     *
     */
    private static class ConditionalCompilation extends TermExpansionTask {

        protected final PlPrologParser parser;
        protected final IVafInterner interner;


        /**
         * @param action
         */
        public ConditionalCompilation ( IPreCompiler preCompiler,
                                        Function <ITerm, List <ITerm>> action,
                                        DirectiveKind kind ) {
            super(preCompiler, action, kind);

            parser = preCompiler.getParser();
            interner = preCompiler.getInterner();
        }

        /**
         * @param task
         */
        public void add ( CompilerTask task ) {

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
        if (!checkDirective(t, kind)) {
            l.add(t);
        } else {

        }
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
//        HiTalkWAMMachine wamMachine = new HtResolutionEngine <HiTalkWAMCompiledQuery,>();
        HiTalkWAMCompiledQuery query = new HiTalkWAMCompiledQuery();
//                query.current solve();

        return l;
    }

    /**
     * @param t
     * @return
     */
    protected List <ITerm> dcgExpansion ( ITerm t ) {
        final List <ITerm> l = new ArrayList <>();
//        HiTalkWAMMachine wamMachine = new HtResolutionEngine <HiTalkWAMCompiledQuery,>();
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
//        HiTalkWAMMachine wamMachine = new HtResolutionEngine <HtClause,HiTalkWAMCompiledPredicate,HiTalkWAMCompiledQuery>();
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

    public Deque <CompilerTask> getComponents () {
        return tasks;
    }
}
