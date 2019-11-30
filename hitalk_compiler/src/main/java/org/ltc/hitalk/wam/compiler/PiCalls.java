package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

import java.util.List;

/**
 *
 */
public class PiCalls<C extends BodyCall.BodyCalls <C>> extends HtFunctor {
    /**
     *
     */
    protected boolean representable;

    public PiCalls () {
    }

    /**
     * @param <C>
     */
    public static class XPiCalls<C extends BodyCall.BodyCalls <C>> extends PiCalls <C> {

        /**
         * @param sym
         * @param calls
         */
        public XPiCalls ( IFunctor sym, List <BodyCall.BodyCalls <C>> calls ) {
            super(sym, calls);
        }
    }

    /**
     * @return
     */
    public List <BodyCall.BodyCalls <C>> getCalls () {
        return calls;
    }

    protected List <BodyCall.BodyCalls <C>> calls;

    /**
     * @param sym
     * @param calls
     */
    public PiCalls ( IFunctor sym, List <BodyCall.BodyCalls <C>> calls ) {
        ITerm[] t = getArguments();
        t[0] = sym;
        setCalls(calls);
    }

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
        this(name, args, 0);
    }

    /**
     * @param name
     * @param args
     * @param arityDelta
     */
    public PiCalls ( int name, ListTerm args, int arityDelta ) {
//        this(name, args, arityDelta);
    }

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
        this.getHeads()[0] = var;
    }

    /**
     * @param calls
     */
    public void setCalls ( List <BodyCall.BodyCalls <C>> calls ) {
        this.calls = calls;
    }
}