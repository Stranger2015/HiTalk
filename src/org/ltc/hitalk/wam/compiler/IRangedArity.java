package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;

public
interface IRangedArity {

    /**
     * @param arityMin
     * @param arityMax
     * @return
     */
    default
    int setArityRange ( int arityMin, int arityMax ) {
        return (arityMin & 0xffff) | ((arityMax >>> 16) & 0xffff);
    }

    /**
     * @return
     */
    default
    public
    int getArityMin () {
        return getArityInt() & 0xffff0000;
    }

    /**
     * @return
     */
    default
    public
    int getArityMax () {
        return (getArityInt() << 16) & 0xffff;
    }

    /**
     * @return
     */
    int getArityInt ();

    /**
     * @return
     */
    Term getArityTerm ();
}
