package org.ltc.hitalk.term;

import com.thesett.aima.logic.fol.RecursiveList;
import com.thesett.aima.logic.fol.Term;

import java.util.Iterator;

public
class ListTerm extends RecursiveList {
    private static final int DOT = -1;

//    private static final int DOT = ITermFactory.createAtom("." );

    /**
     * Creates a cons functor. Two arguments must be specified.
     *
     * @param heads The arguments; there must be two.
     */
    public
    ListTerm ( Term[] heads ) {
        super(DOT, heads);//fixme head, tail(s)
    }

    public
    int length () {
        return 0;
    }

    /**
     * Provides a Java iterator over this recursively defined list.
     *
     * @return A Java iterator over this recursively defined list.
     */
    @Override
    public
    Iterator <Term> iterator () {
        return null;
    }

    /**
     * Reports whether this list is the empty list 'nil'.
     *
     * @return <tt>true</tt> if this is the empty list 'nil'.
     */
    @Override
    public
    boolean isNil () {
        return false;
    }

    /**
     * @return list or var
     */
    public
    Term getTail () {
        return null;
    }

    public
    Term getHead () {
        return null;
    }
}
