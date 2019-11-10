package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import org.ltc.hitalk.term.PackedDottedPair;

/**
 *
 */
public class PiCalls extends PackedDottedPair {
    /**
     *
     */
    protected boolean representable;

    /**
     * @param sym
     * @param calls
     */
    public PiCalls ( IFunctor sym, PackedDottedPair calls ) {
        Term[] t = getArguments();
        t[0] = (Term) sym;
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
    public void setCalls ( PackedDottedPair calls ) {
        this.getHeads()[0] = calls;
    }


    /**
     * @return
     */
    public PackedDottedPair getCalls () {
        return (PackedDottedPair) this.getHeads()[0];
    }
}