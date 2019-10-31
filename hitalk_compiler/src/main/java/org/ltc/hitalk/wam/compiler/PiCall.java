package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PiCall extends HtFunctor {

    protected final List <HtFunctor> clauses = new ArrayList <>();
    protected boolean representable;

    /**
     * @param name
     * @param args
     */
    public PiCall ( int name, Term[] args ) {
        super(name, args);
    }
}
