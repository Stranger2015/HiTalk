package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

import java.util.List;

/**
 *
 */
public class PiCalls extends HtFunctor {
    /**
     *
     */
    protected boolean representable;
    protected List <BodyCall> calls;

    /**
     * @param sym
     * @param calls
     */
    public PiCalls ( IFunctor sym, ListTerm calls ) {
//        super(Kind.LIST, sym.getName(), calls.getHeads());
//        this(sym.getName(), calls);
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
    public PiCalls ( int name, ITerm[] args ) {
        super(name, args);
    }

    /**
     * @param name
     * @param args
     * @param arityDelta
     */
    public PiCalls ( int name, ITerm[] args, int arityDelta ) {
        super(name, args, arityDelta);
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
    public void setCalls ( List <BodyCall> calls ) {
        this.calls = calls;
    }
}