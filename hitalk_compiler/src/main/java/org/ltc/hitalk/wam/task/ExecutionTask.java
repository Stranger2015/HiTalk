package org.ltc.hitalk.wam.task;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.wam.compiler.prolog.IExecutionTask;

import java.util.*;

/**
 *
 */
public class ExecutionTask extends PreCompilerTask implements IExecutionTask {

    private Deque <PreCompilerTask> tasks = new ArrayDeque <>();

    /**
     * @param preCompiler
     */
    public ExecutionTask(PlLexer lexer, IPreCompiler preCompiler, EnumSet<DirectiveKind> kind) {
        super(lexer, preCompiler, kind);
    }

    /**
     * @return
     */
    public Deque <PreCompilerTask> getQueue () {
        return tasks;
    }

    /**
     * @param item
     */
    public void push ( PreCompilerTask item ) {
        getQueue().push(item);
    }

    /**
     *
     */
    public PreCompilerTask poll () {
        return getQueue().poll();
    }

    /**
     * Adds the specified construction to the domain of resolution searched by this resolver.
     *
     * @param term The term to add to the domain.
     * @throws LinkageException If the term to add to the domain, cannot be added to it, because it depends on the
     *                          existance of other clauses which are not in the domain. Implementations may elect to
     *                          raise this as an error at the time the clauses are added to the domain, or during
     *                          resolution, or simply to fail to find a resolution.
     */
    public void addToDomain ( HtPredicate term ) throws LinkageException {

    }

    /**
     * Sets the query to resolve.
     *
     * @param query The query to resolve.
     * @throws LinkageException If the query to add run over the domain, cannot be applied to it, because it depends on
     *                          the existance of clauses which are not in the domain. Implementations may elect to raise
     *                          this as an error at the time the query is created, or during resolution, or simply to
     *                          fail to find a resolution.
     */
    public void setQuery ( HtClause query ) throws LinkageException {

    }

    /**
     * Resolves a query over a logical domain, or knowledge base and a query. The domain and query to resolve over must
     * be established by prior to invoking this method. There may be more than one set of bindings that make the query
     * provable over the domain, in which case subsequent calls to this method will return successive bindings until no
     * more can be found. If no proof can be found, this method will return <tt>null</tt>.
     *
     * @return A list of variable bindings, if the query can be satisfied, or <tt>null</tt> otherwise.
     */
    public Set <HtVariable> resolve () {
        return null;
    }

    /**
     * Resets the resolver. This should clear any start and goal states, and leave the resolver in a state in which it
     * is ready to be run.
     */
    public void reset () {

    }

    /**
     * Provides an iterator that generates all solutions on demand as a sequence of variable bindings.
     *
     * @return An iterator that generates all solutions on demand as a sequence of variable bindings.
     */
    public Iterator <Set <HtVariable>> iterator () {
        return null;
    }

    public void toString0 ( StringBuilder sb ) {

    }
}

