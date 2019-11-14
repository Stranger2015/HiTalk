package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.TermTraverser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

/**
 *
 */
public interface IFunctor extends ITerm, IRangedArity {
    /**
     * @return
     */
    int getName ();

    /**
     * @return
     */
    ITerm[] getArguments ();

    /**
     * @param i
     * @return
     */
    ITerm getArgument ( int i );

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
        return this instanceof ListTerm;
    }

    boolean isBracketed ();

    void setTermTraverser ( TermTraverser traverser );

    boolean isDefined ();

    String toStringArguments ();

    void setArgument ( int i, ITerm term );

    void setArguments ( ITerm[] terms );
}