package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

/**
 *
 */
public class PiCalls extends ListTerm {
    /**
     *
     */
    protected boolean representable;

    /**
     * @param sym
     * @param calls
     */
    public PiCalls ( IFunctor sym, ListTerm calls ) {
        super(Kind.LIST, sym.getName(), calls.getHeads());
        ITerm[] t = getArguments();
        t[0] = sym;
        setCalls(calls);
    }

    /**
     * @return
     */
    @Override
    public int getArity () {
        return 2;   // piCalls(symbol, Calls)
    }

    public boolean isDottedPair () {
        return false;
    }

    /**
     * @return
     */
    @Override //todo
    public boolean isDefined () {
        return false;
    }

    public String toStringArguments () {
        return null;//fixme
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
    public void setCalls ( ListTerm calls ) {
        this.getHeads()[0] = calls;
    }


    /**
     * @return
     */
    public ListTerm getCalls () {
        return (ListTerm) this.getHeads()[0];
    }
}