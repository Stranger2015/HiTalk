package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;

import java.util.ArrayList;
import java.util.List;

public class BodyCall extends PiCalls {
    List <Term> args = new ArrayList <>();
    List <HtClause> selectedClauses = new ArrayList <>();

    /**
     * @param name
     * @param args
     */
    public BodyCall ( int name, Term[] args ) {
        super(name, args);
    }
}
