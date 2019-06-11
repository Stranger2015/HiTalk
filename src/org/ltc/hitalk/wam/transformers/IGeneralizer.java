package org.ltc.hitalk.wam.transformers;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.ClauseTransformer;

/**
 *
 */
public
interface IGeneralizer extends ClauseTransformer {
    /**
     * @param clause
     * @return
     */
    Clause generalize ( Clause clause );

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is a transformation of the argument.
     */
    @Override
    default
    Clause transform ( Clause clause ) {
        return generalize(clause);
    }
}
