package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.builtins.Bypass;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.core.PrologBuiltIns.EXPAND_TERM;

/**
 * This predicate is normally called by the compiler on terms read from the input to perform preprocessing.
 * consists of four steps, where each step processes the output of the previous step.
 * <p>
 * 1) Test conditional compilation directives and translate all input to [] if we are in a ‘false branch' of the
 * conditional compilation. See section 4.3.1.2.
 * <p>
 * 2) Call term_expansion/2.
 * This predicate is first tried in the module that is being compiled and then in modules from which this module
 * inherits according to default_module/2.
 * he output of the expansion in a module is used as input for the next module.
 * Using the default setup and when compiling a normal application module M, this implies expansion is
 * executed in M, user and finally in system. Library modules inherit directly from system and can thus not be
 * re-interpreted by term expansion rules in user.
 * <p>
 * 3) Call DCG expansion (dcg_translate_rule/2).
 * <p>
 * 4) Call expand_goal/2 on each body term that appears in the output of the previous steps.
 */
public class TermExpansionTask<T extends HtClause> extends PreCompilerTask<T> {
    protected final Deque<PreCompilerTask<T>> taskQueue = new ArrayDeque<>();

    /**
     * @param preCompiler
     * @param tokenSource
     * @param kind
     */
    public TermExpansionTask(IPreCompiler<T, PreCompilerTask<T>, ?, ?, ?, ?> preCompiler,
                             PlLexer tokenSource,
                             EnumSet<DirectiveKind> kind) {
        super(preCompiler, tokenSource, kind);

        taskQueue.add(new CondCompilationTask<>(preCompiler, tokenSource, kind));
        taskQueue.add(new DcgExpansionTask<>(preCompiler, tokenSource, kind));
        taskQueue.add(new GoalExpansionTask<>(preCompiler, tokenSource, kind));
    }

    public void toString0(StringBuilder sb) {

    }

    /**
     * Step 2a
     *
     * @param t
     * @return
     */
    protected List<ITerm> defaultExpansion(ITerm t) {
        List<ITerm> result = new ArrayList<>();
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
    protected List<ITerm> userExpansion(ITerm t) {
        final List<ITerm> l = new ArrayList<>();
        HiTalkWAMCompiledQuery query = new HiTalkWAMCompiledQuery();

        return l;
    }

    /**
     * @param t
     * @return
     */
    protected List<ITerm> dcgExpansion(ITerm t) {
        final List<ITerm> l = new ArrayList<>();
        HiTalkWAMCompiledQuery query = new HiTalkWAMCompiledQuery();

        return l;
    }

    /**
     * Step 4
     *
     * @param t
     * @return
     */
    protected List<ITerm> goalExpansion(ITerm t) {
        final List<ITerm> l = new ArrayList<>();
        HiTalkWAMCompiledQuery query = new HiTalkWAMCompiledQuery();

        return l;
    }

    /**
     * @param term
     * @return
     */
    protected List<ITerm> apply(ITerm term) {
        final List<ITerm> l = Collections.singletonList(term);
        List<ITerm> output = new ArrayList<>();
        IntStream.range(0, getTaskQueue().size())
                .mapToObj(i -> getTaskQueue().poll()).filter(Objects
                ::nonNull).forEachOrdered(task -> {
            for (ITerm iTerm : l) {
                List<ITerm> invoke = null;
                try {
                    invoke = task.invoke(iTerm);
                } catch (Exception e) {
                    e.printStackTrace();//fixme
                    throw new ExecutionError(PERMISSION_ERROR, toString(), e);
                }
                output.addAll(invoke);
            }
        });

        return output;
    }

    private Deque<PreCompilerTask<T>> getTaskQueue() {
        return taskQueue;
    }

    /**
     * @param term
     * @return
     */
    @Override
    protected List<ITerm> invoke0(ITerm term) throws Exception {
        List<ITerm> list = new ArrayList<>();
        final List<ITerm> l = super.invoke0(term);
        for (ITerm t : l) {
            list.addAll(expandTerm((IFunctor) t));
        }
        getLogger().info("term expansion: " + list);
        output = list;
        return output;

    }

    protected List<ListTerm> expandTerm(IFunctor f) {
        final ListTerm lt = new ListTerm(asList(f.getArgument(0), f.getArgument(1)));
        return EXPAND_TERM.getBuiltInDef().apply(lt);
    }
}