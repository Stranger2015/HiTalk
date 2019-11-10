package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.TermTraverser;
import org.ltc.hitalk.term.PackedDottedPair;

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

    /**
     * @param i
     * @return
     */
    Term getArgument ( int i );

    /**
     * @return
     */
    default int getArity () {
        return getArguments().length;
    }

    /**
     * @return
     */
    default boolean isDottedPair () {
        return this instanceof PackedDottedPair;
    }

    boolean isBracketed ();

    void setTermTraverser ( TermTraverser traverser );

    boolean isDefined ();

}