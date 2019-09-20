package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;

/**
 *
 */
public
interface ISubroutine {
    /**
     * @return
     */
    Functor getHead ();

    /**
     * @return
     */
    Functor[] getBody ();

    /**
     * @param interner
     * @param printVarName
     * @param printBindings
     * @return
     */
    String toString ( VariableAndFunctorInterner interner, boolean printVarName, boolean printBindings );
}
