package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.term.ITerm;

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
    int getArityMax () {
        return getArityMin() + getArityDelta();
    }

    /**
     * @return
     */
    default
    int getArityDelta () {
        return (getArityInt() << 8) & 0xfff0;
    }//

    /**
     * @return
     */
    int getArityInt ();

    /**
     * @return
     */
    ITerm getArityTerm ();
}
