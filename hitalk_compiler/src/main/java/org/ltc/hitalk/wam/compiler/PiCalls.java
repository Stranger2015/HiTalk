package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PiCalls extends HtFunctor {

    protected final List <HtFunctor> clauses = new ArrayList <>();
    protected boolean representable;

    /**
     * @param name
     * @param args
     */
    public PiCalls ( int name, Term[] args ) {
        super(name, args);
    }
}
