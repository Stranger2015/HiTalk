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
    Functor getHead();

    /**
     * @return
     */
    Functor[] getBody();

    String toString ( VariableAndFunctorInterner interner, boolean printVarName, boolean printBindings );
}
