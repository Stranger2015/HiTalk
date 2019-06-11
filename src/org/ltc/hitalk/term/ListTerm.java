package org.ltc.hitalk.term;

import com.thesett.aima.logic.fol.Cons;
import com.thesett.aima.logic.fol.Term;

public
class ListTerm extends Cons {

//    private static final int DOT = ITermFactory.createAtom("." );

    /**
     * Creates a cons functor. Two arguments must be specified.
     *
     * @param arguments The arguments; there must be two.
     */
    public
    ListTerm ( Term[] arguments ) {
        super(DOT, arguments);//fixme head, tail(s)
    }
}
