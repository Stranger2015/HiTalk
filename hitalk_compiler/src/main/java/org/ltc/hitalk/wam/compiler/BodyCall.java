package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class BodyCall<C extends BodyCall.BodyCalls <C>> extends PiCalls <C> {
    public BodyCall () {
        this(-1);
    }

    /**
     * @param name
     * @param arityMin
     * @param arityDelta
     */
    public BodyCall ( int name, int arityMin, int arityDelta ) {
        super(name, arityMin, arityDelta);
    }

    /**
     * @param name
     * @param args
     */
    public BodyCall(int name, ListTerm args) {
        super(args.addHead(name));
    }

    /**
     * @param name
     */
    public BodyCall(int name) {
        super(name);
    }
//    public BodyCall ( ITerm msg, List <HtClause> selectedClauses, List <ListTerm> sameSelect ) {
////        this(msg, selectedClauses, sameSelect);
////        super(msg, selectedClauses, sameSelect);
//    }

    /**
     *
     */
    public static class BodyCalls<C extends BodyCalls<C>> extends BodyCall<C> {

        /**
         * @param sym
         * @param calls
         * @param args
         * @param selectedClauses
         */
        public BodyCalls ( IFunctor sym,
                           List <BodyCall <C>> calls,
                           ListTerm args,//./????????
                           List <HtClause> selectedClauses ) {
            super(sym, calls, args, selectedClauses);
        }

        /**
         * @param sym
         * @param listTerm
         */
        public BodyCalls ( IFunctor sym, List <BodyCall <C>> listTerm ) {
            super(sym, listTerm);
        }

        public BodyCalls ( ITerm msg, List <HtClause> selectedClauses, List <ListTerm> sameSelect ) {
//            super(msg, selectedClauses, sameSelect);
        }
    }

    /**
     * @param sym
     * @param calls
     * @param args
     * @param selectedClauses
     */
    public BodyCall ( IFunctor sym,
                      List <BodyCall <C>> calls,
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
     * @param list
     */
    public BodyCall ( IFunctor sym, List <BodyCall <C>> list ) {
        this(sym, list, new ListTerm(0), Collections.emptyList());
    }

    /**
     * @return
     */
    @Override
    public String toStringArguments () {
        return super.toStringArguments();//fixme
    }
}
