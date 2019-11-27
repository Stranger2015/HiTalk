package org.ltc.hitalk.parser;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.term.HtBaseTerm;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 *
 */
public class HtClause<F extends IFunctor> extends HtBaseTerm implements ITerm, ISubroutine {
    protected F head;
    protected ListTerm body;

    /**
     * Creates a program sentence in L2.
     *
     * @param head The head of the program.
     * @param body The functors that make up the query body of the program, if any. May be <tt>null</tt>
     */
    public HtClause ( F head, ListTerm body ) {
        this.head = head;
        this.body = body;
    }

    /**
     * @param head
     */
    public HtClause ( F head ) {
        this(head, null);
    }

    /**
     * Gets the wrapped sentence in the logical language over T.
     *
     * @return The wrapped sentence in the logical language.
     */
//    @Override
    public HtClause <F> getT () {
        return this;
    }

    /**
     * @return
     */
    public boolean isDcgRule () {
        return false;
    }

    @Override
    public F getHead () {
        return head;
    }

    /**
     * @return
     */
    @Override
    public ListTerm getBody () {
        return body;
    }

    /**
     * @param i
     * @return
     */
    @Override
    public F getGoal ( int i ) {
        return (F) getBody().get(i);
    }

    /**
     * @return
     */
    public int bodyLength () {
        return getBody().size();
    }

    public boolean isHiLog () {
        return false;
    }

    public void free () {

    }

    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
        return toString();
    }

    public ListTerm getBodyAsListTerm () {
        return body;
    }
}