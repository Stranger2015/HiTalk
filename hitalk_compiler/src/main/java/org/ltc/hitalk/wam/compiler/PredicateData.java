package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

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
    public PredicateData ( int name, ITerm[] args ) {
        super(name, new ListTerm(args));
    }

    /**
     * @param name
     * @param args
     * @param arityDelta
     */
    public PredicateData ( int name, int arityDelta, ITerm... args ) {
        super(name, arityDelta, new ListTerm(args));
    }
}
