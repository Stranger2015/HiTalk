package org.ltc.hitalk.wam.transformers;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;

/**
 *
 */
public
interface IGeneralizer<T extends HtClause, TC extends Term> extends ITransformer <T,TC> {
    /**
     * @param clause
     * @return
     */
    T generalize ( T clause );

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is a transformation of the argument.
     */
    @Override
    default
    T transform ( T clause ) {
        return generalize(clause);
    }
}
