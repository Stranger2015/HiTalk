package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.TermTraverser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

import java.util.List;

/**
 *
 */
public interface IFunctor extends ITerm, IRangedArity {
    /**
     * @return
     */
    int getHeadsOffset();

    /**
     * @return
     */
    int getName() throws Exception;

    /**
     * @return
     */
    List<ITerm> getArguments();

    /**
     * @param i
     * @return
     */
    ITerm getArgument ( int i );

    /**
     * @return
     */
    default int getArity () {
        return getArguments().size();
    }

    boolean isBracketed ();

    void setTermTraverser ( TermTraverser traverser );

    boolean isDefined ();

    String toStringArguments();

    void setArgument(int i, ITerm term);

    void setArguments(List<ITerm> terms);

    default boolean isListTerm() {
        return false;
    }

    ListTerm getArgs();
}