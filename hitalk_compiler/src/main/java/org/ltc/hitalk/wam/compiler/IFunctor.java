package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;

/**
 *
 */
public interface IFunctor extends IRangedArity {
    /**
     * @return
     */
    int getName ();

    /**
     * @return
     */
    Term[] getArguments ();

    Term getArgument ( int i );

    /**
     * @return
     */
    default int getArity () {

        return getArguments().length;
    }
}