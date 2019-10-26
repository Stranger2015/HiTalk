package org.ltc.hitalk.wam.transformers;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;

/**
 * @param <T>
 * @param <TC>
 */
public
interface ISpecializer<T extends HtClause, TC extends Term> extends ITransformer <T, TC> {

    /**
     * @param clause
     * @return
     */
    T specialize ( T clause );

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is the transformation of the argument.
     */
    @Override
    default
    T transform ( T clause ) {
        return specialize(clause);
    }
}