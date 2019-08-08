package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;

/**
 *
 */
public
class HtFunctor extends Functor implements IRangedArity {
    /**
     * @param name
     * @param arityMin
     * @param arityMax
     */
    public
    HtFunctor ( int name, int arityMin, int arityMax ) {
        super(name, null);
        setArityRange(arityMin, arityMax);
    }

    /**
     * Reports the number of arguments that this functor takes.
     *
     * @return The number of arguments that this functor takes.
     */
    @Override
    public
    int getArity () {
        return super.getArity();
    }

    /**
     * @return
     */
    @Override
    public
    int getArityInt () {
        return super.getArity();
    }

    /**
     * @return
     */
    @Override
    public
    Term getArityTerm () {
        return null;
    }
}
