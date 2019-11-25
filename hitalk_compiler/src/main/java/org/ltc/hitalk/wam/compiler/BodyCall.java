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
     *
     */
    public static class BodyCalls extends BodyCall {

        /**
         * @param sym
         * @param calls
         * @param args
         * @param selectedClauses
         */
        public BodyCalls ( IFunctor sym,
                           ListTerm calls,
                           ListTerm args,//./????????
                           List <HtClause> selectedClauses ) {
            super(sym, calls, args, selectedClauses);
        }

        /**
         * @param sym
         * @param listTerm
         */
        public BodyCalls ( IFunctor sym, ListTerm listTerm ) {
            super(sym, listTerm);
        }

        public BodyCalls ( ITerm msg, List <HtClause> selectedClauses, List <ListTerm> sameSelect ) {
            super(msg, selectedClauses, sameSelect);
        }
    }

    /**
     * @param sym
     * @param calls
     * @param args
     * @param selectedClauses
     */
    public BodyCall ( IFunctor sym,
                      ListTerm calls,
                      ListTerm args,//fixme listterm
                      List <HtClause> selectedClauses ) {
        super(sym, calls);
        this.args = args;
        this.selectedClauses = selectedClauses;
    }

    protected ListTerm args = new ListTerm(0);
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
}
