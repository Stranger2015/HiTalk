package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import org.ltc.hitalk.wam.compiler.HtFunctor;

/**
 *
 */
public
interface ISubroutine {
    /**
     * @return
     */
    HtFunctor getHead ();

    /**
     * @return
     */
    HtFunctor[] getBody ();

    HtFunctor getGoal ( int i );

    /**
     * @param interner
     * @param printVarName
     * @param printBindings
     * @return
     */
    String toString ( VariableAndFunctorInterner interner, boolean printVarName, boolean printBindings );
}
