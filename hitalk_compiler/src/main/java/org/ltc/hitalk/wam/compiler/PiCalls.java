package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
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
        name = sym.getName();
        Term[] t = getArguments();
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

    public void setMgCalls ( Variable var ) {
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