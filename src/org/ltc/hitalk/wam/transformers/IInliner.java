package org.ltc.hitalk.wam.transformers;

import com.thesett.aima.logic.fol.Clause;

/**
 *
 */
public
interface IInliner<T extends Clause> extends ITransformer <T> {

    /**
     * @param clause
     * @return
     */
    T inline ( T clause );

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is a transformation of the argument.
     */
    @Override
    default
    T transform ( T clause ) {
        return inline(clause);
    }
}
