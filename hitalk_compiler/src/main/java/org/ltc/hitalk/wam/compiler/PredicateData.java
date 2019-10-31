package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;

public class PredicateData extends HtFunctor {
    /**
     * @param name
     * @param arityMin
     * @param arityDelta
     */
    public PredicateData ( int name, int arityMin, int arityDelta ) {
        super(name, arityMin, arityDelta);
    }

    /**
     * @param name
     * @param args
     */
    public PredicateData ( int name, Term[] args ) {
        super(name, args);
    }

    /**
     * @param name
     * @param args
     * @param arityDelta
     */
    public PredicateData ( int name, Term[] args, int arityDelta ) {
        super(name, args, arityDelta);
    }
}
