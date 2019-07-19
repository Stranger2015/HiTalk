package org.ltc.hitalk.wam.transformers;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Term;

/**
 * @param <T>
 * @param <TC>
 */
public
interface ISpecializer<T extends Clause, TC extends Term> extends ITransformer <T, TC> {

    /**
     * @param clause
     * @return
     */
    T specialize ( T clause );

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is a transformation of the argument.
     */
    @Override
    default
    T transform ( T clause ) {
        return specialize(clause);
    }
}