package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 *
 */
public
interface ISubroutine<F extends IFunctor<?>> {
    /**
     * @return
     */
    F getHead ();

    /**
     * @return
     */
    ListTerm<?> getBody ();

    F getGoal ( int i );

    /**
     * @param interner
     * @param printVarName
     * @param printBindings
     * @return
     */
    String toString ( IVafInterner interner, boolean printVarName, boolean printBindings );

    /**
     * @return
     */
    default boolean isQuery () {
        return getHead() == null;
    }
}
