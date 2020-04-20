package org.ltc.hitalk.wam.task;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.wam.compiler.WAMCallPoint;
import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.IExecutionTask;

import java.util.*;

/**
 *
 */
public class ExecutionTask<T extends HtClause> extends PreCompilerTask<T> implements IExecutionTask {

    private final Deque<PreCompilerTask<T>> tasks = new ArrayDeque<>();

    private final HtResolutionEngine<T, HtPredicate, HtClause,
            HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> engine;

    /**
     * @param preCompiler
     */
    public ExecutionTask(
            IPreCompiler<T> preCompiler,
            PlLexer lexer,
            EnumSet<DirectiveKind> kind) throws Exception {

        super(preCompiler, lexer, kind);
        engine = new HtResolutionEngine<>();
    }

    /**
     * @return
     */
    public Deque<PreCompilerTask<T>> getQueue() {
        return tasks;
    }

    /**
     * @param item
     */
    public void push(PreCompilerTask<T> item) {
        getQueue().push(item);
    }

    /**
     *
     */
    public PreCompilerTask<T> poll() {
        return getQueue().poll();
    }

    /**
     * Adds the specified construction to the domain of resolution searched by this resolver.
     *
     * @param term The term to add to the domain.
     */
    public void addToDomain(HtPredicate term) throws LinkageException {
        engine.addToDomain((HiTalkWAMCompiledPredicate) term);
    }

    /**
     * Sets the query to resolve.
     *
     * @param query The query to resolve.
     */
    public void setQuery(HtClause query) throws LinkageException {
        engine.setQuery((HiTalkWAMCompiledQuery) query);
    }

    /**
     * Resolves a query over a logical domain, or knowledge base and a query. The domain and query to resolve over must
     * be established by prior to invoking this method. There may be more than one set of bindings that make the query
     * provable over the domain, in which case subsequent calls to this method will return successive bindings until no
     * more can be found. If no proof can be found, this method will return <tt>null</tt>.
     *
     * @return A list of variable bindings, if the query can be satisfied, or <tt>null</tt> otherwise.
     */
    public Set<HtVariable> resolve() {
        return engine.resolve();
    }

    /**
     * Resets the resolver. This should clear any start and goal states, and leave the resolver in a state in which it
     * is ready to be run.
     */
    public void reset() throws Exception {
        engine.reset();
    }

    /**
     * Provides an iterator that generates all solutions on demand as a sequence of variable bindings.
     *
     * @return An iterator that generates all solutions on demand as a sequence of variable bindings.
     */
    public @NotNull Iterator<Set<HtVariable>> iterator() {
        return engine.iterator();
    }


    public void toString0(StringBuilder sb) {

    }


    /**
     * @param goal
     * @return
     */
    @Override
    public boolean call(IFunctor goal) {
        WAMCallPoint callPoint = new WAMCallPoint(hashCode(), hashCode(), hashCode());
        engine.execute(callPoint);
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

