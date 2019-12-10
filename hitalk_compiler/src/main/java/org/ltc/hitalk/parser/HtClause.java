package org.ltc.hitalk.parser;

import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.term.HtBaseTerm;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 *
 */
public class HtClause extends HtBaseTerm implements ITerm, ISubroutine {
    protected IFunctor head;
    protected ListTerm body;

    /**
     * Creates a program sentence in L2.
     *
     * @param head The head of the program.
     * @param body The functors that make up the query body of the program, if any. May be <tt>null</tt>
     */
    public HtClause ( IFunctor head, ListTerm body ) {
        this.head = head;
        this.body = body;
    }

    /**
     * @param head
     */
    public HtClause ( IFunctor head ) {
        this(head, null);
    }

    /**
     * Gets the wrapped sentence in the logical language over T.
     *
     * @return The wrapped sentence in the logical language.
     */
//    @Override
    public HtClause getT () {
        return this;
    }

    /**
     * @return
     */
    public boolean isDcgRule () {
        return false;
    }

    @Override
    public IFunctor getHead () {
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
    public IFunctor getGoal ( int i ) {
        return (IFunctor) getBody().get(i);
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

//    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
//        return getClass().getSimpleName()+interner.getDeinternedFunctorName();
//    }

    public ListTerm getBodyAsListTerm () {
        return body;
    }
}