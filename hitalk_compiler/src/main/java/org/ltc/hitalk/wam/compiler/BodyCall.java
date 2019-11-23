package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BodyCall extends PiCalls {

    /**
     * @param sym
     * @param calls
     * @param args
     * @param selectedClauses
     */
    public BodyCall ( IFunctor sym, ListTerm calls, List <ITerm> args, List <HtClause> selectedClauses ) {
        super(sym, calls);
        this.args = args;
        this.selectedClauses = selectedClauses;
    }

    protected List <ITerm> args = new ArrayList <>();
    protected List <HtClause> selectedClauses = new ArrayList <>();

    /**
     * @param sym
     * @param listTerm
     */
    public BodyCall ( IFunctor sym, ListTerm listTerm ) {
        super(sym, listTerm);
    }

    @Override
    public String toStringArguments () {
        return super.toStringArguments();//fixme
    }

//    public void forEach ( Consumer <? super ITerm> action ) {
//
//    }
}
