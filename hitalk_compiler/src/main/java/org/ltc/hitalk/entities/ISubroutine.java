package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 *
 */
public
interface ISubroutine {
    /**
     * @return
     */
    IFunctor getHead ();

    /**
     * @return
     */
    IFunctor[] getBody ();

    IFunctor getGoal ( int i );

    /**
     * @param interner
     * @param printVarName
     * @param printBindings
     * @return
     */
    String toString ( VariableAndFunctorInterner interner, boolean printVarName, boolean printBindings );

    default boolean isQuery () {
        return getHead() == null;
    }
}
