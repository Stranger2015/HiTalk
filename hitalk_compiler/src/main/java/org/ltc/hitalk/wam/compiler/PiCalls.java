package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.BodyCall.BodyCalls;

import java.util.List;

/**
 *
 */
public class PiCalls<C extends BodyCalls<C>> extends HtFunctor {
    /**
     *
     */
    protected boolean representable;

    public PiCalls(int name) {
        super(name);
    }

    /**
     * @param name
     * @param args
     */
    public PiCalls(int name, ITerm[] args) {
        super(name, new ListTerm(args));
    }

    /**
     * @param name
     * @param args
     * @param arityDelta
     */
    public PiCalls(int name, int arityDelta, ITerm... args) {
        super(name, arityDelta, new ListTerm(args));
    }

    public <C extends BodyCalls<C>> PiCalls(IFunctor sym, List<BodyCall<C>> calls, ListTerm args, List<HtClause> selectedClauses) {
        super(sym, args, ListTerm.NIL, selectedClauses);


    }

    /**
     * @param <C>
     */
    public static class XPiCalls<C extends BodyCalls<C>> extends PiCalls<C> {

        /**
         * @param sym
         * @param calls
         */
        public XPiCalls(IFunctor sym, List<BodyCalls<C>> calls) throws Exception {
            super(sym.getName(), 0, 0);
        }
    }

    /**
     * @return
     */
    public List<BodyCalls<C>> getCalls() {
        return calls;
    }

    protected List<BodyCalls<C>> calls;

    /**
     * @param name
     * @param arityMin
     * @param arityDelta
     */
    public PiCalls ( int name, int arityMin, int arityDelta ) {
        super(name, arityMin, arityDelta);
    }

    /**
     * @param name
     * @param args
     */
    public PiCalls ( int name, ListTerm args ) {
        this(name, args.getHeads().length, 0);//fixme
    }

    /**
     * @param name
     * @param args
     * @param arityDelta
     */
//    public PiCalls ( int name, ListTerm args, int arityDelta ) {
////        this(name, args, arityDelta);
//    }

    /**
     * @return
     */
    @Override
    public int getArity () {
        return 2;   // piCalls(symbol, Calls)
    }

    public boolean isList () {
        return false;
    }

    /**
     * @return
     */
    @Override //todo
    public boolean isDefined () {
        return false;
    }

    /**
     * @return
     */
    @Override
    public String toStringArguments () {
        return toString();//fixme
    }

    /**
     * @return
     */
    public boolean isRepresentable () {
        return representable;
    }

    public void setMgCalls ( HtVariable var ) {
        this.setArgument(0, var);
    }

    /**
     * @param calls
     */
    public void setCalls(List<BodyCalls<C>> calls) {
        this.calls = calls;
    }
}