package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.TermTraverser;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PiCalls implements IFunctor {

    protected final IFunctor sym;
    protected final List <IFunctor> calls = new ArrayList <>();

    public boolean isRepresentable () {
        return representable;
    }

    /**
     *
     */
    protected boolean representable;

    /**
     * @param sym
     * @param calls
     */
    public PiCalls ( IFunctor sym, List <IFunctor> calls ) {
        this.sym = sym;
        this.calls.addAll(calls);
    }

    public int getName () {
        return sym.getName();
    }

    public Term[] getArguments () {
        return new Term[0];
    }

    public Term getArgument ( int i ) {
        return null;
    }

    public boolean isBracketed () {
        return false;
    }

    public void setTermTraverser ( TermTraverser traverser ) {

    }

    public boolean isDefined () {
        return false;
    }

    public int getArityInt () {
        return 0;
    }

    public Term getArityTerm () {
        return null;
    }
}
