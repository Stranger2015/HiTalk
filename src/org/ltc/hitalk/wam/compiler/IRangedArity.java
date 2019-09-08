package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;

public
interface IRangedArity {

    /**
     * @param arityMin
     * @param arityDelta
     * @return
     */
    default
    int setArityRange ( int arityMin, int arityDelta ) {
        return (arityMin & 0xfff0) | ((arityDelta >>> 8) & 0xff);//fixme >>> 8
    }

    /**
     * @return
     */
    default
    int getArityMin () {
        return getArityInt() & 0xffff0000;
    }

    /**
     * @return
     */
    default
    int getArityDelta () {
        return (getArityInt() << 8) & 0xffff;
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
